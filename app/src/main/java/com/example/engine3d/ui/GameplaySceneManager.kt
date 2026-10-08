package com.example.engine3d.ui

import android.content.Context
import android.util.Log
import com.example.engine3d.actions.InteractableType
import com.example.engine3d.actions.InteractionSystem
import com.example.engine3d.actions.WorldInteractable
import com.example.engine3d.importer.SampleExportManager
import com.example.engine3d.math.Vec3
import com.example.engine3d.npc.NpcBehavior
import com.example.engine3d.npc.NpcEntity
import com.example.engine3d.npc.NpcManager
import com.example.engine3d.physics.BarrierManager
import com.example.engine3d.physics.BarrierType
import com.example.engine3d.physics.PhysicsEngine
import com.example.engine3d.physics.WorldBarrier
import com.example.engine3d.renderer.Apex3DRenderer
import com.example.engine3d.renderer.EngineSettings
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Mengelola penyimpanan, pemuatan, dan ekspor konfigurasi seluruh dunia gameplay:
 * Termasuk rintangan/barikade, NPC kustom, portal teleport, interaksi, fisika, dan cuaca.
 */
class GameplaySceneManager(private val context: Context) {

    private val sceneConfigFile = File(context.filesDir, "custom_gameplay_scene.json")

    fun saveScene(
        physicsEngine: PhysicsEngine,
        barrierManager: BarrierManager,
        npcManager: NpcManager,
        interactionSystem: InteractionSystem,
        renderer: Apex3DRenderer,
        settings: EngineSettings
    ): Boolean {
        return try {
            val root = JSONObject()

            // 1. Fisika Karakter
            val physObj = JSONObject().apply {
                put("walkSpeed", physicsEngine.walkSpeed.toDouble())
                put("sprintSpeed", physicsEngine.sprintSpeed.toDouble())
                put("jumpImpulse", physicsEngine.jumpImpulse.toDouble())
                put("gravity", physicsEngine.gravity.toDouble())
                put("maxClimbableSlope", physicsEngine.maxClimbableSlope.toDouble())
                put("stepHeight", physicsEngine.stepHeight.toDouble())
            }
            root.put("physics", physObj)

            // 2. Barikade & Batas Dunia
            val barriersArray = JSONArray()
            barrierManager.barriers.forEach { b ->
                val bObj = JSONObject().apply {
                    put("id", b.id)
                    put("name", b.name)
                    put("type", b.type.name)
                    put("posX", b.position.x.toDouble())
                    put("posY", b.position.y.toDouble())
                    put("posZ", b.position.z.toDouble())
                    put("sizeX", b.size.x.toDouble())
                    put("sizeY", b.size.y.toDouble())
                    put("sizeZ", b.size.z.toDouble())
                    put("isPassable", b.isPassable)
                    put("speedMultiplier", b.speedMultiplier.toDouble())
                    put("bounceImpulse", b.bounceImpulse.toDouble())
                    put("warningMessage", b.warningMessage)
                }
                barriersArray.put(bObj)
            }
            root.put("barriers", barriersArray)

            // 3. NPC & Dialog
            val npcsArray = JSONArray()
            npcManager.npcs.forEach { npc ->
                val nObj = JSONObject().apply {
                    put("id", npc.id)
                    put("name", npc.name)
                    put("role", npc.role)
                    put("posX", npc.position.x.toDouble())
                    put("posY", npc.position.y.toDouble())
                    put("posZ", npc.position.z.toDouble())
                    put("behavior", npc.behavior.name)

                    val dArray = JSONArray()
                    npc.dialogues.forEach { dArray.put(it) }
                    put("dialogues", dArray)
                }
                npcsArray.put(nObj)
            }
            root.put("npcs", npcsArray)

            // 4. Benda Interaktif & Portal
            val interactablesArray = JSONArray()
            interactionSystem.interactables.forEach { item ->
                val iObj = JSONObject().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("type", item.type.name)
                    put("posX", item.position.x.toDouble())
                    put("posY", item.position.y.toDouble())
                    put("posZ", item.position.z.toDouble())
                    put("promptText", item.promptText)
                    if (item.targetTeleportPos != null) {
                        put("targetX", item.targetTeleportPos.x.toDouble())
                        put("targetY", item.targetTeleportPos.y.toDouble())
                        put("targetZ", item.targetTeleportPos.z.toDouble())
                    }
                }
                interactablesArray.put(iObj)
            }
            root.put("interactables", interactablesArray)

            // 5. Grafis, Kabut & Pencahayaan
            val lightingObj = JSONObject().apply {
                put("enableFog", settings.enableFog)
                put("fogDensity", settings.fogDensity.toDouble())
                put("sunAzimuthDeg", renderer.lighting.sunAzimuthDeg.toDouble())
                put("sunElevationDeg", renderer.lighting.sunElevationDeg.toDouble())
                put("ambientR", renderer.lighting.ambientColor[0].toDouble())
                put("ambientG", renderer.lighting.ambientColor[1].toDouble())
                put("ambientB", renderer.lighting.ambientColor[2].toDouble())
            }
            root.put("lighting", lightingObj)

            sceneConfigFile.writeText(root.toString(2))
            Log.d("GameplaySceneManager", "Scene saved successfully to: ${sceneConfigFile.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e("GameplaySceneManager", "Failed to save scene", e)
            false
        }
    }

