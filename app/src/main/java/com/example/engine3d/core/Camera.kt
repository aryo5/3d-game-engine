package com.example.engine3d.core

import com.example.engine3d.math.Mat4
import com.example.engine3d.math.Vec3
import com.example.engine3d.terrain.TerrainHeightQuery
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

enum class CameraPresetMode(val displayName: String) {
    DYNAMIC_EXPLORATION("Eksplorasi Bayangan (Default)"),
    STEALTH_CROUCH("Mengendap / Stealth Rendah"),
    COMBAT_FOCUS("Fokus Tempur Jarak Dekat"),
    EAGLE_PANORAMA("Sudut Tinggi / Elang Panorama"),
    FIRST_PERSON("Sudut Pandang Pertama (FPP)")
}

/**
 * Sistem Kamera Sinematik Open-World dinamis ala Assassin's Creed Shadows:
 * 1. Shoulder Offset (Kamera sedikit condong di atas bahu kanan karakter, khas AC Shadows)
 * 2. Dynamic Speed Framing (Mundur dan melebarkan FOV saat sprint/lari kencang, mendekat saat jalan/diam)
 * 3. Stealth/Crouch Stance (Menurunkan ketinggian dan mendekat ke punggung saat merayap/jongkok)
 * 4. Terrain & Wall Collision Avoidance (Mencegah kamera tembus lereng bukit/objek GLB dengan auto-pull)
 * 5. Smooth Spring Damping & Lag (Pergerakan kamera halus mengikuti inersia rotasi dan posisi karakter)
 * 6. Action Impact Shake (Goyangan kamera halus saat jurus tebasan/slam menghantam)
 */
class Camera {
    var mode: CameraPresetMode = CameraPresetMode.DYNAMIC_EXPLORATION
    var isFirstPerson: Boolean
        get() = mode == CameraPresetMode.FIRST_PERSON
        set(value) {
            mode = if (value) CameraPresetMode.FIRST_PERSON else CameraPresetMode.DYNAMIC_EXPLORATION
        }

    // Target positions
    var target = Vec3(0f, 1.5f, 0f)
    var smoothedTarget = Vec3(0f, 1.5f, 0f)
    var position = Vec3(0f, 3f, 6f)
    var smoothedPosition = Vec3(0f, 3f, 6f)

    // Angles
    var yawDeg: Float = 0f
    var pitchDeg: Float = 16f
    var targetYawDeg: Float = 0f
    var targetPitchDeg: Float = 16f

    // Distance and offsets
    var baseDistance: Float = 6.0f
    var currentDistance: Float = 6.0f
    var targetDistance: Float = 6.0f
    var baseHeightOffset: Float = 1.55f
    var shoulderOffset: Float = 0.55f // Geser ke kanan karakter ala AC Shadows

    // Camera Matrices
    val viewMatrix = Mat4()
    val projMatrix = Mat4()
    val viewProjMatrix = Mat4()

    // FOV & Projection
    var baseFovDeg: Float = 62f
    var currentFovDeg: Float = 62f
    var targetFovDeg: Float = 62f
    var fovDeg: Float
        get() = currentFovDeg
        set(value) {
            baseFovDeg = value
            currentFovDeg = value
            targetFovDeg = value
        }
    var distance: Float
        get() = currentDistance
        set(value) {
            baseDistance = value
            currentDistance = value
            targetDistance = value
        }
    var nearPlane: Float = 0.1f
    var farPlane: Float = 500f
    var aspectRatio: Float = 1.777f

    // Camera Shake
    private var shakeIntensity: Float = 0f
    private var shakeDecay: Float = 5f
    private var shakeTimer: Float = 0f

    // Reference to terrain for collision raycast
    var terrainQuery: TerrainHeightQuery? = null

    fun triggerShake(intensity: Float = 0.25f, durationSec: Float = 0.2f) {
        shakeIntensity = intensity.coerceIn(0.05f, 1.0f)
        shakeTimer = durationSec
    }

    fun cycleCameraMode(): CameraPresetMode {
        val allModes = CameraPresetMode.values()
        val nextIdx = (mode.ordinal + 1) % allModes.size
        mode = allModes[nextIdx]
        applyModeParameters()
        return mode
    }

    fun setCameraMode(newMode: CameraPresetMode) {
        mode = newMode
        applyModeParameters()
    }

