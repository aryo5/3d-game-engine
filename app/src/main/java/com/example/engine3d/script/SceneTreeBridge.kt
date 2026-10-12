package com.example.engine3d.script

import com.example.engine3d.scene.*

object SceneTreeManager {
    val tree = SceneTree()
    val playerNode = CharacterBody3D("Player")
    val cameraNode = Camera3D("Camera3D")
    val farmerMesh = MeshInstance3D("FarmerMesh", "farmer_harvest_moon.glb")

    init {
        playerNode.add_child(farmerMesh)
        tree.root.add_child(playerNode)
        tree.root.add_child(cameraNode)

        // SAMBUNGKAN JANTUNG FRAME LOOP 60 FPS DARI ENGINE KE SCENETREE & LUA
        Apex3DRealHook.onFrameUpdate = { dt ->
            val delta = dt.toDouble()
            // 1. Eksekusi siklus proses Godot per frame
            tree.physics_process(delta)
            tree.process(delta)
            // 2. Eksekusi fungsi _process(delta) di script Lua 60 FPS
            LuaScriptManager.engine.update(delta)
        }
    }
}