    fun loadSavedScene(
        physicsEngine: PhysicsEngine,
        barrierManager: BarrierManager,
        npcManager: NpcManager,
        interactionSystem: InteractionSystem,
        renderer: Apex3DRenderer,
        settings: EngineSettings
    ): Boolean {
        if (!sceneConfigFile.exists()) return false

        return try {
            val jsonStr = sceneConfigFile.readText()
            val root = JSONObject(jsonStr)

            // 1. Fisika
            if (root.has("physics")) {
                val p = root.getJSONObject("physics")
                physicsEngine.walkSpeed = p.optDouble("walkSpeed", 5.2).toFloat()
                physicsEngine.sprintSpeed = p.optDouble("sprintSpeed", 8.8).toFloat()
                physicsEngine.jumpImpulse = p.optDouble("jumpImpulse", 7.8).toFloat()
                physicsEngine.gravity = p.optDouble("gravity", -19.6).toFloat()
                physicsEngine.maxClimbableSlope = p.optDouble("maxClimbableSlope", 46.0).toFloat()
                physicsEngine.stepHeight = p.optDouble("stepHeight", 0.5).toFloat()
            }

            // 2. Barikade
            if (root.has("barriers")) {
                val bArray = root.getJSONArray("barriers")
                barrierManager.barriers.clear()
                for (i in 0 until bArray.length()) {
                    val bObj = bArray.getJSONObject(i)
                    val barrier = WorldBarrier(
                        id = bObj.getString("id"),
                        name = bObj.getString("name"),
                        type = try { BarrierType.valueOf(bObj.getString("type")) } catch (e: Exception) { BarrierType.WALL_BARRIER },
                        position = Vec3(bObj.getDouble("posX").toFloat(), bObj.getDouble("posY").toFloat(), bObj.getDouble("posZ").toFloat()),
                        size = Vec3(bObj.getDouble("sizeX").toFloat(), bObj.getDouble("sizeY").toFloat(), bObj.getDouble("sizeZ").toFloat()),
                        isPassable = bObj.optBoolean("isPassable", false),
                        speedMultiplier = bObj.optDouble("speedMultiplier", 1.0).toFloat(),
                        bounceImpulse = bObj.optDouble("bounceForce", 14.0).toFloat(),
                        warningMessage = bObj.optString("warningMessage", "Akses jalan diblokir!")
                    )
                    barrierManager.barriers.add(barrier)
                }
            }

            // 3. NPC
            if (root.has("npcs")) {
                val nArray = root.getJSONArray("npcs")
                npcManager.npcs.clear()
                for (i in 0 until nArray.length()) {
                    val nObj = nArray.getJSONObject(i)
                    val dialogues = mutableListOf<String>()
                    val dArr = nObj.optJSONArray("dialogues")
                    if (dArr != null) {
                        for (d in 0 until dArr.length()) dialogues.add(dArr.getString(d))
                    }
                    val npc = NpcEntity(
                        id = nObj.getString("id"),
                        name = nObj.getString("name"),
                        role = nObj.optString("role", "Penduduk"),
                        position = Vec3(nObj.getDouble("posX").toFloat(), nObj.getDouble("posY").toFloat(), nObj.getDouble("posZ").toFloat()),
                        dialogues = dialogues,
                        behavior = try { NpcBehavior.valueOf(nObj.optString("behavior", "LOOK_AT_PLAYER")) } catch (e: Exception) { NpcBehavior.LOOK_AT_PLAYER }
                    )
                    npcManager.npcs.add(npc)
                }
            }

            // 4. Interaksi
            if (root.has("interactables")) {
                val iArray = root.getJSONArray("interactables")
                interactionSystem.interactables.clear()
                for (i in 0 until iArray.length()) {
                    val iObj = iArray.getJSONObject(i)
                    val targetPos = if (iObj.has("targetX")) {
                        Vec3(iObj.getDouble("targetX").toFloat(), iObj.getDouble("targetY").toFloat(), iObj.getDouble("targetZ").toFloat())
                    } else null

                    val item = WorldInteractable(
                        id = iObj.getString("id"),
                        name = iObj.getString("name"),
                        type = try { InteractableType.valueOf(iObj.getString("type")) } catch (e: Exception) { InteractableType.POWER_BEACON },
                        position = Vec3(iObj.getDouble("posX").toFloat(), iObj.getDouble("posY").toFloat(), iObj.getDouble("posZ").toFloat()),
                        targetTeleportPos = targetPos,
                        promptText = iObj.optString("promptText", "Interaksi")
                    )
                    interactionSystem.interactables.add(item)
                }
            }

            // 5. Lighting
            if (root.has("lighting")) {
                val l = root.getJSONObject("lighting")
                settings.enableFog = l.optBoolean("enableFog", settings.enableFog)
                settings.fogDensity = l.optDouble("fogDensity", settings.fogDensity.toDouble()).toFloat()
                if (l.has("sunAzimuthDeg")) {
                    val az = l.getDouble("sunAzimuthDeg").toFloat()
                    settings.sunAzimuth = az
                    renderer.lighting.sunAzimuthDeg = az
                }
                if (l.has("sunElevationDeg")) {
                    val el = l.getDouble("sunElevationDeg").toFloat()
                    settings.sunElevation = el
                    renderer.lighting.sunElevationDeg = el
                }
                if (l.has("ambientR")) {
                    renderer.lighting.ambientColor[0] = l.getDouble("ambientR").toFloat()
                    renderer.lighting.ambientColor[1] = l.getDouble("ambientG").toFloat()
                    renderer.lighting.ambientColor[2] = l.getDouble("ambientB").toFloat()
                }
            }

            Log.d("GameplaySceneManager", "Saved scene successfully loaded from JSON.")
            true
        } catch (e: Exception) {
            Log.e("GameplaySceneManager", "Failed to load saved scene", e)
            false
        }
    }

