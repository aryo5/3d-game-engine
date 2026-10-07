package com.example.engine3d.physics

import com.example.engine3d.math.Vec3
import com.example.engine3d.terrain.TerrainHeightQuery
import kotlin.math.cos
import kotlin.math.sin

class PhysicsEngine(
    val terrainQuery: TerrainHeightQuery,
    val barrierManager: BarrierManager = BarrierManager()
) {
    // Character Physics State
    val characterPos = Vec3(0f, 0f, 0f)
    val characterVel = Vec3(0f, 0f, 0f)
    var characterYawDeg: Float = 0f

    var isGrounded: Boolean = true
    var isCrouched: Boolean = false
    var isSprinting: Boolean = false
    var isSliding: Boolean = false
    var currentSlopeAngle: Float = 0f
    var surfaceNormal: Vec3 = Vec3.UP

    // Constants
    var gravity: Float = -19.6f
    var walkSpeed: Float = 5.2f
    var sprintSpeed: Float = 8.8f
    var crouchSpeed: Float = 2.4f
    var jumpImpulse: Float = 7.8f
    var maxClimbableSlope: Float = 46f
    var stepHeight: Float = 0.5f

    // Dynamic scene bodies
    val rigidBodies = mutableListOf<RigidBody>()

    init {
        // Spawn sample dynamic crates and orbs for physics demonstration
        rigidBodies.add(RigidBody("Crate1", ShapeType.BOX, Vec3(4f, 6f, 3f), Vec3(1.2f, 1.2f, 1.2f), 15f, 0.4f, 0.82f, floatArrayOf(0.9f, 0.5f, 0.2f, 1f)))
        rigidBodies.add(RigidBody("Crate2", ShapeType.BOX, Vec3(-5f, 7f, -4f), Vec3(1.0f, 1.0f, 1.0f), 10f, 0.5f, 0.85f, floatArrayOf(0.2f, 0.7f, 0.9f, 1f)))
        rigidBodies.add(RigidBody("Orb1", ShapeType.SPHERE, Vec3(2f, 8f, -6f), Vec3(0.8f, 0.8f, 0.8f), 8f, 0.75f, 0.92f, floatArrayOf(0.3f, 0.95f, 0.4f, 1f)))
        rigidBodies.add(RigidBody("Orb2", ShapeType.SPHERE, Vec3(-3f, 5f, 6f), Vec3(1.1f, 1.1f, 1.1f), 12f, 0.7f, 0.90f, floatArrayOf(0.95f, 0.25f, 0.8f, 1f)))

        // Snap character to surface
        val initSurface = terrainQuery.sampleSurface(characterPos.x, characterPos.z)
        characterPos.y = initSurface.height
    }

    fun jump() {
        if (isGrounded && !isSliding) {
            characterVel.y = jumpImpulse
            isGrounded = false
        }
    }

    fun applyImpulse(impulse: Vec3) {
        characterVel.addSelf(impulse)
        if (impulse.y > 0) {
            isGrounded = false
        }
    }

    fun resetCharacterPosition() {
        characterPos.set(0f, 0f, 0f)
        characterVel.set(0f, 0f, 0f)
        val s = terrainQuery.sampleSurface(0f, 0f)
        characterPos.y = s.height
        isGrounded = true
    }

    fun spawnPhysicsBox(nearPos: Vec3) {
        val newBox = RigidBody(
            id = "Box_${System.currentTimeMillis() % 1000}",
            shape = ShapeType.BOX,
            position = Vec3(nearPos.x + (Math.random().toFloat() - 0.5f) * 4f, nearPos.y + 5f, nearPos.z + (Math.random().toFloat() - 0.5f) * 4f),
            size = Vec3(1f, 1f, 1f),
            mass = 12f,
            restitution = 0.4f,
            friction = 0.85f,
            color = floatArrayOf(0.9f, 0.8f, 0.1f, 1f)
        )
        rigidBodies.add(newBox)
        if (rigidBodies.size > 20) {
            rigidBodies.removeAt(0)
        }
    }

    fun update(
        dt: Float,
        moveInputX: Float, // -1 to 1 (strafe)
        moveInputY: Float, // -1 to 1 (forward/backward)
        cameraYawDeg: Float
    ) {
        val clampedDt = dt.coerceIn(0.001f, 0.05f)

        // Determine target speed
        val baseSpeed = when {
            isCrouched -> crouchSpeed
            isSprinting -> sprintSpeed
            else -> walkSpeed
        }

        // Apply speed multiplier from road speed zones
        val finalSpeed = baseSpeed * barrierManager.activeSpeedMultiplier

        // Calculate world move direction relative to camera yaw
        var targetVelX = 0f
        var targetVelZ = 0f

        val hasInput = (moveInputX != 0f || moveInputY != 0f)
        if (hasInput) {
            val yawRad = Math.toRadians(cameraYawDeg.toDouble())
            val camForwardX = -sin(yawRad).toFloat()
            val camForwardZ = -cos(yawRad).toFloat()
            val camRightX = cos(yawRad).toFloat()
            val camRightZ = -sin(yawRad).toFloat()

            // True camera-relative movement:
            // moveInputY > 0 -> Move in camera forward direction (-sin, -cos)
            // moveInputX > 0 -> Move in camera right direction (cos, -sin)
            val dirX = camForwardX * moveInputY + camRightX * moveInputX
            val dirZ = camForwardZ * moveInputY + camRightZ * moveInputX

            val len = kotlin.math.sqrt(dirX * dirX + dirZ * dirZ)
            if (len > 0.0001f) {
                targetVelX = (dirX / len) * finalSpeed
                targetVelZ = (dirZ / len) * finalSpeed

                // Rotate character to smoothly face movement direction
                val targetYaw = Math.toDegrees(kotlin.math.atan2(dirX.toDouble(), -dirZ.toDouble())).toFloat()
                characterYawDeg = targetYaw
            }
        }

        // Sample surface terrain contour at current position
        val surface = terrainQuery.sampleSurface(characterPos.x, characterPos.z)
        val groundY = surface.height
        currentSlopeAngle = surface.slopeAngleDeg
        surfaceNormal = surface.normal

        // Slope check: sliding downhill if slope is too steep!
        if (currentSlopeAngle > maxClimbableSlope && isGrounded) {
            isSliding = true
            // Slide along surface normal projected horizontally
            val slidePower = (currentSlopeAngle - maxClimbableSlope) * 0.45f
            targetVelX += surface.normal.x * slidePower
            targetVelZ += surface.normal.z * slidePower
        } else {
            isSliding = false
        }

        // Smooth horizontal acceleration/deceleration
        val accel = if (isGrounded) 14f else 4f
        characterVel.x += (targetVelX - characterVel.x) * (accel * clampedDt).coerceIn(0f, 1f)
        characterVel.z += (targetVelZ - characterVel.z) * (accel * clampedDt).coerceIn(0f, 1f)

        // Apply gravity if not grounded or jumping
        if (!isGrounded || characterVel.y > 0) {
            characterVel.y += gravity * clampedDt
        }

        // Predict next position
        val nextX = characterPos.x + characterVel.x * clampedDt
        val nextZ = characterPos.z + characterVel.z * clampedDt
        var nextY = characterPos.y + characterVel.y * clampedDt

        // Sample target terrain contour to prevent climbing impossible vertical cliffs
        val targetSurface = terrainQuery.sampleSurface(nextX, nextZ)
        val heightDiff = targetSurface.height - characterPos.y

        if (heightDiff > stepHeight && targetSurface.slopeAngleDeg > maxClimbableSlope && isGrounded) {
            // Cannot walk up wall: stop forward momentum
            characterVel.x = 0f
            characterVel.z = 0f
        } else {
            characterPos.x = nextX
            characterPos.z = nextZ
        }

        // Ground snapping and contour following
        val currentGround = terrainQuery.sampleSurface(characterPos.x, characterPos.z).height
        if (nextY <= currentGround) {
            characterPos.y = currentGround
            characterVel.y = 0f
            isGrounded = true
        } else {
            characterPos.y = nextY
            // Check if within snap-to-ground threshold (walking down gentle slopes)
            if (characterPos.y - currentGround < 0.25f && characterVel.y <= 0f) {
                characterPos.y = currentGround
                characterVel.y = 0f
                isGrounded = true
            } else {
                isGrounded = false
            }
        }

        // Resolve barriers, invisible walls, road blockers, speed pads, launch pads
        barrierManager.resolveCharacterCollision(characterPos, characterVel, 0.45f, clampedDt)

        // Update dynamic rigid bodies and player-box interaction
        for (body in rigidBodies) {
            body.update(clampedDt, gravity, terrainQuery)

            // Player pushes rigid body
            val dx = body.position.x - characterPos.x
            val dz = body.position.z - characterPos.z
            val dist = kotlin.math.sqrt(dx * dx + dz * dz)
            val pushRadius = 1.0f + body.size.x * 0.5f

            if (dist < pushRadius && dist > 0.001f) {
                val overlap = pushRadius - dist
                val pushX = (dx / dist) * overlap * 8f
                val pushZ = (dz / dist) * overlap * 8f
                body.velocity.x += pushX
                body.velocity.z += pushZ
            }
        }
    }
}
