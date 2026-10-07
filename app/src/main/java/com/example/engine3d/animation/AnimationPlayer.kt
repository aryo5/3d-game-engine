package com.example.engine3d.animation

import com.example.engine3d.core.Mesh
import com.example.engine3d.importer.PlayerConfig
import kotlin.math.cos
import kotlin.math.sin

data class PoseEvaluation(
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val offsetZ: Float = 0f,
    val rotationYDeg: Float = 0f,
    val pitchXDeg: Float = 0f,
    val rollZDeg: Float = 0f,
    val scaleMultX: Float = 1f,
    val scaleMultY: Float = 1f,
    val scaleMultZ: Float = 1f,
    val activeClipName: String = "idle"
)

class AnimationPlayer {
    var animTimeSec: Float = 0f
    var currentClipName: String = "idle"

    fun evaluatePose(
        mesh: Mesh?,
        config: PlayerConfig,
        speed: Float,
        isGrounded: Boolean,
        isSprinting: Boolean,
        isSlashing: Boolean,
        dt: Float
    ): PoseEvaluation {
        val targetClipName = when {
            isSlashing -> config.animSlashName
            !isGrounded -> config.animJumpName
            speed > 0.1f -> if (isSprinting || speed > config.walkSpeed * 1.1f) config.animRunName else config.animWalkName
            else -> config.animIdleName
        }

        currentClipName = targetClipName

        val timeMultiplier = when {
            isSlashing -> 2.5f
            !isGrounded -> 1.0f
            speed > 0.1f -> (speed / config.walkSpeed).coerceIn(0.8f, 2.2f)
            else -> 1.0f
        }
        animTimeSec += dt * timeMultiplier

        val keyframeClip = mesh?.animationClips?.firstOrNull { clip ->
            clip.name.contains(targetClipName, ignoreCase = true) ||
            targetClipName.contains(clip.name, ignoreCase = true) ||
            (targetClipName == "walk" && clip.name.contains("move", ignoreCase = true)) ||
            (targetClipName == "run" && clip.name.contains("sprint", ignoreCase = true))
        }

        var baseOffsetY = 0f
        if (keyframeClip != null && keyframeClip.duration > 0f) {
            val clipTime = animTimeSec % keyframeClip.duration
            val phaseNorm = clipTime / keyframeClip.duration
            baseOffsetY = sin(phaseNorm * Math.PI * 2.0).toFloat() * 0.05f
        }

        var pose = PoseEvaluation(offsetY = baseOffsetY, activeClipName = targetClipName)

        return when {
            isSlashing -> {
                val slashProgress = (animTimeSec * 3f) % 1.0f
                val slashSwing = sin(slashProgress * Math.PI).toFloat()
                pose.copy(
                    rotationYDeg = slashSwing * 35f,
                    pitchXDeg = slashSwing * 12f,
                    scaleMultY = 1.0f + slashSwing * 0.08f,
                    offsetY = pose.offsetY + slashSwing * 0.1f
                )
            }
            !isGrounded -> {
                pose.copy(
                    pitchXDeg = -12f,
                    scaleMultY = 1.08f,
                    offsetY = pose.offsetY + 0.15f
                )
            }
            speed > 0.1f -> {
                val strideFreq = if (isSprinting) 12f else 8f
                val bounceAmp = if (isSprinting) 0.12f else 0.06f
                val tiltAngle = if (isSprinting) 9f else 4f
                val phase = animTimeSec * strideFreq

                val bounceY = kotlin.math.abs(sin(phase)).toFloat() * bounceAmp
                val swayZ = cos(phase * 0.5f).toFloat() * (if (isSprinting) 3.5f else 2.0f)
                val pitchX = sin(phase).toFloat() * 3.0f + tiltAngle

                pose.copy(
                    offsetY = pose.offsetY + bounceY,
                    pitchXDeg = pitchX,
                    rollZDeg = swayZ,
                    scaleMultY = 1.0f + sin(phase * 2f).toFloat() * 0.03f
                )
            }
            else -> {
                val breath = sin(animTimeSec * 2.5f).toFloat() * 0.02f
                pose.copy(
                    offsetY = pose.offsetY + breath,
                    scaleMultY = 1.0f + breath * 0.5f
                )
            }
        }
    }
}
