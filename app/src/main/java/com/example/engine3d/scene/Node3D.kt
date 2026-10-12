package com.example.engine3d.scene

open class Node3D(name: String = "Node3D") : Node(name) {
    var posX: Double = 0.0
    var posY: Double = 0.0
    var posZ: Double = 0.0

    var rotX: Double = 0.0
    var rotY: Double = 0.0
    var rotZ: Double = 0.0

    var scaleX: Double = 1.0
    var scaleY: Double = 1.0
    var scaleZ: Double = 1.0

    fun set_position(x: Double, y: Double, z: Double) {
        posX = x; posY = y; posZ = z
    }
    fun setPosition(x: Double, y: Double, z: Double) = set_position(x, y, z)

    fun translate(dx: Double, dy: Double, dz: Double) {
        posX += dx; posY += dy; posZ += dz
    }

    fun rotate_y(deg: Double) {
        rotY = (rotY + deg) % 360.0
    }
    fun rotateY(deg: Double) = rotate_y(deg)

    fun set_scale(s: Double) {
        scaleX = s; scaleY = s; scaleZ = s
    }
    fun setScale(s: Double) = set_scale(s)

    fun look_at(targetX: Double, targetY: Double, targetZ: Double) {
        val dx = targetX - posX
        val dz = targetZ - posZ
        rotY = Math.toDegrees(Math.atan2(dx, dz))
    }
    fun lookAt(x: Double, y: Double, z: Double) = look_at(x, y, z)

    override fun toString(): String = "$name (Pos: ${String.format("%.1f", posX)}, ${String.format("%.1f", posY)}, ${String.format("%.1f", posZ)})"
}
