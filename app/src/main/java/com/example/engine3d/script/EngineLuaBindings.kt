package com.example.engine3d.script

import android.util.Log

class PlayerBinding {
    var posX = 0.0
    var posY = 0.0
    var posZ = 0.0
    var rotY = 0.0
    private var _scale = 1.0
    val scale: Double get() = _scale
    var currentAnim = "Idle"

    fun setPosition(x: Double, y: Double, z: Double) {
        posX = x; posY = y; posZ = z
        Log.d("Apex3D-Lua", "Player pos: ($x, $y, $z)")
    }

    fun translate(dx: Double, dy: Double, dz: Double) {
        posX += dx; posY += dy; posZ += dz
    }

    fun rotateY(deg: Double) {
        rotY = (rotY + deg) % 360.0
    }

    fun setScale(s: Double) {
        _scale = s
    }

    fun playAnimation(animName: String) {
        currentAnim = animName
        Log.d("Apex3D-Lua", "Player animation: $animName")
    }

    fun getX(): Double = posX
    fun getY(): Double = posY
    fun getZ(): Double = posZ
}

class CameraBinding {
    var posX = 0.0
    var posY = 3.0
    var posZ = -6.0
    private var _fov = 60.0
    val fov: Double get() = _fov

    fun setPosition(x: Double, y: Double, z: Double) {
        posX = x; posY = y; posZ = z
    }

    fun setFov(newFov: Double) {
        _fov = newFov
    }

    fun lookAt(x: Double, y: Double, z: Double) {
        Log.d("Apex3D-Lua", "Camera lookAt: ($x, $y, $z)")
    }
}

class WorldBinding {
    var timeOfDay = 12.0
    private var _sunIntensity = 1.0
    val sunIntensity: Double get() = _sunIntensity
    private var _gravity = -9.8
    val gravity: Double get() = _gravity

    fun setTime(hour: Double) {
        timeOfDay = hour % 24.0
    }

    fun setSunIntensity(intensity: Double) {
        _sunIntensity = intensity
    }

    fun setGravity(g: Double) {
        _gravity = g
    }

    fun getTime(): Double = timeOfDay
}

object LuaScriptManager {
    val engine = LuaEngine()
    val player = PlayerBinding()
    val camera = CameraBinding()
    val world = WorldBinding()

    init {
        engine.bind("player", player)
        engine.bind("camera", camera)
        engine.bind("world", world)
    }
}
