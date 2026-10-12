package com.example.engine3d.script

import android.util.Log
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.jse.CoerceJavaToLua
import com.example.engine3d.scene.Node
import java.io.File

class PlayerBinding {
    var posX = 0.0
    var posY = 0.0
    var posZ = 0.0
    var rotY = 0.0
    private var _scale = 1.0
    val scale: Double get() = _scale
    private var _speed = 1.0
    val speed: Double get() = _speed
    var currentAnim = "Idle"

    fun jump() {
        Apex3DRealHook.triggerJump()
        SceneTreeManager.playerNode.emit_signal("on_jump")
    }

    fun slash() {
        Apex3DRealHook.triggerSlash()
        SceneTreeManager.playerNode.emit_signal("on_action", "SLASH")
    }

    fun slam() {
        Apex3DRealHook.triggerSlam()
        SceneTreeManager.playerNode.emit_signal("on_action", "SLAM")
    }

    fun dash() {
        Apex3DRealHook.triggerDash()
        SceneTreeManager.playerNode.emit_signal("on_action", "DASH")
    }

    fun reset() = Apex3DRealHook.triggerReset()

    fun set_position(x: Double, y: Double, z: Double) { posX = x; posY = y; posZ = z }
    fun setPosition(x: Double, y: Double, z: Double) = set_position(x, y, z)

    fun translate(dx: Double, dy: Double, dz: Double) { posX += dx; posY += dy; posZ += dz }

    fun rotate_y(deg: Double) { rotY = (rotY + deg) % 360.0 }
    fun rotateY(deg: Double) = rotate_y(deg)

    fun set_scale(s: Double) { _scale = s }
    fun setScale(s: Double) = set_scale(s)

    fun set_speed(s: Double) {
        _speed = s
        Apex3DRealHook.setSpeed(s.toFloat())
    }
    fun setSpeed(s: Double) = set_speed(s)

    fun connect(signal: String, callback: LuaValue) = SceneTreeManager.playerNode.connect(signal, callback)
    fun emit_signal(signal: String, vararg args: Any?) = SceneTreeManager.playerNode.emit_signal(signal, *args)
    fun set_process(enable: Boolean) = SceneTreeManager.playerNode.set_process(enable)
}

class CameraBinding {
    var posX = 0.0
    var posY = 3.0
    var posZ = -6.0
    private var _fov = 60.0
    val fov: Double get() = _fov

    fun set_position(x: Double, y: Double, z: Double) { posX = x; posY = y; posZ = z }
    fun setPosition(x: Double, y: Double, z: Double) = set_position(x, y, z)

    fun set_fov(newFov: Double) { _fov = newFov }
    fun setFov(newFov: Double) = set_fov(newFov)

    fun look_at(x: Double, y: Double, z: Double) {}
    fun lookAt(x: Double, y: Double, z: Double) = look_at(x, y, z)
}

class WorldBinding {
    var timeOfDay = 12.0
    private var _sunIntensity = 1.0
    val sunIntensity: Double get() = _sunIntensity
    private var _gravity = -9.8
    val gravity: Double get() = _gravity

    fun set_time(hour: Double) { timeOfDay = hour % 24.0 }
    fun setTime(hour: Double) = set_time(hour)

    fun set_sun_intensity(intensity: Double) { _sunIntensity = intensity }
    fun setSunIntensity(intensity: Double) = set_sun_intensity(intensity)

    fun set_gravity(g: Double) { _gravity = g }
    fun setGravity(g: Double) = set_gravity(g)

    fun get_time(): Double = timeOfDay
    fun getTime(): Double = timeOfDay
}

class UIBinding {
    fun add_button(label: String, callback: LuaValue) = Apex3DRealHook.addCustomButton(label, callback)
    fun addButton(label: String, callback: LuaValue) = add_button(label, callback)

    fun add_slider(label: String, min: Double, max: Double, initial: Double, callback: LuaValue) {
        Apex3DRealHook.addCustomSlider(label, min.toFloat(), max.toFloat(), initial.toFloat(), callback)
    }
    fun addSlider(label: String, min: Double, max: Double, initial: Double, callback: LuaValue) = add_slider(label, min, max, initial, callback)

