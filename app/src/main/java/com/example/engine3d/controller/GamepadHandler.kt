package com.example.engine3d.controller

import android.view.KeyEvent
import android.view.MotionEvent

class GamepadHandler {
    var stickX: Float = 0f
    var stickY: Float = 0f

    var onJumpPressed: (() -> Unit)? = null
    var onAttackPressed: (() -> Unit)? = null
    var onCrouchToggle: (() -> Unit)? = null
    var onInteractPressed: (() -> Unit)? = null
    var onAction1Pressed: (() -> Unit)? = null
    var onAction2Pressed: (() -> Unit)? = null

    fun handleKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_SPACE -> {
                onJumpPressed?.invoke()
                true
            }
            KeyEvent.KEYCODE_BUTTON_X, KeyEvent.KEYCODE_J -> {
                onAttackPressed?.invoke()
                true
            }
            KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_C -> {
                onCrouchToggle?.invoke()
                true
            }
            KeyEvent.KEYCODE_BUTTON_Y, KeyEvent.KEYCODE_F, KeyEvent.KEYCODE_E -> {
                onInteractPressed?.invoke()
                true
            }
            KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_1 -> {
                onAction1Pressed?.invoke()
                true
            }
            KeyEvent.KEYCODE_BUTTON_R1, KeyEvent.KEYCODE_2 -> {
                onAction2Pressed?.invoke()
                true
            }
            KeyEvent.KEYCODE_W -> {
                stickY = 1.0f
                true
            }
            KeyEvent.KEYCODE_S -> {
                stickY = -1.0f
                true
            }
            KeyEvent.KEYCODE_A -> {
                stickX = -1.0f
                true
            }
            KeyEvent.KEYCODE_D -> {
                stickX = 1.0f
                true
            }
            else -> false
        }
    }

    fun handleKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_S -> {
                stickY = 0f
                true
            }
            KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_D -> {
                stickX = 0f
                true
            }
            else -> false
        }
    }

    fun handleGenericMotion(event: MotionEvent): Boolean {
        if ((event.source and android.view.InputDevice.SOURCE_JOYSTICK) == android.view.InputDevice.SOURCE_JOYSTICK) {
            val axisX = event.getAxisValue(MotionEvent.AXIS_X)
            val axisY = event.getAxisValue(MotionEvent.AXIS_Y)

            // Deadzone
            stickX = if (kotlin.math.abs(axisX) > 0.15f) axisX else 0f
            stickY = if (kotlin.math.abs(axisY) > 0.15f) -axisY else 0f // Invert Y for forward
            return true
        }
        return false
    }
}
