package com.example.engine3d.math

import kotlin.math.max
import kotlin.math.min

data class AABB(
    var min: Vec3 = Vec3(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE),
    var max: Vec3 = Vec3(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE)
) {
    fun reset() {
        min.set(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE)
        max.set(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE)
    }

    fun encircle(p: Vec3) {
        min.x = min(min.x, p.x)
        min.y = min(min.y, p.y)
        min.z = min(min.z, p.z)

        max.x = max(max.x, p.x)
        max.y = max(max.y, p.y)
        max.z = max(max.z, p.z)
    }

    fun contains(p: Vec3): Boolean {
        return p.x in min.x..max.x &&
               p.y in min.y..max.y &&
               p.z in min.z..max.z
    }

    fun intersects(other: AABB): Boolean {
        return (min.x <= other.max.x && max.x >= other.min.x) &&
               (min.y <= other.max.y && max.y >= other.min.y) &&
               (min.z <= other.max.z && max.z >= other.min.z)
    }

    fun center(): Vec3 {
        return Vec3(
            (min.x + max.x) * 0.5f,
            (min.y + max.y) * 0.5f,
            (min.z + max.z) * 0.5f
        )
    }

    fun size(): Vec3 {
        return Vec3(
            max.x - min.x,
            max.y - min.y,
            max.z - min.z
        )
    }
}
