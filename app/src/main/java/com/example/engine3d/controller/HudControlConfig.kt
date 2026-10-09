package com.example.engine3d.controller

import org.json.JSONArray
import org.json.JSONObject

enum class HudControlId(val displayName: String, val iconName: String) {
    JOYSTICK("Virtual Joystick", "sports_esports"),
    LOOK_PAD("Area Kamera (Swipe)", "touch_app"),
    ATTACK("Serang (Attack)", "flash_on"),
    JUMP("Lompat (Jump)", "north"),
    CROUCH("Jongkok (Crouch)", "arrow_downward"),
    ACTION_SLAM("Ground Slam", "terrain"),
    ACTION_DASH("Cyber Dash", "speed"),
    INTERACT("Interaksi (NPC/Benda)", "chat"),
    SPRINT_LOCK("Kunci Lari", "directions_run"),
    CAMERA_SWITCH("Ganti Mode Kamera", "switch_video"),
    FLASHLIGHT("Senter Dunia", "highlight"),
    RESET_POS("Reset Lokasi", "restart_alt"),
    EXPANDED_MENU("Menu Expanded", "apps"),
    STATUS_BAR("Bar Nyawa & Stamina", "favorite"),
    PERF_MONITOR("Monitor FPS & Sistem", "monitor_heart"),
    MINIMAP_RADAR("Mini Radar Kompas", "radar"),
    TOP_BAR_ACTIONS("Tombol Menu & Home", "home"),
    CROSSHAIR("Reticle Titik Bidik", "filter_center_focus")
}

data class HudElementConfig(
    val id: HudControlId,
    var xPercent: Float,      // 0.0 .. 1.0
    var yPercent: Float,      // 0.0 .. 1.0
    var scale: Float = 1.0f,  // 0.5 .. 2.0
    var alpha: Float = 0.85f, // 0.2 .. 1.0
    var isEnabled: Boolean = true
) {
    val percentX: Float get() = xPercent
    val percentY: Float get() = yPercent

    fun copy(): HudElementConfig = HudElementConfig(id, xPercent, yPercent, scale, alpha, isEnabled)
}

