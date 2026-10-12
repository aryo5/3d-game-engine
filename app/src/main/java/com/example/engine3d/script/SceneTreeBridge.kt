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
    }
}
