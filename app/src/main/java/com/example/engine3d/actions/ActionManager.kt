package com.example.engine3d.actions

import com.example.engine3d.math.Vec3
import com.example.engine3d.physics.PhysicsEngine

class ActionManager(
    private val physicsEngine: PhysicsEngine
) {
    val registeredActions = mutableListOf<CharacterAction>()

    var activeAction: CharacterAction? = null
    var actionTimer: Float = 0f
    var actionProgress: Float = 0f

    // Action slots mapped to HUD buttons
    var primaryAction: CharacterAction = CharacterAction.DEFAULT_SLASH
    var secondaryAction: CharacterAction = CharacterAction.DEFAULT_SLAM
    var utilityAction: CharacterAction = CharacterAction.DEFAULT_DASH

    // VFX / animation output
    var swingAngle: Float = 0f
    var slashTrailAlpha: Float = 0f
    var lastVfxColor: String = "#00E5FF"
    var isActionInProgress: Boolean = false

    var onActionTriggered: ((CharacterAction) -> Unit)? = null

    init {
        registeredActions.add(CharacterAction.DEFAULT_SLASH)
        registeredActions.add(CharacterAction.DEFAULT_SLAM)
        registeredActions.add(CharacterAction.DEFAULT_DASH)
    }

    fun addCustomAction(action: CharacterAction) {
        registeredActions.removeAll { it.id == action.id }
        registeredActions.add(action)
        // Set as secondary or utility if added
        secondaryAction = action
    }

    fun triggerAction(action: CharacterAction): Boolean {
        if (isActionInProgress) return false

        activeAction = action
        actionTimer = action.durationSec
        actionProgress = 0f
        isActionInProgress = true
        lastVfxColor = action.vfxColorHex

        // Apply initial physics impulse if any
        if (action.jumpImpulse > 0f) {
            physicsEngine.applyImpulse(Vec3(0f, action.jumpImpulse, 0f))
        }

        // Apply dash forward impulse if DASH
        if (action.animationType == "DASH") {
            val yawRad = Math.toRadians(physicsEngine.characterYawDeg.toDouble())
            val fwdX = kotlin.math.sin(yawRad).toFloat()
            val fwdZ = -kotlin.math.cos(yawRad).toFloat()
            physicsEngine.applyImpulse(Vec3(fwdX * 12f, 1f, fwdZ * 12f))
        }

        onActionTriggered?.invoke(action)

        return true
    }

    fun update(dt: Float) {
        if (!isActionInProgress || activeAction == null) {
            slashTrailAlpha = (slashTrailAlpha - dt * 4f).coerceAtLeast(0f)
            swingAngle = 0f
            return
        }

        val action = activeAction!!
        actionTimer -= dt
        val elapsed = action.durationSec - actionTimer
        actionProgress = (elapsed / action.durationSec).coerceIn(0f, 1f)

        when (action.animationType) {
            "SLASH" -> {
                // Swing sword horizontally from -70 to +70 deg
                swingAngle = -70f + 140f * actionProgress
                slashTrailAlpha = kotlin.math.sin(actionProgress * Math.PI.toFloat())
            }
            "SLAM" -> {
                // Raise and slam down
                swingAngle = if (actionProgress < 0.5f) {
                    actionProgress * 2f * 90f
                } else {
                    90f - (actionProgress - 0.5f) * 2f * 120f
                }
                slashTrailAlpha = if (actionProgress > 0.4f) 1f - (actionProgress - 0.4f) * 1.6f else 0.3f
            }
            "DASH" -> {
                swingAngle = 45f
                slashTrailAlpha = 0.8f
            }
            else -> {
                swingAngle = actionProgress * 360f
                slashTrailAlpha = 0.5f
            }
        }

        if (actionTimer <= 0f) {
            isActionInProgress = false
            activeAction = null
        }
    }
}
