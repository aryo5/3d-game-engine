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

        val baseOffsetY = 0f

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
                    scaleMultY = 1.0f,
                    offsetY = 0f,
                    limbSwingAngle = slashSwing * 65f
                )
            }
            "jump" -> {
                pose.copy(
                    pitchXDeg = -12f,
                    scaleMultY = 1.0f,
                    offsetY = 0.15f,
                    limbSwingAngle = -15f
                )
            }
            "run", "walk" -> {
                val isFast = slotType == "run"
                val strideFreq = if (isFast) 10f else 7f
                val tiltAngle = if (isFast) 4f else 2f
                val phase = animTimeSec * strideFreq

                // If GLB has its own animation clip, DO NOT add artificial vertical bouncing or squashing
                val hasClip = matchedClip != null
                val pitchX = if (hasClip) 0f else tiltAngle
                val limbSwing = if (hasClip) 0f else sin(phase).toFloat() * (if (isFast) 30f else 20f)

                pose.copy(
                    offsetY = 0f,
                    pitchXDeg = pitchX,
                    rollZDeg = 0f,
                    scaleMultY = 1.0f,
                    limbSwingAngle = limbSwing
                )
            }
            else -> {
                pose.copy(
                    offsetY = 0f,
                    scaleMultY = 1.0f,
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

        // 4. If only 1 clip exists in the GLB, check if it's a locomotion clip during idle
        if (clips.size == 1) {
            val cn = clean(clips[0].name)
            if (slotType == "idle" && (cn.contains("walk") || cn.contains("run") || cn.contains("move") || cn.contains("step") || cn.contains("sprint"))) {
                return null
            }
            return clips[0]
        }

        // 5. If idle or walk, pick first non-attack clip
        if (slotType == "idle" || slotType == "walk") {
            return clips.firstOrNull { c ->
                val cn = clean(c.name)
                if (slotType == "idle" && (cn.contains("walk") || cn.contains("run") || cn.contains("sprint"))) false
                else !cn.contains("attack") && !cn.contains("death") && !cn.contains("die")
            } ?: if (slotType == "walk") clips.firstOrNull() else null
        }

        return null
    }
}

