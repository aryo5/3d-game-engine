package com.example.engine3d.renderer

import android.content.Context

enum class GraphicPreset(val label: String, val description: String) {
    POTATO_ULTRA_LIGHT("Paling Ringan (Potato)", "30 FPS, Unlit Shader, Tanpa Bayangan/Kabut. Sangat hemat baterai & lancar di HP spek rendah."),
    BALANCED("Seimbang (Balanced)", "60 FPS, Pencahayaan Gouraud, Kabut Atmosfer Lembut. Visual mulus."),
    ULTRA_HD("Ultra HD (Max Quality)", "120 FPS / Uncapped, Pencahayaan Blinn-Phong Per-Pixel, Lampu Dinamis & Bayangan Penuh.")
}

class EngineSettings {
    var activePreset: GraphicPreset = GraphicPreset.BALANCED

    var targetFps: Int = 60
    var resolutionScale: Float = 0.85f
    var lightingQuality: Int = 1 // 0: Unlit, 1: Gouraud, 2: Blinn-Phong Specular
    var enableFog: Boolean = true
    var fogDensity: Float = 0.008f
    var enableWireframe: Boolean = false
    var showCollisionDebug: Boolean = false
    var showPerformanceStats: Boolean = true
    var renderDistance: Float = 250f

    var sunAzimuth: Float = 45f
    var sunElevation: Float = 55f
    var cameraDistance: Float = 6.0f

    // Camera & Character Movement Controls (PUBG-Style & Invert Settings)
    var invertCameraX: Boolean = false
    var invertCameraY: Boolean = false
    var cameraSensitivity: Float = 0.28f
    var invertCharacterMovementX: Boolean = false // Membalikkan gerak strafe/belok tanpa membalikkan maju-mundur
    var invertCharacterFacing: Boolean = false    // Memutar hadap model GLB 180° bila model terbalik
    var twoSidedGlbRendering: Boolean = true      // Render kedua sisi poligon (double-sided / anti culling) agar tidak ada celah tembus pandang pada model pakaian/karakter

    fun saveToPrefs(context: Context) {
        val prefs = context.getSharedPreferences("apex3d_engine_settings", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("preset", activePreset.name)
            .putInt("targetFps", targetFps)
            .putFloat("resolutionScale", resolutionScale)
            .putInt("lightingQuality", lightingQuality)
            .putBoolean("enableFog", enableFog)
            .putFloat("fogDensity", fogDensity)
            .putBoolean("enableWireframe", enableWireframe)
            .putBoolean("showCollisionDebug", showCollisionDebug)
            .putBoolean("showPerformanceStats", showPerformanceStats)
            .putFloat("renderDistance", renderDistance)
            .putFloat("sunAzimuth", sunAzimuth)
            .putFloat("sunElevation", sunElevation)
            .putFloat("cameraDistance", cameraDistance)
            .putBoolean("invertCameraX", invertCameraX)
            .putBoolean("invertCameraY", invertCameraY)
            .putFloat("cameraSensitivity", cameraSensitivity)
            .putBoolean("invertCharacterMovementX", invertCharacterMovementX)
            .putBoolean("invertCharacterFacing", invertCharacterFacing)
            .putBoolean("twoSidedGlbRendering", twoSidedGlbRendering)
            .apply()
    }

    fun loadFromPrefs(context: Context) {
        val prefs = context.getSharedPreferences("apex3d_engine_settings", Context.MODE_PRIVATE)
        val presetStr = prefs.getString("preset", GraphicPreset.BALANCED.name) ?: GraphicPreset.BALANCED.name
        activePreset = try { GraphicPreset.valueOf(presetStr) } catch(e: Exception) { GraphicPreset.BALANCED }
        targetFps = prefs.getInt("targetFps", 60)
        resolutionScale = prefs.getFloat("resolutionScale", 0.85f)
        lightingQuality = prefs.getInt("lightingQuality", 1)
        enableFog = prefs.getBoolean("enableFog", true)
        fogDensity = prefs.getFloat("fogDensity", 0.008f)
        enableWireframe = prefs.getBoolean("enableWireframe", false)
        showCollisionDebug = prefs.getBoolean("showCollisionDebug", false)
        showPerformanceStats = prefs.getBoolean("showPerformanceStats", true)
        renderDistance = prefs.getFloat("renderDistance", 250f)
        sunAzimuth = prefs.getFloat("sunAzimuth", 45f)
        sunElevation = prefs.getFloat("sunElevation", 55f)
        cameraDistance = prefs.getFloat("cameraDistance", 6.0f)
        invertCameraX = prefs.getBoolean("invertCameraX", false)
        invertCameraY = prefs.getBoolean("invertCameraY", false)
        cameraSensitivity = prefs.getFloat("cameraSensitivity", 0.28f)
        invertCharacterMovementX = prefs.getBoolean("invertCharacterMovementX", false)
        invertCharacterFacing = prefs.getBoolean("invertCharacterFacing", false)
        twoSidedGlbRendering = prefs.getBoolean("twoSidedGlbRendering", true)
    }

    fun applyPreset(preset: GraphicPreset) {
        activePreset = preset
        when (preset) {
            GraphicPreset.POTATO_ULTRA_LIGHT -> {
                targetFps = 30
                resolutionScale = 0.60f
                lightingQuality = 0 // Unlit / Ultra light
                enableFog = false
                fogDensity = 0.0f
                renderDistance = 120f
            }
            GraphicPreset.BALANCED -> {
                targetFps = 60
                resolutionScale = 0.85f
                lightingQuality = 1
                enableFog = true
                fogDensity = 0.008f
                renderDistance = 250f
            }
            GraphicPreset.ULTRA_HD -> {
                targetFps = 120
                resolutionScale = 1.0f
                lightingQuality = 2
                enableFog = true
                fogDensity = 0.006f
                renderDistance = 450f
            }
        }
    }
}