object HudPresets {
    fun getPreset(presetName: String): Map<HudControlId, HudElementConfig> {
        val map = mutableMapOf<HudControlId, HudElementConfig>()

        when (presetName) {
            "PUBG_4_FINGER_CLAW" -> {
                // 4-finger claw layout: Index fingers on top corners, thumbs on lower corners
                map[HudControlId.JOYSTICK] = HudElementConfig(HudControlId.JOYSTICK, 0.14f, 0.74f, 1.15f, 0.85f)
                map[HudControlId.SPRINT_LOCK] = HudElementConfig(HudControlId.SPRINT_LOCK, 0.14f, 0.50f, 0.85f, 0.80f)

                // Top Left: Fire / Attack (Left Index Finger)
                map[HudControlId.ATTACK] = HudElementConfig(HudControlId.ATTACK, 0.14f, 0.18f, 1.35f, 0.90f)

                // Top Right: Jump & Crouch (Right Index Finger)
                map[HudControlId.JUMP] = HudElementConfig(HudControlId.JUMP, 0.86f, 0.18f, 1.30f, 0.90f)
                map[HudControlId.CROUCH] = HudElementConfig(HudControlId.CROUCH, 0.72f, 0.18f, 1.10f, 0.85f)

                // Right Thumb area
                map[HudControlId.ACTION_DASH] = HudElementConfig(HudControlId.ACTION_DASH, 0.74f, 0.74f, 1.10f, 0.85f)
                map[HudControlId.ACTION_SLAM] = HudElementConfig(HudControlId.ACTION_SLAM, 0.86f, 0.58f, 1.05f, 0.85f)
                map[HudControlId.INTERACT] = HudElementConfig(HudControlId.INTERACT, 0.50f, 0.70f, 1.15f, 0.95f)
                map[HudControlId.EXPANDED_MENU] = HudElementConfig(HudControlId.EXPANDED_MENU, 0.88f, 0.38f, 1.05f, 0.90f)

                // Top center utilities (tersedia di menu pengaturan atas & wadah jurus radial)
                map[HudControlId.CAMERA_SWITCH] = HudElementConfig(HudControlId.CAMERA_SWITCH, 0.40f, 0.14f, 0.85f, 0.85f, isEnabled = false)
                map[HudControlId.FLASHLIGHT] = HudElementConfig(HudControlId.FLASHLIGHT, 0.50f, 0.14f, 0.85f, 0.85f, isEnabled = false)
                map[HudControlId.RESET_POS] = HudElementConfig(HudControlId.RESET_POS, 0.60f, 0.14f, 0.85f, 0.85f, isEnabled = false)
                map[HudControlId.LOOK_PAD] = HudElementConfig(HudControlId.LOOK_PAD, 0.55f, 0.45f, 1.0f, 0.15f, isEnabled = false)
            }
            "CLAW_3_FINGER" -> {
                map[HudControlId.JOYSTICK] = HudElementConfig(HudControlId.JOYSTICK, 0.14f, 0.74f, 1.1f, 0.85f)
                map[HudControlId.SPRINT_LOCK] = HudElementConfig(HudControlId.SPRINT_LOCK, 0.14f, 0.50f, 0.85f, 0.80f)

                // Top left Attack (Index finger)
                map[HudControlId.ATTACK] = HudElementConfig(HudControlId.ATTACK, 0.14f, 0.18f, 1.35f, 0.90f)

                // Right thumb: Jump, Crouch, Actions
                map[HudControlId.JUMP] = HudElementConfig(HudControlId.JUMP, 0.86f, 0.58f, 1.25f, 0.85f)
                map[HudControlId.ACTION_DASH] = HudElementConfig(HudControlId.ACTION_DASH, 0.74f, 0.74f, 1.10f, 0.85f)
                map[HudControlId.CROUCH] = HudElementConfig(HudControlId.CROUCH, 0.62f, 0.84f, 1.05f, 0.85f)
                map[HudControlId.ACTION_SLAM] = HudElementConfig(HudControlId.ACTION_SLAM, 0.76f, 0.56f, 1.05f, 0.85f)
                map[HudControlId.INTERACT] = HudElementConfig(HudControlId.INTERACT, 0.50f, 0.70f, 1.15f, 0.95f)
                map[HudControlId.EXPANDED_MENU] = HudElementConfig(HudControlId.EXPANDED_MENU, 0.88f, 0.38f, 1.05f, 0.90f)

                map[HudControlId.CAMERA_SWITCH] = HudElementConfig(HudControlId.CAMERA_SWITCH, 0.40f, 0.14f, 0.85f, 0.85f, isEnabled = false)
                map[HudControlId.FLASHLIGHT] = HudElementConfig(HudControlId.FLASHLIGHT, 0.50f, 0.14f, 0.85f, 0.85f, isEnabled = false)
                map[HudControlId.RESET_POS] = HudElementConfig(HudControlId.RESET_POS, 0.60f, 0.14f, 0.85f, 0.85f, isEnabled = false)
                map[HudControlId.LOOK_PAD] = HudElementConfig(HudControlId.LOOK_PAD, 0.55f, 0.45f, 1.0f, 0.15f, isEnabled = false)
            }
            "LEFT_HANDED" -> {
                // Mirrored controls for left-handed players
                map[HudControlId.JOYSTICK] = HudElementConfig(HudControlId.JOYSTICK, 0.86f, 0.74f, 1.15f, 0.85f)
                map[HudControlId.SPRINT_LOCK] = HudElementConfig(HudControlId.SPRINT_LOCK, 0.86f, 0.50f, 0.85f, 0.80f)

                map[HudControlId.ATTACK] = HudElementConfig(HudControlId.ATTACK, 0.15f, 0.76f, 1.30f, 0.90f)
                map[HudControlId.JUMP] = HudElementConfig(HudControlId.JUMP, 0.12f, 0.56f, 1.15f, 0.85f)
                map[HudControlId.ACTION_DASH] = HudElementConfig(HudControlId.ACTION_DASH, 0.28f, 0.76f, 1.10f, 0.85f)
                map[HudControlId.CROUCH] = HudElementConfig(HudControlId.CROUCH, 0.40f, 0.84f, 1.00f, 0.85f)
                map[HudControlId.ACTION_SLAM] = HudElementConfig(HudControlId.ACTION_SLAM, 0.24f, 0.56f, 1.05f, 0.85f)
                map[HudControlId.INTERACT] = HudElementConfig(HudControlId.INTERACT, 0.50f, 0.70f, 1.15f, 0.95f)
                map[HudControlId.EXPANDED_MENU] = HudElementConfig(HudControlId.EXPANDED_MENU, 0.12f, 0.38f, 1.05f, 0.90f)

                map[HudControlId.CAMERA_SWITCH] = HudElementConfig(HudControlId.CAMERA_SWITCH, 0.60f, 0.14f, 0.85f, 0.85f, isEnabled = false)
                map[HudControlId.FLASHLIGHT] = HudElementConfig(HudControlId.FLASHLIGHT, 0.50f, 0.14f, 0.85f, 0.85f, isEnabled = false)
                map[HudControlId.RESET_POS] = HudElementConfig(HudControlId.RESET_POS, 0.40f, 0.14f, 0.85f, 0.85f, isEnabled = false)
                map[HudControlId.LOOK_PAD] = HudElementConfig(HudControlId.LOOK_PAD, 0.45f, 0.45f, 1.0f, 0.15f, isEnabled = false)
            }
            else -> {
                // DEFAULT_2_FINGER (Tata letak ergonomis standar Action RPG / PUBG Mobile)
                // Sisi Kiri Bawah: Virtual Joystick & Sprint Lock
                map[HudControlId.JOYSTICK] = HudElementConfig(HudControlId.JOYSTICK, 0.14f, 0.74f, 1.15f, 0.85f)
                map[HudControlId.SPRINT_LOCK] = HudElementConfig(HudControlId.SPRINT_LOCK, 0.14f, 0.46f, 0.85f, 0.80f)

                // Sisi Kanan Bawah: Kluster Tombol Aksi Melengkung (Ergonomis Jempol Kanan)
                map[HudControlId.ATTACK] = HudElementConfig(HudControlId.ATTACK, 0.86f, 0.75f, 1.30f, 0.90f)
                map[HudControlId.JUMP] = HudElementConfig(HudControlId.JUMP, 0.90f, 0.52f, 1.15f, 0.85f)
                map[HudControlId.ACTION_DASH] = HudElementConfig(HudControlId.ACTION_DASH, 0.74f, 0.75f, 1.05f, 0.85f)
                map[HudControlId.CROUCH] = HudElementConfig(HudControlId.CROUCH, 0.63f, 0.84f, 1.00f, 0.85f)
                map[HudControlId.ACTION_SLAM] = HudElementConfig(HudControlId.ACTION_SLAM, 0.77f, 0.56f, 1.05f, 0.85f)
                map[HudControlId.EXPANDED_MENU] = HudElementConfig(HudControlId.EXPANDED_MENU, 0.90f, 0.33f, 1.00f, 0.90f)

                // Tengah Bawah: Tombol Interaksi Cepat (Bicara / Buka Peti / Portal)
                map[HudControlId.INTERACT] = HudElementConfig(HudControlId.INTERACT, 0.50f, 0.74f, 1.15f, 0.95f)

                // Sisi Atas Layar: Utilitas Cepat (tersedia di menu pengaturan atas & wadah jurus radial)
                map[HudControlId.CAMERA_SWITCH] = HudElementConfig(HudControlId.CAMERA_SWITCH, 0.40f, 0.14f, 0.85f, 0.85f, isEnabled = false)
                map[HudControlId.FLASHLIGHT] = HudElementConfig(HudControlId.FLASHLIGHT, 0.50f, 0.14f, 0.85f, 0.85f, isEnabled = false)
                map[HudControlId.RESET_POS] = HudElementConfig(HudControlId.RESET_POS, 0.60f, 0.14f, 0.85f, 0.85f, isEnabled = false)

                // Look pad indicator (kamera dapat diusap bebas di seluruh sisi kanan layar)
                map[HudControlId.LOOK_PAD] = HudElementConfig(HudControlId.LOOK_PAD, 0.55f, 0.45f, 1.0f, 0.15f, isEnabled = false)
            }
        }

        // Common defaults for system HUD elements across all presets if not explicitly defined
        if (!map.containsKey(HudControlId.STATUS_BAR)) {
            map[HudControlId.STATUS_BAR] = HudElementConfig(HudControlId.STATUS_BAR, 0.16f, 0.08f, 1.0f, 0.90f)
        }
        if (!map.containsKey(HudControlId.PERF_MONITOR)) {
            map[HudControlId.PERF_MONITOR] = HudElementConfig(HudControlId.PERF_MONITOR, 0.16f, 0.17f, 1.0f, 0.85f)
        }
        if (!map.containsKey(HudControlId.MINIMAP_RADAR)) {
            map[HudControlId.MINIMAP_RADAR] = HudElementConfig(HudControlId.MINIMAP_RADAR, 0.50f, 0.08f, 0.90f, 0.80f)
        }
        if (!map.containsKey(HudControlId.TOP_BAR_ACTIONS)) {
            map[HudControlId.TOP_BAR_ACTIONS] = HudElementConfig(HudControlId.TOP_BAR_ACTIONS, 0.88f, 0.08f, 1.0f, 0.90f)
        }
        if (!map.containsKey(HudControlId.CROSSHAIR)) {
            map[HudControlId.CROSSHAIR] = HudElementConfig(HudControlId.CROSSHAIR, 0.50f, 0.50f, 1.0f, 0.65f)
        }

        return map
    }

