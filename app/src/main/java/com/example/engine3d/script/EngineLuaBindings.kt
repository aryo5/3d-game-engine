package com.example.engine3d.script

import android.util.Log
import android.widget.Toast
import org.luaj.vm2.LuaValue

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

    // Integrasi Sinyal
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
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            Toast.makeText(com.example.engine3d.renderer.EngineSettings.appContext, text, Toast.LENGTH_SHORT).show()
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
    val engineApi = EngineBinding()

    init {
        engine.bind("player", player)
        engine.bind("camera", camera)
        engine.bind("world", world)
        engine.bind("UI", ui)
        engine.bind("Engine", engineApi)
    }
}
