package com.example.engine3d.script

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.jse.CoerceJavaToLua
import org.luaj.vm2.lib.jse.JsePlatform

class LuaEngine {
    var globals: Globals? = null
    var logs by mutableStateOf("")
        private set

    var currentScript by mutableStateOf(
        """-- Petani Harvest Moon Controller
function _ready()
    print("=== ENGINE READY: PETANI TERPASANG ===")
    player:setPosition(0.0, 0.0, 0.0)
    player:playAnimation("Idle")
end

function _process(delta)
    player:translate(0.2 * delta, 0.0, 0.0)
    player:rotateY(20.0 * delta)
end
""".trimIndent()
    )

    private var readyFunc: LuaValue = LuaValue.NIL
    private var processFunc: LuaValue = LuaValue.NIL

    init {
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
            Log.e("Apex3D-Lua", "Gagal inisialisasi LuaJ: ${e.message}", e)
            logs = "[Error Init LuaJ] ${e.message}"
        }
    }

    fun bind(name: String, obj: Any) {
        try {
            globals?.set(name, CoerceJavaToLua.coerce(obj))
        } catch (e: Throwable) {
            Log.e("Apex3D-Lua", "Error binding $name: ${e.message}", e)
        }
    }

    fun clearLogs() {
        logs = ""
    }

    fun runScript(code: String) {
        currentScript = code
        val g = globals
        if (g == null) {
            logs = "$logs\n[System] Lua runtime belum siap."
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
            logs = "$logs\n[System] Script sukses di-reload!"
        } catch (e: Throwable) {
            logs = "$logs\n[Lua Error] ${e.message}"
        }
    }

    fun update(delta: Double) {
        if (!processFunc.isnil()) {
            try {
                processFunc.call(LuaValue.valueOf(delta))
            } catch (e: Throwable) {
                // cegah spam error di log loop
            }
        }
    }
}