    fun exportSceneToPublicFolder(exportManager: SampleExportManager): String {
        return try {
            if (!sceneConfigFile.exists()) {
                return "Belum ada konfigurasi level yang disimpan untuk diekspor. Silakan simpan level terlebih dahulu!"
            }
            val jsonContent = sceneConfigFile.readText()
            val dir = File(context.filesDir, "ApexExports/scene")
            if (!dir.exists()) dir.mkdirs()
            val target = File(dir, "custom_gameplay_scene.json")
            target.writeText(jsonContent, Charsets.UTF_8)
            "✓ Berhasil mengekspor konfigurasi level ke penyimpanan internal:\n${target.absolutePath}"
        } catch (e: Exception) {
            "Kesalahan ekspor internal: ${e.message}"
        }
    }

    fun resetToDefaults(
        physicsEngine: PhysicsEngine,
        barrierManager: BarrierManager,
        npcManager: NpcManager,
        interactionSystem: InteractionSystem,
        renderer: Apex3DRenderer,
        settings: EngineSettings
    ) {
        physicsEngine.walkSpeed = 5.2f
        physicsEngine.sprintSpeed = 8.8f
        physicsEngine.jumpImpulse = 7.8f
        physicsEngine.gravity = -19.6f
        physicsEngine.maxClimbableSlope = 46f
        physicsEngine.stepHeight = 0.5f

        barrierManager.setupDefaultBarriers()
        npcManager.spawnSampleNpcs()

        interactionSystem.interactables.clear()
        interactionSystem.interactables.add(
            WorldInteractable("beacon_1", "Ancient Energy Beacon", InteractableType.POWER_BEACON, Vec3(0f, 0f, 0f), promptText = "Aktifkan Sinar Beacon")
        )
        interactionSystem.interactables.add(
            WorldInteractable("crate_1", "Supply Cache", InteractableType.SUPPLY_CRATE, Vec3(8f, 0f, 5f), promptText = "Buka Kotak Suplai")
        )
        interactionSystem.interactables.add(
            WorldInteractable("hoverboard_1", "Cyber Hoverboard", InteractableType.HOVERBOARD, Vec3(-6f, 0f, -8f), promptText = "Kendarai Hoverboard")
        )
        interactionSystem.interactables.add(
            WorldInteractable("portal_1", "Quantum Teleporter", InteractableType.QUANTUM_PORTAL, Vec3(18f, 0f, -15f), promptText = "Teleportasi ke Puncak Gunung")
        )

        settings.enableFog = true
        settings.fogDensity = 0.015f
        settings.sunAzimuth = 45f
        settings.sunElevation = 55f
        renderer.lighting.sunAzimuthDeg = 45f
        renderer.lighting.sunElevationDeg = 55f
        renderer.lighting.ambientColor = floatArrayOf(0.38f, 0.42f, 0.48f)

        if (sceneConfigFile.exists()) {
            sceneConfigFile.delete()
        }
    }
}

