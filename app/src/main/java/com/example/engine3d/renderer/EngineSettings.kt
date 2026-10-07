package com.example.engine3d.renderer

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
