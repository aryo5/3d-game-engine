package com.example.engine3d.scene

class Camera3D(name: String = "Camera3D") : Node3D(name) {
    private var _fov: Double = 60.0
    val fov: Double get() = _fov
    var distance: Double = 6.0
    var orbitAngle: Double = 0.0

    fun set_fov(newFov: Double) { _fov = newFov }
    fun setFov(newFov: Double) { _fov = newFov }

    fun orbit_around(target: Node3D, angleDelta: Double) {
        orbitAngle = (orbitAngle + angleDelta) % 360.0
        val rad = Math.toRadians(orbitAngle)
        posX = target.posX + distance * Math.sin(rad)
        posZ = target.posZ + distance * Math.cos(rad)
        posY = target.posY + 3.0
        look_at(target.posX, target.posY, target.posZ)
    }
}
