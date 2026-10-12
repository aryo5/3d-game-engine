package com.example.engine3d.script

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.engine3d.project.ProjectManager
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.jse.CoerceJavaToLua
import org.luaj.vm2.lib.jse.JsePlatform

class LuaEngine {
    var globals: Globals? = null
    var logs by mutableStateOf("")
        private set

    var currentScript by mutableStateOf(ProjectManager.readActiveScript())

    private var readyFunc: LuaValue = LuaValue.NIL
    private var processFunc: LuaValue = LuaValue.NIL

    init {
        initRuntime()

        // Listener saat proyek aktif berganti di Project Manager
        ProjectManager.onProjectActivatedListener = { proj ->
            reloadFromActiveProject()
        }
    }

    private fun initRuntime() {
        try {
            globals = JsePlatform.standardGlobals().apply {
                set("print", object : OneArgFunction() {
                    override fun call(arg: LuaValue): LuaValue {
                        val msg = arg.tojstring()
                        logs = if (logs.isEmpty()) msg else "$logs\n$msg"
                        return LuaValue.NIL
                    }
                })
            }
        } catch (e: Throwable) {
            Log.e("Apex3D-Lua", "Init error: ${e.message}", e)
            logs = "[Error Init] ${e.message}"
        }
    }

    fun bind(name: String, obj: Any) {
        try {
            globals?.set(name, CoerceJavaToLua.coerce(obj))
        } catch (e: Throwable) {
            Log.e("Apex3D-Lua", "Error bind $name: ${e.message}", e)
        }
    }

    fun clearLogs() {
        logs = ""
    }

    // Memuat ulang script saat berganti proyek aktif
    fun reloadFromActiveProject() {
        val code = ProjectManager.readActiveScript()
        currentScript = code
        runScript(code)
        logs = "$logs\n[System] Beralih ke proyek: ${ProjectManager.activeProject?.name ?: "Default"}"
    }

    // Eksekusi sekaligus menyimpan permanen ke file di Documents
    fun runScript(code: String) {
        currentScript = code
        ProjectManager.saveActiveScript(code) // Simpan permanen ke file HP

        val g = globals
        if (g == null) {
            logs = "$logs\n[System] Lua globals belum siap."
            return
        }
        try {
            val chunk = g.load(code)
            chunk.call()
            readyFunc = g.get("_ready")
            processFunc = g.get("_process")
            if (!readyFunc.isnil()) {
                readyFunc.call()
            }
            logs = "$logs\n[System] Script sukses di-reload & disimpan ke Documents!"
        } catch (e: Throwable) {
            logs = "$logs\n[Lua Error] ${e.message}"
        }
    }

    fun update(delta: Double) {
        if (!processFunc.isnil()) {
            try {
                processFunc.call(LuaValue.valueOf(delta))
            } catch (_: Throwable) {}
        }
    }
}
