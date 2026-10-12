package com.example.engine3d.script

import androidx.compose.runtime.mutableStateListOf
import org.luaj.vm2.LuaValue

data class LuaCustomButton(val label: String, val callback: LuaValue)
data class LuaCustomSlider(val label: String, val min: Float, val max: Float, var value: Float, val callback: LuaValue)

object Apex3DRealHook {
    // Jantung Frame Loop 60 FPS
    var onFrameUpdate: ((Float) -> Unit)? = null

    // Aksi Fisik Karakter
    var onRealJump: (() -> Unit)? = null
    var onRealReset: (() -> Unit)? = null
    var onRealSlash: (() -> Unit)? = null
    var onRealSlam: (() -> Unit)? = null
    var onRealDash: (() -> Unit)? = null
    var onRealAction: ((String) -> Unit)? = null
    var onRealSpeedChange: ((Float) -> Unit)? = null
    var onRealTimeScale: ((Float) -> Unit)? = null

    val customButtons = mutableStateListOf<LuaCustomButton>()
    val customSliders = mutableStateListOf<LuaCustomSlider>()

    fun triggerJump() = onRealJump?.invoke()
    fun triggerReset() = onRealReset?.invoke()
    fun triggerSlash() = onRealSlash?.invoke()
    fun triggerSlam() = onRealSlam?.invoke()
    fun triggerDash() = onRealDash?.invoke()
    fun setSpeed(speed: Float) = onRealSpeedChange?.invoke(speed)
    fun setTimeScale(scale: Float) = onRealTimeScale?.invoke(scale)

    fun triggerAction(action: String) {
        when (action.uppercase()) {
            "SLASH" -> triggerSlash()
            "SLAM" -> triggerSlam()
            "DASH" -> triggerDash()
            "JUMP" -> triggerJump()
            else -> onRealAction?.invoke(action)
        }
    }

    fun addCustomButton(label: String, callback: LuaValue) {
        customButtons.removeAll { it.label == label }
        customButtons.add(LuaCustomButton(label, callback))
    }

    fun addCustomSlider(label: String, min: Float, max: Float, initial: Float, callback: LuaValue) {
        customSliders.removeAll { it.label == label }
        customSliders.add(LuaCustomSlider(label, min, max, initial, callback))
    }

    fun clearCustomUI() {
        customButtons.clear()
        customSliders.clear()
    }
}
