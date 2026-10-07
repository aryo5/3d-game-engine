package com.example.engine3d.physics

import com.example.engine3d.math.Vec3
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs

class BarrierManager {
    val barriers = mutableListOf<WorldBarrier>()
    var activeTriggerMessage: String? = null
    var triggerMessageTimer: Float = 0f
    var activeSpeedMultiplier: Float = 1.0f

    init {
        setupDefaultBarriers()
    }

    fun setupDefaultBarriers() {
        barriers.clear()
        // 1. Barikade Blokir Jalan Jembatan / Gap
        barriers.add(
            WorldBarrier(
                id = "road_block_bridge",
                name = "Barikade Blokir Akses Jembatan",
                type = BarrierType.WALL_BARRIER,
                position = Vec3(14f, 1.5f, 0f),
                size = Vec3(1.2f, 3.5f, 8.0f),
                isPassable = false,
                warningMessage = "🚫 Jalan ditutup! Wilayah terlarang."
            )
        )

        // 2. Batas Luar Peta (Tembok batas dunia tak kasat mata di 4 sisi peta 260x260m)
        barriers.add(
            WorldBarrier(
                id = "world_limit_north",
                name = "Batas Ujung Utara",
                type = BarrierType.WALL_BARRIER,
                position = Vec3(0f, 5f, 115f),
                size = Vec3(240f, 16f, 3f),
                isPassable = false,
                warningMessage = "⚠️ Telah mencapai batas terluar wilayah Utara!"
            )
        )
        barriers.add(
            WorldBarrier(
                id = "world_limit_south",
                name = "Batas Ujung Selatan",
                type = BarrierType.WALL_BARRIER,
                position = Vec3(0f, 5f, -115f),
                size = Vec3(240f, 16f, 3f),
                isPassable = false,
                warningMessage = "⚠️ Telah mencapai batas terluar wilayah Selatan!"
            )
        )
        barriers.add(
            WorldBarrier(
                id = "world_limit_east",
                name = "Batas Ujung Timur",
                type = BarrierType.WALL_BARRIER,
                position = Vec3(115f, 5f, 0f),
                size = Vec3(3f, 16f, 240f),
                isPassable = false,
                warningMessage = "⚠️ Telah mencapai batas terluar wilayah Timur!"
            )
        )
        barriers.add(
            WorldBarrier(
                id = "world_limit_west",
                name = "Batas Ujung Barat",
                type = BarrierType.WALL_BARRIER,
                position = Vec3(-115f, 5f, 0f),
                size = Vec3(3f, 16f, 240f),
                isPassable = false,
                warningMessage = "⚠️ Telah mencapai batas terluar wilayah Barat!"
            )
        )

        // 3. Speed Boost Pad (Jalur Turbo)
        barriers.add(
            WorldBarrier(
                id = "speed_pad_1",
                name = "Jalur Akselerasi Turbo (Speed Pad)",
                type = BarrierType.SPEED_BOOST_PAD,
                position = Vec3(0f, 0.1f, -12f),
                size = Vec3(4f, 0.4f, 16f),
                isPassable = true,
                speedMultiplier = 2.4f,
                warningMessage = "⚡ Turbo Booster Aktif: Kecepatan x2.4!"
            )
        )

        // 4. Bounce Jump Pad (Pelontar Lompatan Tinggi)
        barriers.add(
            WorldBarrier(
                id = "jump_pad_1",
                name = "Launch Pad Pelontar Vertikal",
                type = BarrierType.BOUNCE_PAD,
                position = Vec3(-10f, 0.2f, -5f),
                size = Vec3(3f, 0.4f, 3f),
                isPassable = true,
                bounceImpulse = 15.0f,
                warningMessage = "🚀 Meluncur ke Udara!"
            )
        )
    }

