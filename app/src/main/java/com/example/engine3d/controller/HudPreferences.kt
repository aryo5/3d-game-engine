package com.example.engine3d.controller

import android.content.Context
import android.content.SharedPreferences

class HudPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("apex3d_hud_prefs", Context.MODE_PRIVATE)

    fun saveLayout(presetName: String, configs: Map<HudControlId, HudElementConfig>) {
        val json = HudPresets.serializeMap(configs)
        prefs.edit()
            .putString("layout_$presetName", json)
            .putString("active_preset", presetName)
            .apply()
    }

    fun loadLayout(presetName: String): Map<HudControlId, HudElementConfig> {
        val json = prefs.getString("layout_$presetName", null)
        if (json != null) {
            val deserialized = HudPresets.deserializeMap(json)
            if (deserialized != null && deserialized.isNotEmpty()) {
                return deserialized
            }
        }
        return HudPresets.getPreset(presetName)
    }

    fun getActivePresetName(): String {
        return prefs.getString("active_preset", "DEFAULT_2_FINGER") ?: "DEFAULT_2_FINGER"
    }

    fun setActivePresetName(name: String) {
        prefs.edit().putString("active_preset", name).apply()
    }

    fun resetPreset(presetName: String) {
        prefs.edit().remove("layout_$presetName").apply()
    }
}
