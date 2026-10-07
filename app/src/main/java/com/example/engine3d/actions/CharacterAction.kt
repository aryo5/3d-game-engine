package com.example.engine3d.actions

import org.json.JSONObject

data class CharacterAction(
    val id: String,
    val name: String,
    val description: String,
    val durationSec: Float = 0.6f,
    val speedMultiplier: Float = 1.0f,
    val jumpImpulse: Float = 0.0f,
    val damage: Float = 35.0f,
    val radius: Float = 2.5f,
    val cooldownSec: Float = 1.0f,
    val vfxColorHex: String = "#00E5FF",
    val animationType: String = "SLASH" // SLASH, SLAM, DASH, LEAP, SPIN
) {
    fun toJson(): String {
        val obj = JSONObject()
        obj.put("id", id)
        obj.put("name", name)
        obj.put("description", description)
        obj.put("durationSec", durationSec.toDouble())
        obj.put("speedMultiplier", speedMultiplier.toDouble())
        obj.put("jumpImpulse", jumpImpulse.toDouble())
        obj.put("damage", damage.toDouble())
        obj.put("radius", radius.toDouble())
        obj.put("cooldownSec", cooldownSec.toDouble())
        obj.put("vfxColorHex", vfxColorHex)
        obj.put("animationType", animationType)
        return obj.toString(2)
    }

    companion object {
        fun fromJson(jsonStr: String): CharacterAction? {
            return try {
                val obj = JSONObject(jsonStr)
                CharacterAction(
                    id = obj.optString("id", "custom_action_${System.currentTimeMillis()}"),
                    name = obj.optString("name", "Custom Action"),
                    description = obj.optString("description", "Imported external action file"),
                    durationSec = obj.optDouble("durationSec", 0.6).toFloat(),
                    speedMultiplier = obj.optDouble("speedMultiplier", 1.2).toFloat(),
                    jumpImpulse = obj.optDouble("jumpImpulse", 0.0).toFloat(),
                    damage = obj.optDouble("damage", 40.0).toFloat(),
                    radius = obj.optDouble("radius", 2.5).toFloat(),
                    cooldownSec = obj.optDouble("cooldownSec", 1.5).toFloat(),
                    vfxColorHex = obj.optString("vfxColorHex", "#FF9100"),
                    animationType = obj.optString("animationType", "SLASH")
                )
            } catch (e: Exception) {
                null
            }
        }

        val DEFAULT_SLASH = CharacterAction(
            id = "action_slash",
            name = "Energy Slash",
            description = "Quick horizontal plasma blade strike",
            durationSec = 0.45f,
            speedMultiplier = 1.1f,
            jumpImpulse = 0f,
            damage = 30f,
            radius = 2.2f,
            cooldownSec = 0.5f,
            vfxColorHex = "#00E5FF",
            animationType = "SLASH"
        )

        val DEFAULT_SLAM = CharacterAction(
            id = "action_slam",
            name = "Ground Slam",
            description = "High jump followed by shockwave impact",
            durationSec = 0.85f,
            speedMultiplier = 0.5f,
            jumpImpulse = 6.0f,
            damage = 75f,
            radius = 4.5f,
            cooldownSec = 3.0f,
            vfxColorHex = "#FF3D00",
            animationType = "SLAM"
        )

        val DEFAULT_DASH = CharacterAction(
            id = "action_dash",
            name = "Cyber Dash",
            description = "High-speed forward propulsion burst",
            durationSec = 0.35f,
            speedMultiplier = 3.2f,
            jumpImpulse = 0.5f,
            damage = 20f,
            radius = 1.8f,
            cooldownSec = 2.0f,
            vfxColorHex = "#76FF03",
            animationType = "DASH"
        )
    }
}
