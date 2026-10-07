package com.example.engine3d.npc

import com.example.engine3d.core.Mesh
import com.example.engine3d.math.Vec3
import org.json.JSONArray
import org.json.JSONObject

enum class NpcBehavior {
    IDLE,
    LOOK_AT_PLAYER,
    PATROL_ROAM
}

data class NpcEntity(
    val id: String,
    var name: String,
    var role: String = "Penduduk",
    val position: Vec3 = Vec3(0f, 0f, 0f),
    var yawDeg: Float = 0f,
    var behavior: NpcBehavior = NpcBehavior.LOOK_AT_PLAYER,
    val dialogues: MutableList<String> = mutableListOf("Halo penjelajah dunia 3D! Selamat datang di Apex Engine."),
    var customMeshName: String? = null,
    var customMesh: Mesh? = null,
    var tintColor: FloatArray = floatArrayOf(0.2f, 0.8f, 0.5f, 1.0f),
    var patrolCenter: Vec3 = position.copy(),
    var patrolRadius: Float = 6.0f,
    var moveSpeed: Float = 1.8f
) {
    var patrolTimer: Float = 0f
    var targetWaypoint: Vec3 = position.copy()

    fun toJson(): String {
        val obj = JSONObject()
        obj.put("id", id)
        obj.put("name", name)
        obj.put("role", role)
        obj.put("x", position.x.toDouble())
        obj.put("y", position.y.toDouble())
        obj.put("z", position.z.toDouble())
        obj.put("yawDeg", yawDeg.toDouble())
        obj.put("behavior", behavior.name)
        obj.put("meshName", customMeshName ?: "")

        val diagArray = JSONArray()
        dialogues.forEach { diagArray.put(it) }
        obj.put("dialogues", diagArray)

        return obj.toString(2)
    }

    companion object {
        fun fromJson(jsonStr: String): NpcEntity? {
            return try {
                val obj = JSONObject(jsonStr)
                val diagList = mutableListOf<String>()
                val dArr = obj.optJSONArray("dialogues")
                if (dArr != null) {
                    for (i in 0 until dArr.length()) {
                        diagList.add(dArr.getString(i))
                    }
                }
                if (diagList.isEmpty()) {
                    diagList.add("Salam hangat, petualang!")
                }

                val x = obj.optDouble("x", 0.0).toFloat()
                val y = obj.optDouble("y", 0.0).toFloat()
                val z = obj.optDouble("z", 0.0).toFloat()

                NpcEntity(
                    id = obj.optString("id", "npc_${System.currentTimeMillis()}"),
                    name = obj.optString("name", "NPC Misterius"),
                    role = obj.optString("role", "Pengelana"),
                    position = Vec3(x, y, z),
                    yawDeg = obj.optDouble("yawDeg", 0.0).toFloat(),
                    behavior = try {
                        NpcBehavior.valueOf(obj.optString("behavior", "LOOK_AT_PLAYER"))
                    } catch (e: Exception) {
                        NpcBehavior.LOOK_AT_PLAYER
                    },
                    dialogues = diagList,
                    customMeshName = obj.optString("meshName").takeIf { it.isNotEmpty() }
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
