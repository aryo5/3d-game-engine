package com.example.engine3d.animation

import com.example.engine3d.core.GlbAnimationClip
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
    val activeClipName: String = "idle",
    val matchedGlbClip: GlbAnimationClip? = null,
    val limbSwingAngle: Float = 0f
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
        val slotType = when {
            isSlashing -> "slash"
            !isGrounded -> "jump"
            speed > 0.1f -> if (isSprinting || speed > config.walkSpeed * 1.1f) "run" else "walk"
            else -> "idle"
        }

        val targetConfigName = when (slotType) {
            "slash" -> config.animSlashName
            "jump" -> config.animJumpName
            "run" -> config.animRunName
            "walk" -> config.animWalkName
            else -> config.animIdleName
        }

        val matchedClip = findBestMatchingClip(mesh, targetConfigName, slotType, config)
        val activeClipName = matchedClip?.name ?: targetConfigName
        currentClipName = activeClipName

        val timeMultiplier = when (slotType) {
            "slash" -> 2.5f
            "jump" -> 1.0f
            "run" -> 1.4f
            "walk" -> (speed / config.walkSpeed).coerceIn(0.7f, 2.2f)
            else -> 1.0f
        }
        animTimeSec += dt * timeMultiplier

        var baseOffsetY = 0f
        if (matchedClip != null && matchedClip.duration > 0f) {
            val clipTime = animTimeSec % matchedClip.duration
            val phaseNorm = clipTime / matchedClip.duration
            baseOffsetY = sin(phaseNorm * Math.PI * 2.0).toFloat() * 0.05f
        }

        var pose = PoseEvaluation(
            offsetY = baseOffsetY,
            activeClipName = activeClipName,
            matchedGlbClip = matchedClip
        )

        return when (slotType) {
            "slash" -> {
                val slashProgress = (animTimeSec * 3.5f) % 1.0f
                val slashSwing = sin(slashProgress * Math.PI).toFloat()
                pose.copy(
                    rotationYDeg = slashSwing * 35f,
                    pitchXDeg = slashSwing * 12f,
                    scaleMultY = 1.0f + slashSwing * 0.08f,
                    offsetY = pose.offsetY + slashSwing * 0.1f,
                    limbSwingAngle = slashSwing * 65f
                )
            }
            "jump" -> {
                pose.copy(
                    pitchXDeg = -12f,
                    scaleMultY = 1.08f,
                    offsetY = pose.offsetY + 0.15f,
                    limbSwingAngle = -15f
                )
            }
            "run", "walk" -> {
                val isFast = slotType == "run"
                val strideFreq = if (isFast) 12f else 8f
                val bounceAmp = if (isFast) 0.12f else 0.06f
                val tiltAngle = if (isFast) 9f else 4f
                val phase = animTimeSec * strideFreq

                val bounceY = kotlin.math.abs(sin(phase)).toFloat() * bounceAmp
                val swayZ = cos(phase * 0.5f).toFloat() * (if (isFast) 3.5f else 2.0f)
                val pitchX = sin(phase).toFloat() * 3.0f + tiltAngle
                val limbSwing = sin(phase).toFloat() * (if (isFast) 35f else 22f)

                pose.copy(
                    offsetY = pose.offsetY + bounceY,
                    pitchXDeg = pitchX,
                    rollZDeg = swayZ,
                    scaleMultY = 1.0f + sin(phase * 2f).toFloat() * 0.03f,
                    limbSwingAngle = limbSwing
                )
            }
            else -> {
                val breath = sin(animTimeSec * 2.5f).toFloat() * 0.02f
                pose.copy(
                    offsetY = pose.offsetY + breath,
                    scaleMultY = 1.0f + breath * 0.5f,
                    limbSwingAngle = 0f
                )
            }
        }
    }

    private fun findBestMatchingClip(
        mesh: Mesh?,
        configuredName: String,
        slotType: String,
        config: PlayerConfig
    ): GlbAnimationClip? {
        val clips = mesh?.animationClips ?: return null
        if (clips.isEmpty()) return null

        fun clean(str: String): String {
            return str.substringAfterLast("|")
                .substringAfterLast(":")
                .replace("_", "")
                .replace("-", "")
                .trim()
                .lowercase()
        }

        // 1. Direct match with configured name if it's not a dummy placeholder
        if (configuredName.isNotBlank() && !configuredName.startsWith("anim_")) {
            val direct = clips.firstOrNull {
                it.name.equals(configuredName, ignoreCase = true) ||
                clean(it.name) == clean(configuredName) ||
                it.name.contains(configuredName, ignoreCase = true) ||
                configuredName.contains(it.name, ignoreCase = true)
            }
            if (direct != null) return direct
        }

        // 2. Keyword-based matching per slot type
        val keywords = when (slotType) {
            "idle" -> listOf("idle", "stand", "breath", "stay", "wait", "loop", "rest")
            "walk" -> listOf("walk", "move", "jalan", "step", "stride", "forward", "run")
            "run" -> listOf("run", "sprint", "lari", "dash", "jog", "fast", "walk")
            "jump" -> listOf("jump", "leap", "lompat", "air", "fall")
            "slash" -> listOf("slash", "attack", "serang", "hit", "strike", "swing", "punch", "sword")
            else -> listOf()
        }

        for (kw in keywords) {
            val match = clips.firstOrNull { clean(it.name).contains(kw) }
            if (match != null) return match
        }

        // 3. Fallback to secondary bound clips
        val fallbackName = when (slotType) {
            "run" -> config.animWalkName
            "jump" -> config.animIdleName
            "slash" -> config.animIdleName
            else -> ""
        }
        if (fallbackName.isNotBlank() && !fallbackName.startsWith("anim_")) {
            val fallbackMatch = clips.firstOrNull { clean(it.name).contains(clean(fallbackName)) }
            if (fallbackMatch != null) return fallbackMatch
        }

        // 4. If only 1 clip exists in the GLB, always use it
        if (clips.size == 1) {
            return clips[0]
        }

        // 5. If idle or walk, pick first non-attack clip
        if (slotType == "idle" || slotType == "walk") {
            return clips.firstOrNull { c ->
                val cn = clean(c.name)
                !cn.contains("attack") && !cn.contains("death") && !cn.contains("die")
            } ?: clips.firstOrNull()
        }

        return null
    }
}

