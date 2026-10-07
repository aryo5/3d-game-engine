package com.example.engine3d.physics

import com.example.engine3d.math.AABB
import com.example.engine3d.math.Vec3
import org.json.JSONArray
import org.json.JSONObject

enum class BarrierType {
    WALL_BARRIER,       // Tembok penghalang / blokir jalan tak kasat mata
    ROAD_ZONE,          // Jalur jalan khusus
    SPEED_BOOST_PAD,    // Pelat penambah kecepatan
    BOUNCE_PAD,         // Pelat pelontar tinggi
    KILL_ZONE,          // Zona jurang / reset posisi
    CHECKPOINT          // Titik simpan lokasi
}

data class WorldBarrier(
    val id: String,
    var name: String,
    var type: BarrierType = BarrierType.WALL_BARRIER,
    val position: Vec3 = Vec3(0f, 0f, 0f),
    val size: Vec3 = Vec3(2f, 3f, 2f),
    var isPassable: Boolean = false,
    var speedMultiplier: Float = 1.0f,
    var bounceImpulse: Float = 0.0f,
    var warningMessage: String = "Akses jalan diblokir!",
    var color: FloatArray = floatArrayOf(1f, 0.2f, 0.2f, 0.7f)
) {
    fun getAABB(): AABB {
        val halfX = size.x * 0.5f
        val halfY = size.y * 0.5f
        val halfZ = size.z * 0.5f
        return AABB(
            min = Vec3(position.x - halfX, position.y - halfY, position.z - halfZ),
            max = Vec3(position.x + halfX, position.y + halfY, position.z + halfZ)
        )
    }

    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("id", id)
        obj.put("name", name)
        obj.put("type", type.name)
        obj.put("x", position.x.toDouble())
        obj.put("y", position.y.toDouble())
        obj.put("z", position.z.toDouble())
        obj.put("sizeX", size.x.toDouble())
        obj.put("sizeY", size.y.toDouble())
        obj.put("sizeZ", size.z.toDouble())
        obj.put("isPassable", isPassable)
        obj.put("speedMultiplier", speedMultiplier.toDouble())
        obj.put("bounceImpulse", bounceImpulse.toDouble())
        obj.put("warningMessage", warningMessage)
        return obj
    }

    companion object {
        fun fromJson(obj: JSONObject): WorldBarrier {
            val type = try {
                BarrierType.valueOf(obj.optString("type", "WALL_BARRIER"))
            } catch (e: Exception) {
                BarrierType.WALL_BARRIER
            }

            val color = when (type) {
                BarrierType.WALL_BARRIER -> floatArrayOf(1.0f, 0.15f, 0.15f, 0.65f) // Merah neon
                BarrierType.ROAD_ZONE -> floatArrayOf(0.2f, 0.6f, 1.0f, 0.45f)      // Biru jalan
                BarrierType.SPEED_BOOST_PAD -> floatArrayOf(0.0f, 0.95f, 1.0f, 0.8f) // Cyan turbo
                BarrierType.BOUNCE_PAD -> floatArrayOf(1.0f, 0.9f, 0.0f, 0.8f)      // Kuning pelontar
                BarrierType.KILL_ZONE -> floatArrayOf(0.8f, 0.0f, 0.0f, 0.9f)       // Merah pekat
                BarrierType.CHECKPOINT -> floatArrayOf(0.2f, 1.0f, 0.4f, 0.7f)      // Hijau
            }

            return WorldBarrier(
                id = obj.optString("id", "barrier_${System.currentTimeMillis()}"),
                name = obj.optString("name", "Batas Jalan"),
                type = type,
                position = Vec3(
                    obj.optDouble("x", 0.0).toFloat(),
                    obj.optDouble("y", 0.0).toFloat(),
                    obj.optDouble("z", 0.0).toFloat()
                ),
                size = Vec3(
                    obj.optDouble("sizeX", 2.0).toFloat(),
                    obj.optDouble("sizeY", 3.0).toFloat(),
                    obj.optDouble("sizeZ", 2.0).toFloat()
                ),
                isPassable = obj.optBoolean("isPassable", false),
                speedMultiplier = obj.optDouble("speedMultiplier", 1.0).toFloat(),
                bounceImpulse = obj.optDouble("bounceImpulse", 0.0).toFloat(),
                warningMessage = obj.optString("warningMessage", "Jalanan ini ditutup!"),
                color = color
            )
        }
    }
}
