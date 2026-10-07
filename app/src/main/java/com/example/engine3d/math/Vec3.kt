package com.example.engine3d.math

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class Vec3(
    var x: Float = 0f,
    var y: Float = 0f,
    var z: Float = 0f
) {
    fun set(nx: Float, ny: Float, nz: Float): Vec3 {
        x = nx
        y = ny
        z = nz
        return this
    }

    fun set(other: Vec3): Vec3 {
        x = other.x
        y = other.y
        z = other.z
        return this
    }

    fun add(other: Vec3): Vec3 = Vec3(x + other.x, y + other.y, z + other.z)
    fun add(nx: Float, ny: Float, nz: Float): Vec3 = Vec3(x + nx, y + ny, z + nz)
    fun addSelf(other: Vec3): Vec3 {
        x += other.x
        y += other.y
        z += other.z
        return this
    }

    fun sub(other: Vec3): Vec3 = Vec3(x - other.x, y - other.y, z - other.z)
    fun subSelf(other: Vec3): Vec3 {
        x -= other.x
        y -= other.y
        z -= other.z
        return this
    }

    fun scale(factor: Float): Vec3 = Vec3(x * factor, y * factor, z * factor)
    fun scaleSelf(factor: Float): Vec3 {
        x *= factor
        y *= factor
        z *= factor
        return this
    }

    fun dot(other: Vec3): Float = x * other.x + y * other.y + z * other.z

    fun cross(other: Vec3): Vec3 = Vec3(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x
    )

    fun length(): Float = sqrt(x * x + y * y + z * z)
    fun lengthSquared(): Float = x * x + y * y + z * z

    fun normalize(): Vec3 {
        val len = length()
        return if (len > 0.00001f) {
            Vec3(x / len, y / len, z / len)
        } else {
            Vec3(0f, 1f, 0f)
        }
    }

    fun normalizeSelf(): Vec3 {
        val len = length()
        if (len > 0.00001f) {
            x /= len
            y /= len
            z /= len
        }
        return this
    }

    fun distanceTo(other: Vec3): Float = sub(other).length()

    fun copy(): Vec3 = Vec3(x, y, z)

    companion object {
        val ZERO get() = Vec3(0f, 0f, 0f)
        val UP get() = Vec3(0f, 1f, 0f)
        val FORWARD get() = Vec3(0f, 0f, -1f)
        val RIGHT get() = Vec3(1f, 0f, 0f)

        fun lerp(a: Vec3, b: Vec3, t: Float): Vec3 {
            val clampedT = t.coerceIn(0f, 1f)
            return Vec3(
                a.x + (b.x - a.x) * clampedT,
                a.y + (b.y - a.y) * clampedT,
                a.z + (b.z - a.z) * clampedT
            )
        }
    }
}
