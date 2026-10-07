package com.example.engine3d.importer

data class ModelRiggingConfig(
    var targetRole: ModelTarget = ModelTarget.CHARACTER,
    var modelScale: Float = 1.0f,
    var heightOffset: Float = 0.0f,
    var walkSpeedMultiplier: Float = 1.0f,
    var attackRadiusMultiplier: Float = 1.0f,
    var jumpImpulseMultiplier: Float = 1.0f,
    var maxClimbAngleDeg: Float = 45.0f,
    var terrainFriction: Float = 0.85f,
    var auraGlowHex: String = "#00E5FF"
)