    fun resolveCharacterCollision(
        charPos: Vec3,
        charVel: Vec3,
        charRadius: Float = 0.5f,
        dt: Float
    ) {
        if (triggerMessageTimer > 0f) {
            triggerMessageTimer -= dt
            if (triggerMessageTimer <= 0f) {
                activeTriggerMessage = null
            }
        }

        var speedMult = 1.0f

        for (b in barriers) {
            val halfX = b.size.x * 0.5f
            val halfY = b.size.y * 0.5f
            val halfZ = b.size.z * 0.5f

            // Check AABB overlap with capsule radius
            val minX = b.position.x - halfX - charRadius
            val maxX = b.position.x + halfX + charRadius
            val minY = b.position.y - halfY - 0.2f
            val maxY = b.position.y + halfY + 1.8f
            val minZ = b.position.z - halfZ - charRadius
            val maxZ = b.position.z + halfZ + charRadius

            if (charPos.x in minX..maxX && charPos.y in minY..maxY && charPos.z in minZ..maxZ) {
                if (!b.isPassable) {
                    // Solid Blocking Barrier: Push character back along shortest penetration axis
                    val overlapLeft = (charPos.x - (b.position.x - halfX))
                    val overlapRight = ((b.position.x + halfX) - charPos.x)
                    val overlapBack = (charPos.z - (b.position.z - halfZ))
                    val overlapFront = ((b.position.z + halfZ) - charPos.z)

                    val minOverlapX = minOf(abs(overlapLeft), abs(overlapRight))
                    val minOverlapZ = minOf(abs(overlapBack), abs(overlapFront))

                    if (minOverlapX < minOverlapZ) {
                        if (charPos.x < b.position.x) {
                            charPos.x = b.position.x - halfX - charRadius
                            if (charVel.x > 0) charVel.x = 0f
                        } else {
                            charPos.x = b.position.x + halfX + charRadius
                            if (charVel.x < 0) charVel.x = 0f
                        }
                    } else {
                        if (charPos.z < b.position.z) {
                            charPos.z = b.position.z - halfZ - charRadius
                            if (charVel.z > 0) charVel.z = 0f
                        } else {
                            charPos.z = b.position.z + halfZ + charRadius
                            if (charVel.z < 0) charVel.z = 0f
                        }
                    }

                    activeTriggerMessage = b.warningMessage
                    triggerMessageTimer = 2.0f
                } else {
                    // Trigger pad behaviors
                    when (b.type) {
                        BarrierType.SPEED_BOOST_PAD -> {
                            speedMult = maxOf(speedMult, b.speedMultiplier)
                            activeTriggerMessage = b.warningMessage
                            triggerMessageTimer = 1.0f
                        }
                        BarrierType.BOUNCE_PAD -> {
                            if (charVel.y <= 1f) {
                                charVel.y = b.bounceImpulse
                                activeTriggerMessage = b.warningMessage
                                triggerMessageTimer = 1.2f
                            }
                        }
                        BarrierType.KILL_ZONE -> {
                            charPos.set(0f, 1f, 0f)
                            charVel.set(0f, 0f, 0f)
                            activeTriggerMessage = b.warningMessage
                            triggerMessageTimer = 2.5f
                        }
                        else -> {}
                    }
                }
            }
        }

        activeSpeedMultiplier = speedMult
    }

    fun addBarrier(barrier: WorldBarrier) {
        barriers.removeAll { it.id == barrier.id }
        barriers.add(barrier)
    }

    fun serializeBarriers(): String {
        val array = JSONArray()
        for (b in barriers) {
            array.put(b.toJson())
        }
        return array.toString(2)
    }

    fun toJsonString(): String = serializeBarriers()

    fun loadBarriersFromJson(jsonStr: String): Boolean {
        return try {
            val array = JSONArray(jsonStr)
            barriers.clear()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                barriers.add(WorldBarrier.fromJson(obj))
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun loadFromJson(jsonStr: String): Boolean = loadBarriersFromJson(jsonStr)
}
