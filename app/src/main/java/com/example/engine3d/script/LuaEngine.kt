package com.example.engine3d.script

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.jse.CoerceJavaToLua
import org.luaj.vm2.lib.jse.JsePlatform

class LuaEngine {
    val globals: Globals = JsePlatform.standardGlobals()
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
    -- Putar dan gerakkan petani per frame
    player:translate(0.2 * delta, 0.0, 0.0)
    player:rotateY(20.0 * delta)
end
""".trimIndent()
    )

    private var readyFunc: LuaValue = LuaValue.NIL
    private var processFunc: LuaValue = LuaValue.NIL

    init {
        globals.set("print", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                val msg = arg.tojstring()
                logs = if (logs.isEmpty()) msg else "$logs\n$msg"
                return LuaValue.NIL
            }
        })
    }

    fun bind(name: String, obj: Any) {
        globals.set(name, CoerceJavaToLua.coerce(obj))
    }

    fun clearLogs() {
        logs = ""
    }

    fun runScript(code: String) {
        currentScript = code
        try {
            val chunk = globals.load(code)
            chunk.call()
            readyFunc = globals.get("_ready")
            processFunc = globals.get("_process")
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
                // error suppressed
            }
        }
    }
}
