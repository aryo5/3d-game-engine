package com.example.engine3d.scene

import com.example.engine3d.script.Apex3DRealHook

open class CharacterBody3D(name: String = "CharacterBody3D") : Node3D(name) {
    var velocityX: Double = 0.0
    var velocityY: Double = 0.0
    var velocityZ: Double = 0.0

    var speed: Double = 5.0
    var jumpVelocity: Double = 8.0
    var gravity: Double = 9.8
    private var onFloor: Boolean = true

    fun set_velocity(vx: Double, vy: Double, vz: Double) {
        velocityX = vx; velocityY = vy; velocityZ = vz
    }
    fun setVelocity(vx: Double, vy: Double, vz: Double) = set_velocity(vx, vy, vz)

    // Metode Legendaris Godot: Perhitungan Fisika Karakter
    fun move_and_slide() {
        // Terapkan translasi berdasarkan kecepatan
        translate(velocityX * 0.016, velocityY * 0.016, velocityZ * 0.016)

        // Deteksi lantai sederhana
        if (posY <= 0.0) {
            posY = 0.0
            velocityY = 0.0
            onFloor = true
        } else {
            onFloor = false
        }
    }

    fun jump() {
        if (onFloor) {
            velocityY = jumpVelocity
            onFloor = false
            Apex3DRealHook.triggerJump()
        }
    }

    fun is_on_floor(): Boolean = onFloor
    fun isOnFloor(): Boolean = onFloor
}
