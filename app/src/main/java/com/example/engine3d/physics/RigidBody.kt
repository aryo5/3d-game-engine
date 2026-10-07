package com.example.engine3d.physics

import com.example.engine3d.math.Vec3
import com.example.engine3d.terrain.TerrainHeightQuery

enum class ShapeType { BOX, SPHERE }

class RigidBody(
    var id: String,
    var shape: ShapeType = ShapeType.BOX,
    var position: Vec3 = Vec3(0f, 5f, 0f),
    var size: Vec3 = Vec3(1f, 1f, 1f),
    var mass: Float = 10f,
    var restitution: Float = 0.5f, // Bounciness
    var friction: Float = 0.85f,
    var color: FloatArray = floatArrayOf(0.9f, 0.6f, 0.2f, 1f)
) {
    val velocity = Vec3(0f, 0f, 0f)
    var isSleeping = false

    fun update(dt: Float, gravity: Float, terrainQuery: TerrainHeightQuery) {
        if (isSleeping) return

        // Apply gravity
        velocity.y += gravity * dt

        // Integrate
        position.x += velocity.x * dt
        position.y += velocity.y * dt
        position.z += velocity.z * dt

        // Terrain collision
        val radius = size.y * 0.5f
        val surface = terrainQuery.sampleSurface(position.x, position.z)
        val groundY = surface.height + radius

        if (position.y <= groundY) {
            position.y = groundY
            if (velocity.y < 0) {
                // Bounce along surface normal
                val normal = surface.normal
                val dot = velocity.dot(normal)
                if (dot < 0) {
                    velocity.subSelf(normal.scale(dot * (1f + restitution)))
                }

                // Apply surface friction
                velocity.x *= friction
                velocity.z *= friction

                // Add slope slide if steep
                if (surface.slopeAngleDeg > 25f) {
                    val slideFactor = (surface.slopeAngleDeg - 25f) / 45f * 8f
                    velocity.x += normal.x * slideFactor * dt
                    velocity.z += normal.z * slideFactor * dt
                }
            }

            if (velocity.lengthSquared() < 0.05f) {
                velocity.set(0f, 0f, 0f)
            }
        }
    }
}
