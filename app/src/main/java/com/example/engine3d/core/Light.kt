package com.example.engine3d.core

import com.example.engine3d.math.Vec3
import kotlin.math.cos
import kotlin.math.sin

data class PointLight(
    var position: Vec3 = Vec3(0f, 2f, 0f),
    var color: FloatArray = floatArrayOf(1f, 0.8f, 0.4f),
    var intensity: Float = 1.5f,
    var radius: Float = 15f
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as PointLight
        if (position != other.position) return false
        if (!color.contentEquals(other.color)) return false
        if (intensity != other.intensity) return false
        if (radius != other.radius) return false
        return true
    }

    override fun hashCode(): Int {
        var result = position.hashCode()
        result = 31 * result + color.contentHashCode()
        result = 31 * result + intensity.hashCode()
        result = 31 * result + radius.hashCode()
        return result
    }
}

class LightingEnvironment {
    var sunAzimuthDeg: Float = 45f
    var sunElevationDeg: Float = 55f
    var sunColor: FloatArray = floatArrayOf(1.0f, 0.96f, 0.88f)
    var ambientColor: FloatArray = floatArrayOf(0.38f, 0.42f, 0.48f)
    var fogColor: FloatArray = floatArrayOf(0.55f, 0.70f, 0.85f)
    var fogDensity: Float = 0.008f
    var fogEnabled: Boolean = true

    val pointLights = mutableListOf<PointLight>()

    init {
        // Default beacon/campfire point light
        pointLights.add(PointLight(Vec3(0f, 2.5f, 0f), floatArrayOf(0.2f, 0.9f, 1.0f), 2.0f, 18f))
    }

    fun getSunDirection(): Vec3 {
        val azRad = Math.toRadians(sunAzimuthDeg.toDouble())
        val elRad = Math.toRadians(sunElevationDeg.toDouble())

        val y = sin(elRad).toFloat()
        val horiz = cos(elRad).toFloat()
        val x = (horiz * sin(azRad)).toFloat()
        val z = (horiz * cos(azRad)).toFloat()

        return Vec3(x, y, z).normalize()
    }
}
