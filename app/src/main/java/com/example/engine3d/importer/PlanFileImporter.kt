package com.example.engine3d.importer

import com.example.engine3d.math.Vec3
import com.example.engine3d.npc.NpcBehavior
import com.example.engine3d.npc.NpcEntity
import com.example.engine3d.npc.NpcManager
import com.example.engine3d.physics.BarrierManager
import com.example.engine3d.physics.BarrierType
import com.example.engine3d.physics.WorldBarrier
import com.example.engine3d.terrain.TerrainHeightQuery
import com.example.engine3d.actions.InteractionSystem
import com.example.engine3d.actions.WorldInteractable
import com.example.engine3d.actions.InteractableType
import org.json.JSONArray
import org.json.JSONObject

data class PlanImportReport(
    val wallsCount: Int,
    val speedPadsCount: Int,
    val bouncePadsCount: Int,
    val killZonesCount: Int,
    val npcsCount: Int,
    val interactablesCount: Int,
    val playerSpawned: Boolean,
    val details: List<String>,
    val errors: List<String>
)

object PlanFileImporter {

    /**
     * Parses a text grid plan of characters and populates the managers.
     * Scale determines the grid cell width and height in meters.
     */
    fun importTextPlan(
        planText: String,
        gridCellSize: Float,
        terrainQuery: TerrainHeightQuery,
        barrierManager: BarrierManager,
        npcManager: NpcManager,
        interactionSystem: InteractionSystem,
        playerPos: Vec3? = null
    ): PlanImportReport {
        val lines = planText.split("\n")
            .map { it.trimEnd() }
            .filter { it.isNotEmpty() }
        
        if (lines.isEmpty()) {
            return PlanImportReport(0, 0, 0, 0, 0, 0, false, emptyList(), listOf("Plan file is empty"))
        }

        val details = mutableListOf<String>()
        val errors = mutableListOf<String>()

        var walls = 0
        var speedPads = 0
        var bouncePads = 0
        var killZones = 0
        var npcs = 0
        var interactables = 0
        var playerSpawned = false

        // Clear existing custom/procedural items to prevent double-spawning
        // We keep default boundaries starting with "world_limit" but clear generated ones
        barrierManager.barriers.removeAll { !it.id.startsWith("world_limit") }
        npcManager.npcs.removeAll { !it.id.startsWith("npc_guide") && !it.id.startsWith("npc_guard") }
        interactionSystem.interactables.removeAll { !it.id.startsWith("beacon_") && !it.id.startsWith("crate_") && !it.id.startsWith("hoverboard_") && !it.id.startsWith("portal_") }

        val rows = lines.size
        val cols = lines.maxOfOrNull { it.length } ?: 0

        // Center the imported plan around (0, 0) in world coordinates
        val startX = -(cols * gridCellSize) / 2f
        val startZ = -(rows * gridCellSize) / 2f

        for (r in 0 until rows) {
            val line = lines[r]
            for (c in 0 until line.length) {
                val char = line[c]
                if (char == '.' || char == ' ') continue

                val worldX = startX + c * gridCellSize + gridCellSize / 2f
                val worldZ = startZ + r * gridCellSize + gridCellSize / 2f
                val ground = terrainQuery.sampleSurface(worldX, worldZ)
                val worldY = ground.height

                val coordStr = "Grid($r,$c) -> 3D(${String.format("%.1f", worldX)}, ${String.format("%.1f", worldY)}, ${String.format("%.1f", worldZ)})"

                when (char) {
                    '#' -> {
                        // Solid Wall Barrier
                        val wallId = "plan_wall_${r}_${c}"
                        val barrier = WorldBarrier(
                            id = wallId,
                            name = "Wall Block ($r, $c)",
                            type = BarrierType.WALL_BARRIER,
                            position = Vec3(worldX, worldY + 2f, worldZ), // offset Y up so it sits nicely
                            size = Vec3(gridCellSize * 0.95f, 4f, gridCellSize * 0.95f),
                            isPassable = false,
                            warningMessage = "🚫 Collision: Wall!"
                        )
                        barrierManager.addBarrier(barrier)
                        walls++
                    }
                    'S' -> {
                        // Speed Boost Pad
                        val padId = "plan_speed_${r}_${c}"
                        val barrier = WorldBarrier(
                            id = padId,
                            name = "Speed Boost Pad ($r, $c)",
                            type = BarrierType.SPEED_BOOST_PAD,
                            position = Vec3(worldX, worldY + 0.1f, worldZ),
                            size = Vec3(gridCellSize * 0.9f, 0.4f, gridCellSize * 0.9f),
                            isPassable = true,
                            speedMultiplier = 2.5f,
                            warningMessage = "⚡ Turbo Active: Speed x2.5!"
                        )
                        barrierManager.addBarrier(barrier)
                        speedPads++
                    }
                    'B' -> {
                        // Bounce Jump Pad
                        val padId = "plan_bounce_${r}_${c}"
                        val barrier = WorldBarrier(
                            id = padId,
                            name = "Bounce Pad ($r, $c)",
                            type = BarrierType.BOUNCE_PAD,
                            position = Vec3(worldX, worldY + 0.2f, worldZ),
                            size = Vec3(gridCellSize * 0.8f, 0.4f, gridCellSize * 0.8f),
                            isPassable = true,
                            bounceImpulse = 14.0f,
                            warningMessage = "🚀 Vertical Launch!"
                        )
                        barrierManager.addBarrier(barrier)
                        bouncePads++
                    }
                    'K' -> {
                        // Kill Zone / Lava Pit
                        val zoneId = "plan_kill_${r}_${c}"
                        val barrier = WorldBarrier(
                            id = zoneId,
                            name = "Lava Zone ($r, $c)",
                            type = BarrierType.KILL_ZONE,
                            position = Vec3(worldX, worldY + 0.1f, worldZ),
                            size = Vec3(gridCellSize * 0.95f, 0.3f, gridCellSize * 0.95f),
                            isPassable = true,
                            warningMessage = "💀 Respawned from Hazard!"
                        )
                        barrierManager.addBarrier(barrier)
                        killZones++
                    }
                    'P' -> {
                        // Player Spawn Spot
                        if (playerPos != null) {
                            playerPos.set(worldX, worldY + 1.0f, worldZ)
                            playerSpawned = true
                            details.add("🧍 Player Spawn relocated to $coordStr")
                        }
                    }
                    'N' -> {
                        // Standard Villager NPC
                        val npcId = "plan_npc_${r}_${c}"
                        val npc = NpcEntity(
                            id = npcId,
                            name = "Villager ($r,$c)",
                            role = "Villager",
                            position = Vec3(worldX, worldY, worldZ),
                            yawDeg = 180f,
                            behavior = NpcBehavior.LOOK_AT_PLAYER,
                            dialogues = mutableListOf(
                                "Greetings! I was spawned from the text plan file.",
                                "Isn't it amazing how a simple letter code built this whole space?",
                                "Have fun exploring the generated level!"
                            ),
                            tintColor = floatArrayOf(0.1f, 0.6f, 0.95f, 1.0f)
                        )
                        npcManager.addNpc(npc)
                        npcs++
                    }
                    'G' -> {
                        // Guard NPC (Patrolling)
                        val npcId = "plan_npc_guard_${r}_${c}"
                        val npc = NpcEntity(
                            id = npcId,
                            name = "Patrol Guard ($r,$c)",
                            role = "Guard",
                            position = Vec3(worldX, worldY, worldZ),
                            yawDeg = 90f,
                            behavior = NpcBehavior.PATROL_ROAM,
                            dialogues = mutableListOf(
                                "Stay alert, citizen! I am securing this sector.",
                                "I patrol around my center grid position.",
                                "Move along now!"
                            ),
                            tintColor = floatArrayOf(0.9f, 0.2f, 0.2f, 1.0f),
                            patrolRadius = gridCellSize * 2f
                        )
                        npcManager.addNpc(npc)
                        npcs++
                    }
                    'D' -> {
                        // Supply Crate
                        val itemId = "plan_crate_${r}_${c}"
                        val crate = WorldInteractable(
                            id = itemId,
                            name = "Loot Crate ($r, $c)",
                            type = InteractableType.SUPPLY_CRATE,
                            position = Vec3(worldX, worldY, worldZ),
                            promptText = "Open Plan Crate"
                        )
                        interactionSystem.interactables.add(crate)
                        interactables++
                    }
                    else -> {
                        errors.add("Unknown character '$char' at row $r, col $c")
                    }
                }
            }
        }

        details.add("✓ Generated: $walls walls, $speedPads speed-boosts, $bouncePads bounce-pads, $killZones kill-zones, $npcs NPCs, $interactables crates from plan.")
        return PlanImportReport(
            wallsCount = walls,
            speedPadsCount = speedPads,
            bouncePadsCount = bouncePads,
            killZonesCount = killZones,
            npcsCount = npcs,
            interactablesCount = interactables,
            playerSpawned = playerSpawned,
            details = details,
            errors = errors
        )
    }