    private fun applyModeParameters() {
        when (mode) {
            CameraPresetMode.DYNAMIC_EXPLORATION -> {
                baseDistance = 6.0f
                baseHeightOffset = 1.55f
                shoulderOffset = 0.55f
                baseFovDeg = 65f
            }
            CameraPresetMode.STEALTH_CROUCH -> {
                baseDistance = 4.2f
                baseHeightOffset = 1.10f
                shoulderOffset = 0.40f
                baseFovDeg = 60f
                targetPitchDeg = 10f
            }
            CameraPresetMode.COMBAT_FOCUS -> {
                baseDistance = 5.2f
                baseHeightOffset = 1.45f
                shoulderOffset = 0.70f
                baseFovDeg = 68f
            }
            CameraPresetMode.EAGLE_PANORAMA -> {
                baseDistance = 9.5f
                baseHeightOffset = 3.2f
                shoulderOffset = 0.1f
                baseFovDeg = 75f
                targetPitchDeg = 32f
            }
            CameraPresetMode.FIRST_PERSON -> {
                baseDistance = 0f
                baseHeightOffset = 1.65f
                shoulderOffset = 0f
                baseFovDeg = 75f
            }
        }
    }

    fun updateCinematic(
        targetPos: Vec3,
        characterYawDeg: Float,
        isSprinting: Boolean,
        isCrouched: Boolean,
        movementSpeed: Float,
        dt: Float
    ) {
        val clampedDt = dt.coerceIn(0.001f, 0.05f)

        // 1. Stance & Speed Framing (Dynamic zoom & FOV)
        if (mode != CameraPresetMode.FIRST_PERSON && mode != CameraPresetMode.EAGLE_PANORAMA) {
            if (isCrouched) {
                targetDistance = 3.4f
                targetFovDeg = 56f
                baseHeightOffset = 0.95f
                shoulderOffset = 0.45f
            } else if (isSprinting && movementSpeed > 4.5f) {
                // AC Shadows sprint pull-back effect
                targetDistance = baseDistance + 1.2f
                targetFovDeg = baseFovDeg + 10f
                baseHeightOffset = 1.55f
                shoulderOffset = 0.65f
            } else {
                targetDistance = baseDistance
                targetFovDeg = baseFovDeg
                baseHeightOffset = 1.45f
                shoulderOffset = 0.55f
            }
        } else if (mode == CameraPresetMode.EAGLE_PANORAMA) {
            targetDistance = baseDistance
            targetFovDeg = baseFovDeg
        }

        // Smooth spring transition for distance and FOV
        currentDistance += (targetDistance - currentDistance) * (6f * clampedDt)
        currentFovDeg += (targetFovDeg - currentFovDeg) * (5f * clampedDt)

        // Smooth target position tracking (prevents jerky camera on uneven ground)
        val desiredTargetY = targetPos.y + baseHeightOffset
        smoothedTarget.x += (targetPos.x - smoothedTarget.x) * (14f * clampedDt)
        smoothedTarget.y += (desiredTargetY - smoothedTarget.y) * (12f * clampedDt)
        smoothedTarget.z += (targetPos.z - smoothedTarget.z) * (14f * clampedDt)
        target.set(smoothedTarget)

        // Clamp rotation angles
        pitchDeg = pitchDeg.coerceIn(-75f, 75f)

        // Camera Shake calculation
        var shakeOffsetX = 0f
        var shakeOffsetY = 0f
        if (shakeTimer > 0f) {
            shakeTimer -= clampedDt
            val rand1 = ((Math.random() - 0.5) * 2.0).toFloat()
            val rand2 = ((Math.random() - 0.5) * 2.0).toFloat()
            shakeOffsetX = rand1 * shakeIntensity
            shakeOffsetY = rand2 * shakeIntensity
            shakeIntensity = max(0f, shakeIntensity - clampedDt * shakeDecay)
        }

        if (mode == CameraPresetMode.FIRST_PERSON) {
            // First Person: Camera placed at character eye level
            val yawRad = Math.toRadians(yawDeg.toDouble())
            val pitchRad = Math.toRadians(pitchDeg.toDouble())

            val dirX = (sin(yawRad) * cos(pitchRad)).toFloat()
            val dirY = sin(pitchRad).toFloat()
            val dirZ = (-cos(yawRad) * cos(pitchRad)).toFloat()

            position.set(target.x, target.y + 0.15f + shakeOffsetY, target.z)
            val lookTarget = position.add(dirX + shakeOffsetX, dirY, dirZ)
            viewMatrix.lookAt(position, lookTarget, Vec3.UP)
        } else {
            // Third Person Over-The-Shoulder (AC Shadows camera)
            val yawRad = Math.toRadians(yawDeg.toDouble())
            val pitchRad = Math.toRadians(pitchDeg.toDouble())

            // Vector pointing backwards from target
            val backX = (sin(yawRad) * cos(pitchRad)).toFloat()
            val backY = sin(pitchRad).toFloat()
            val backZ = (cos(yawRad) * cos(pitchRad)).toFloat()

            // Vector pointing to the right of camera (Shoulder offset)
            val rightX = cos(yawRad).toFloat()
            val rightZ = -sin(yawRad).toFloat()

            var effectiveDist = currentDistance

            // 2. Terrain & Wall Collision Avoidance (Raycast to prevent clipping through GLB terrain)
            val query = terrainQuery
            if (query != null && effectiveDist > 2.5f) {
                // Sample points along the camera arm from target back to position
                val steps = 6
                val charY = targetPos.y
                for (i in 1..steps) {
                    val frac = i.toFloat() / steps
                    val testDist = effectiveDist * frac
                    val testX = target.x + backX * testDist + rightX * shoulderOffset
                    val testZ = target.z + backZ * testDist + rightZ * shoulderOffset
                    val testY = target.y + backY * testDist

                    val terrainHeight = query.sampleSurface(testX, testZ).height
                    // Only pull camera in if test position is beneath a high terrain slope/wall
                    if (terrainHeight > charY + 0.35f && testY < terrainHeight + 0.25f) {
                        effectiveDist = max(2.5f, testDist - 0.20f)
                        break
                    }
                }
            }

            // Desired camera position with shoulder offset and shake
            val desiredX = target.x + backX * effectiveDist + rightX * shoulderOffset + shakeOffsetX
            val desiredY = target.y + backY * effectiveDist + shakeOffsetY
            val desiredZ = target.z + backZ * effectiveDist + rightZ * shoulderOffset

            // Smooth position lag
            smoothedPosition.x += (desiredX - smoothedPosition.x) * (16f * clampedDt)
            smoothedPosition.y += (desiredY - smoothedPosition.y) * (16f * clampedDt)
            smoothedPosition.z += (desiredZ - smoothedPosition.z) * (16f * clampedDt)
            position.set(smoothedPosition)

            // Look slightly ahead of character for cinematic leading
            val lookAtPoint = Vec3(
                target.x + rightX * (shoulderOffset * 0.35f),
                target.y,
                target.z + rightZ * (shoulderOffset * 0.35f)
            )

            viewMatrix.lookAt(position, lookAtPoint, Vec3.UP)
        }

        projMatrix.perspective(currentFovDeg, aspectRatio, nearPlane, farPlane)
        viewProjMatrix.set(projMatrix).multiply(viewMatrix)
    }

