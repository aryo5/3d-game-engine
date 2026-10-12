package com.example.engine3d.scene

class MeshInstance3D(name: String = "MeshInstance3D", var modelPath: String = "") : Node3D(name) {
    var currentAnimation: String = "Idle"

    fun play_animation(animName: String) {
        currentAnimation = animName
        com.example.engine3d.script.Apex3DRealHook.triggerAction(animName)
    }
    fun play(animName: String) = play_animation(animName)
    fun playAnimation(animName: String) = play_animation(animName)
}