    /**
     * Parses a JSON-styled level plan configuration.
     */
    fun importJsonPlan(
        jsonText: String,
        terrainQuery: TerrainHeightQuery,
        barrierManager: BarrierManager,
        npcManager: NpcManager,
        interactionSystem: InteractionSystem,
        playerPos: Vec3? = null
    ): PlanImportReport {
        val details = mutableListOf<String>()
        val errors = mutableListOf<String>()

        var walls = 0
        var speedPads = 0
        var bouncePads = 0
        var killZones = 0
        var npcs = 0
        var interactables = 0
        var playerSpawned = false

        try {
            val root = JSONObject(jsonText)
            
            // Clear existing custom elements
            barrierManager.barriers.removeAll { !it.id.startsWith("world_limit") }
            npcManager.npcs.removeAll { !it.id.startsWith("npc_guide") && !it.id.startsWith("npc_guard") }
            interactionSystem.interactables.removeAll { !it.id.startsWith("beacon_") && !it.id.startsWith("crate_") && !it.id.startsWith("hoverboard_") && !it.id.startsWith("portal_") }

            // 1. Process Barriers
            val bArr = root.optJSONArray("barriers")
            if (bArr != null) {
                for (i in 0 until bArr.length()) {
                    val obj = bArr.getJSONObject(i)
                    val id = obj.optString("id", "json_b_${System.currentTimeMillis()}_$i")
                    val name = obj.optString("name", "Barrier $i")
                    val typeStr = obj.optString("type", "WALL_BARRIER")
                    val x = obj.optDouble("x", 0.0).toFloat()
                    val z = obj.optDouble("z", 0.0).toFloat()
                    val ground = terrainQuery.sampleSurface(x, z)
                    val y = obj.optDouble("y", ground.height.toDouble() + 1.0).toFloat()
                    
                    val sizeX = obj.optDouble("sizeX", 2.0).toFloat()
                    val sizeY = obj.optDouble("sizeY", 2.0).toFloat()
                    val sizeZ = obj.optDouble("sizeZ", 2.0).toFloat()
                    val isPassable = obj.optBoolean("isPassable", false)
                    val speedMultiplier = obj.optDouble("speedMultiplier", 1.0).toFloat()
                    val bounceImpulse = obj.optDouble("bounceImpulse", 0.0).toFloat()
                    val warn = obj.optString("warningMessage", "")

                    val bType = try { BarrierType.valueOf(typeStr) } catch(e: Exception) { BarrierType.WALL_BARRIER }

                    val b = WorldBarrier(
                        id = id,
                        name = name,
                        type = bType,
                        position = Vec3(x, y, z),
                        size = Vec3(sizeX, sizeY, sizeZ),
                        isPassable = isPassable,
                        speedMultiplier = speedMultiplier,
                        bounceImpulse = bounceImpulse,
                        warningMessage = warn
                    )
                    barrierManager.addBarrier(b)
                    
                    when (bType) {
                        BarrierType.WALL_BARRIER -> walls++
                        BarrierType.SPEED_BOOST_PAD -> speedPads++
                        BarrierType.BOUNCE_PAD -> bouncePads++
                        BarrierType.KILL_ZONE -> killZones++
                        else -> {}
                    }
                }
            }

            // 2. Process NPCs
            val nArr = root.optJSONArray("npcs")
            if (nArr != null) {
                for (i in 0 until nArr.length()) {
                    val obj = nArr.getJSONObject(i)
                    val id = obj.optString("id", "json_npc_${System.currentTimeMillis()}_$i")
                    val name = obj.optString("name", "NPC $i")
                    val role = obj.optString("role", "Citizen")
                    val x = obj.optDouble("x", 0.0).toFloat()
                    val z = obj.optDouble("z", 0.0).toFloat()
                    val ground = terrainQuery.sampleSurface(x, z)
                    val y = obj.optDouble("y", ground.height.toDouble()).toFloat()
                    val yaw = obj.optDouble("yawDeg", 0.0).toFloat()
                    val behaviorStr = obj.optString("behavior", "LOOK_AT_PLAYER")
                    val behavior = try { NpcBehavior.valueOf(behaviorStr) } catch(e: Exception) { NpcBehavior.LOOK_AT_PLAYER }
                    
                    val dialogues = mutableListOf<String>()
                    val dArr = obj.optJSONArray("dialogues")
                    if (dArr != null) {
                        for (k in 0 until dArr.length()) {
                            dialogues.add(dArr.getString(k))
                        }
                    } else {
                        dialogues.add("Hello from the JSON plan.")
                    }

                    val npc = NpcEntity(
                        id = id,
                        name = name,
                        role = role,
                        position = Vec3(x, y, z),
                        yawDeg = yaw,
                        behavior = behavior,
                        dialogues = dialogues,
                        tintColor = floatArrayOf(0.4f, 0.9f, 0.3f, 1.0f)
                    )
                    npcManager.addNpc(npc)
                    npcs++
                }
            }

            // 3. Process Player Spawn
            val playerSpawn = root.optJSONObject("playerSpawn")
            if (playerSpawn != null && playerPos != null) {
                val px = playerSpawn.optDouble("x", 0.0).toFloat()
                val pz = playerSpawn.optDouble("z", 0.0).toFloat()
                val ground = terrainQuery.sampleSurface(px, pz)
                val py = playerSpawn.optDouble("y", ground.height.toDouble() + 1.0).toFloat()
                playerPos.set(px, py, pz)
                playerSpawned = true
                details.add("Spawned Player at coordinate ($px, $py, $pz) from JSON.")
            }

            // 4. Process Interactables
            val iArr = root.optJSONArray("interactables")
            if (iArr != null) {
                for (i in 0 until iArr.length()) {
                    val obj = iArr.getJSONObject(i)
                    val id = obj.optString("id", "json_i_${System.currentTimeMillis()}_$i")
                    val name = obj.optString("name", "Object $i")
                    val typeStr = obj.optString("type", "SUPPLY_CRATE")
                    val x = obj.optDouble("x", 0.0).toFloat()
                    val z = obj.optDouble("z", 0.0).toFloat()
                    val ground = terrainQuery.sampleSurface(x, z)
                    val y = obj.optDouble("y", ground.height.toDouble()).toFloat()
                    val prompt = obj.optString("promptText", "Interact")

                    val iType = try { InteractableType.valueOf(typeStr) } catch(e: Exception) { InteractableType.SUPPLY_CRATE }

                    val item = WorldInteractable(
                        id = id,
                        name = name,
                        type = iType,
                        position = Vec3(x, y, z),
                        promptText = prompt
                    )
                    interactionSystem.interactables.add(item)
                    interactables++
                }
            }

            details.add("✓ Successfully parsed plan JSON: Created $walls walls, $speedPads speed boosters, $bouncePads bounce pads, $killZones hazards, $npcs NPCs, and $interactables interactables.")
        } catch (e: Exception) {
            e.printStackTrace()
            errors.add("JSON parse error: ${e.localizedMessage}")
        }

        return PlanImportReport(
            wallsCount = walls,
            speedPadsCount = speedPads,
            bouncePadsCount = bouncePads,
            killZonesCount = killZones,
            npcsCount = npcs,
            interactablesCount = interactables,
            playerSpawned = playerSpawned,
            details = details,
            errors = errors
        )
    }
}