    fun serializeMap(configs: Map<HudControlId, HudElementConfig>): String {
        val array = JSONArray()
        for ((_, item) in configs) {
            val obj = JSONObject()
            obj.put("id", item.id.name)
            obj.put("x", item.xPercent.toDouble())
            obj.put("y", item.yPercent.toDouble())
            obj.put("scale", item.scale.toDouble())
            obj.put("alpha", item.alpha.toDouble())
            obj.put("enabled", item.isEnabled)
            array.put(obj)
        }
        return array.toString()
    }

    fun deserializeMap(jsonStr: String): Map<HudControlId, HudElementConfig>? {
        return try {
            val array = JSONArray(jsonStr)
            val map = mutableMapOf<HudControlId, HudElementConfig>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val idStr = obj.getString("id")
                val id = try { HudControlId.valueOf(idStr) } catch (e: Exception) { null }
                if (id != null) {
                    val cfg = HudElementConfig(
                        id = id,
                        xPercent = obj.getDouble("x").toFloat(),
                        yPercent = obj.getDouble("y").toFloat(),
                        scale = obj.getDouble("scale").toFloat(),
                        alpha = obj.getDouble("alpha").toFloat(),
                        isEnabled = obj.optBoolean("enabled", true)
                    )
                    map[id] = cfg
                }
            }

            // Fill in missing default HUD elements if older preset format was saved
            val defaults = getPreset("DEFAULT_2_FINGER")
            defaults.forEach { (id, defCfg) ->
                if (!map.containsKey(id)) {
                    map[id] = defCfg.copy()
                }
            }

            map
        } catch (e: Exception) {
            null
        }
    }
}
