package com.example.engine3d.importer

import org.json.JSONObject

data class PlayerConfig(
    val characterName: String = "Farmer Pete",
    val modelFile: String = "farmer_harvest_moon.glb",
    val scaleX: Float = 1.0f,
    val scaleY: Float = 1.0f,
    val scaleZ: Float = 1.0f,
    val rotationOffsetYDeg: Float = 0f,
    val heightOffset: Float = -0.10f,
    val collisionRadius: Float = 0.5f,
    val collisionHeight: Float = 1.5f,
    val walkSpeed: Float = 6.5f,
    val runMultiplier: Float = 1.6f,
    val jumpImpulse: Float = 11.5f,
    val animIdleName: String = "idle",
    val animWalkName: String = "Walk",
    val animRunName: String = "Walk",
    val animJumpName: String = "jump",
    val animSlashName: String = "slash"
) {
    fun toJson(): String {
        val obj = JSONObject()
        obj.put("characterName", characterName)
        obj.put("modelFile", modelFile)
        obj.put("scaleX", scaleX.toDouble())
        obj.put("scaleY", scaleY.toDouble())
        obj.put("scaleZ", scaleZ.toDouble())
        obj.put("rotationOffsetYDeg", rotationOffsetYDeg.toDouble())
        obj.put("heightOffset", heightOffset.toDouble())

        val col = JSONObject()
        col.put("radius", collisionRadius.toDouble())
        col.put("height", collisionHeight.toDouble())
        obj.put("collision", col)

        val mov = JSONObject()
        mov.put("walkSpeed", walkSpeed.toDouble())
        mov.put("runMultiplier", runMultiplier.toDouble())
        mov.put("jumpImpulse", jumpImpulse.toDouble())
        obj.put("movement", mov)

        val anims = JSONObject()
        anims.put("idle", animIdleName)
        anims.put("walk", animWalkName)
        anims.put("run", animRunName)
        anims.put("jump", animJumpName)
        anims.put("slash", animSlashName)
        obj.put("animations", anims)

        return obj.toString(2)
    }

    companion object {
        fun fromJson(jsonStr: String): PlayerConfig {
            return try {
                val obj = JSONObject(jsonStr)
                val col = obj.optJSONObject("collision")
                val mov = obj.optJSONObject("movement")
                val anims = obj.optJSONObject("animations")

                PlayerConfig(
                    characterName = obj.optString("characterName", "Farmer Pete"),
                    modelFile = obj.optString("modelFile", "farmer_harvest_moon.glb"),
                    scaleX = obj.optDouble("scaleX", 1.0).toFloat(),
                    scaleY = obj.optDouble("scaleY", 1.0).toFloat(),
                    scaleZ = obj.optDouble("scaleZ", 1.0).toFloat(),
                    rotationOffsetYDeg = obj.optDouble("rotationOffsetYDeg", 0.0).toFloat(),
                    heightOffset = obj.optDouble("heightOffset", -0.10).toFloat(),
                    collisionRadius = col?.optDouble("radius", 0.6)?.toFloat() ?: obj.optDouble("collisionRadius", 0.6).toFloat(),
                    collisionHeight = col?.optDouble("height", 1.8)?.toFloat() ?: obj.optDouble("collisionHeight", 1.8).toFloat(),
                    walkSpeed = mov?.optDouble("walkSpeed", 6.5)?.toFloat() ?: obj.optDouble("walkSpeed", 6.5).toFloat(),
                    runMultiplier = mov?.optDouble("runMultiplier", 1.6)?.toFloat() ?: obj.optDouble("runMultiplier", 1.6).toFloat(),
                    jumpImpulse = mov?.optDouble("jumpImpulse", 11.5)?.toFloat() ?: obj.optDouble("jumpImpulse", 11.5).toFloat(),
                    animIdleName = anims?.optString("idle", "anim_idle") ?: "anim_idle",
                    animWalkName = anims?.optString("walk", "anim_walk") ?: "anim_walk",
                    animRunName = anims?.optString("run", "anim_run") ?: "anim_run",
                    animJumpName = anims?.optString("jump", "anim_jump") ?: "anim_jump",
                    animSlashName = anims?.optString("slash", "anim_slash") ?: "anim_slash"
                )
            } catch (e: Exception) {
                PlayerConfig()
            }
        }
    }
}