    fun clear() = Apex3DRealHook.clearCustomUI()
    fun clearButtons() = clear()

    fun show_message(text: String) {
        Log.d("Apex3D-Lua", "UI Message: $text")
        Apex3DRealHook.onShowMessage?.invoke(text)
    }
}

// -------------------------------------------------------------
// SISTEM AKSES FILE PROYEK PENUH (FS)
// -------------------------------------------------------------
class FilesystemBinding {
    private fun getBaseDir(): File {
        val fallback = File("/sdcard/Download/Apex3D_Projects")
        return fallback
    }

    fun get_project_dir(): String = getBaseDir().absolutePath

    fun list(subDir: String): Array<String> {
        val target = if (subDir.isEmpty() || subDir == ".") getBaseDir() else File(getBaseDir(), subDir)
        if (!target.exists() || !target.isDirectory) return emptyArray()
        return target.list() ?: emptyArray()
    }

    fun read(filePath: String): String {
        val file = if (filePath.startsWith("/")) File(filePath) else File(getBaseDir(), filePath)
        if (!file.exists() || !file.isFile) return ""
        return try { file.readText() } catch (e: Exception) { "" }
    }

    fun write(filePath: String, content: String): Boolean {
        val file = if (filePath.startsWith("/")) File(filePath) else File(getBaseDir(), filePath)
        return try {
            file.parentFile?.mkdirs()
            file.writeText(content)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun exists(filePath: String): Boolean {
        val file = if (filePath.startsWith("/")) File(filePath) else File(getBaseDir(), filePath)
        return file.exists()
    }
}

// -------------------------------------------------------------
// SISTEM SCENE TREE & NODE MANAGEMENT
// -------------------------------------------------------------
class SceneBinding {
    fun create_node(name: String): Node {
        val node = Node(name)
        SceneTreeManager.playerNode.add_child(node)
        return node
    }

    fun spawn_glb(modelFile: String, x: Double, y: Double, z: Double): Node {
        val node = Node(File(modelFile).nameWithoutExtension)
        SceneTreeManager.playerNode.add_child(node)
        Log.d("Apex3D-Lua", "Spawned Node: $modelFile")
        return node
    }

    fun get_player(): Node = SceneTreeManager.playerNode
}

// -------------------------------------------------------------
// SISTEM PLUGIN LOADER
// -------------------------------------------------------------
class PluginBinding {
    fun load(scriptFileName: String): Boolean {
        val code = LuaScriptManager.fs.read(scriptFileName)
        if (code.isEmpty()) return false
        return try {
            LuaScriptManager.engine.execute(code)
            true
        } catch (e: Throwable) {
            Log.e("Apex3D-Plugin", "Plugin error: ${e.message}")
            false
        }
    }
}

class EngineBinding {
    fun set_time_scale(scale: Double) = Apex3DRealHook.setTimeScale(scale.toFloat())
    fun setTimeScale(scale: Double) = set_time_scale(scale)
    fun get_fps(): Int = 60
    fun getFps(): Int = get_fps()
    fun get_active_nodes(): Int = SceneTreeManager.tree.getActiveCount()
    fun get_total_nodes(): Int = SceneTreeManager.tree.getTotalCount()
}

object LuaScriptManager {
    val engine = LuaEngine()
    val player = PlayerBinding()
    val camera = CameraBinding()
    val world = WorldBinding()
    val ui = UIBinding()
    val fs = FilesystemBinding()
    val scene = SceneBinding()
    val plugin = PluginBinding()
    val engineApi = EngineBinding()

    init {
        engine.bind("player", player)
        engine.bind("camera", camera)
        engine.bind("world", world)
        engine.bind("UI", ui)
        engine.bind("FS", fs)
        engine.bind("Scene", scene)
        engine.bind("Plugin", plugin)
        engine.bind("Engine", engineApi)
    }
}