    // Keep compatibility with original update call
    fun update(targetPos: Vec3) {
        updateCinematic(
            targetPos = targetPos,
            characterYawDeg = yawDeg,
            isSprinting = false,
            isCrouched = false,
            movementSpeed = 0f,
            dt = 0.016f
        )
    }

    fun setAspectRatio(width: Int, height: Int) {
        if (height > 0) {
            aspectRatio = width.toFloat() / height.toFloat()
        }
    }

    fun rotate(deltaYaw: Float, deltaPitch: Float) {
        yawDeg = (yawDeg + deltaYaw) % 360f
        if (yawDeg < 0f) yawDeg += 360f
        pitchDeg = (pitchDeg + deltaPitch).coerceIn(-75f, 75f)
    }

    var onZoomChanged: ((Float) -> Unit)? = null

    fun zoom(deltaDistance: Float) {
        baseDistance = (baseDistance + deltaDistance).coerceIn(1.5f, 20f)
        targetDistance = baseDistance
        onZoomChanged?.invoke(baseDistance)
    }

    fun setDirectDistance(newDist: Float) {
        baseDistance = newDist.coerceIn(1.5f, 20f)
        targetDistance = baseDistance
        currentDistance = baseDistance
        onZoomChanged?.invoke(baseDistance)
    }
}
