package com.example.engine3d.npc

import com.example.engine3d.math.Vec3
import com.example.engine3d.terrain.TerrainHeightQuery
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class NpcManager(
    private val terrainQuery: TerrainHeightQuery
) {
    val npcs = mutableListOf<NpcEntity>()
    var nearbyNpc: NpcEntity? = null
    var activeTalkingNpc: NpcEntity? = null
    var activeDialogueIndex: Int = 0

    init {
        // Spawn 2 default NPCs on the world
        spawnSampleNpcs()
    }

    fun spawnSampleNpcs() {
        npcs.clear()
        val npc1 = NpcEntity(
            id = "npc_guide_1",
            name = "Aria (Pemandu Engine)",
            role = "Instruktur",
            position = Vec3(4f, 0f, 3f),
            dialogues = mutableListOf(
                "Halo! Selamat datang di dunia 3D Apex Engine.",
                "Kamu bisa mengimpor seluruh folder file .glb dari memori HP-mu!",
                "Kontur lereng bukit ini dihitung langsung dari poligon mesh 3D secara real-time.",
                "Coba jelajahi puncak lereng atau gunakan hoverboard!"
            ),
            tintColor = floatArrayOf(0.1f, 0.9f, 0.7f, 1f),
            behavior = NpcBehavior.LOOK_AT_PLAYER
        )

        val npc2 = NpcEntity(
            id = "npc_guard_2",
            name = "Kael (Penjaga Lembah)",
            role = "Ksatria",
            position = Vec3(-8f, 0f, 6f),
            dialogues = mutableListOf(
                "Waspadalah jika menuruni tebing curam di atas 45 derajat!",
                "Fisika gravitasi akan membuatmu meluncur jatuh jika terlalu miring.",
                "Tekan tombol Serang atau Aksi Khusus untuk jurus plasma!"
            ),
            tintColor = floatArrayOf(0.95f, 0.4f, 0.15f, 1f),
            behavior = NpcBehavior.PATROL_ROAM
        )

        // Snap their Y positions to terrain
        val s1 = terrainQuery.sampleSurface(npc1.position.x, npc1.position.z)
        npc1.position.y = s1.height
        npc1.patrolCenter = npc1.position.copy()

        val s2 = terrainQuery.sampleSurface(npc2.position.x, npc2.position.z)
        npc2.position.y = s2.height
        npc2.patrolCenter = npc2.position.copy()

        npcs.add(npc1)
        npcs.add(npc2)
    }

    fun addNpc(npc: NpcEntity) {
        val s = terrainQuery.sampleSurface(npc.position.x, npc.position.z)
        npc.position.y = s.height
        npc.patrolCenter = npc.position.copy()
        npcs.removeAll { it.id == npc.id }
        npcs.add(npc)
    }

    fun removeNpc(npc: NpcEntity) {
        npcs.remove(npc)
        if (nearbyNpc == npc) nearbyNpc = null
        if (activeTalkingNpc == npc) activeTalkingNpc = null
    }

    fun update(playerPos: Vec3, dt: Float) {
        var closest: NpcEntity? = null
        var closestDist = Float.MAX_VALUE

        for (npc in npcs) {
            val dx = playerPos.x - npc.position.x
            val dz = playerPos.z - npc.position.z
            val dist = sqrt(dx * dx + dz * dz)

            if (dist < 3.2f && dist < closestDist) {
                closest = npc
                closestDist = dist
            }

            // Update NPC behavior
            when (npc.behavior) {
                NpcBehavior.LOOK_AT_PLAYER -> {
                    if (dist < 15f) {
                        // Turn head/body to face player
                        val targetYaw = Math.toDegrees(atan2(dx.toDouble(), -dz.toDouble())).toFloat()
                        npc.yawDeg = targetYaw
                    }
                }
                NpcBehavior.PATROL_ROAM -> {
                    if (dist < 3.2f) {
                        // Stop and face player when player is talking/close
                        val targetYaw = Math.toDegrees(atan2(dx.toDouble(), -dz.toDouble())).toFloat()
                        npc.yawDeg = targetYaw
                    } else {
                        // Roam around patrol center
                        npc.patrolTimer -= dt
                        if (npc.patrolTimer <= 0f) {
                            npc.patrolTimer = 4.0f + (Math.random().toFloat() * 3.0f)
                            val angle = (Math.random() * Math.PI * 2.0).toFloat()
                            val rad = Math.random().toFloat() * npc.patrolRadius
                            npc.targetWaypoint.set(
                                npc.patrolCenter.x + cos(angle) * rad,
                                0f,
                                npc.patrolCenter.z + sin(angle) * rad
                            )
                        }

                        // Move towards waypoint
                        val wx = npc.targetWaypoint.x - npc.position.x
                        val wz = npc.targetWaypoint.z - npc.position.z
                        val wDist = sqrt(wx * wx + wz * wz)
                        if (wDist > 0.4f) {
                            val dirX = wx / wDist
                            val dirZ = wz / wDist
                            npc.position.x += dirX * npc.moveSpeed * dt
                            npc.position.z += dirZ * npc.moveSpeed * dt
                            npc.yawDeg = Math.toDegrees(atan2(dirX.toDouble(), -dirZ.toDouble())).toFloat()

                            // Snap Y smoothly to terrain contour
                            val ground = terrainQuery.sampleSurface(npc.position.x, npc.position.z)
                            npc.position.y = ground.height
                        }
                    }
                }
                NpcBehavior.IDLE -> {}
            }
        }

        nearbyNpc = closest
    }

    fun startDialogue(npc: NpcEntity) {
        activeTalkingNpc = npc
        activeDialogueIndex = 0
    }

    fun nextDialogue(): Boolean {
        val npc = activeTalkingNpc ?: return false
        if (activeDialogueIndex < npc.dialogues.size - 1) {
            activeDialogueIndex++
            return true
        } else {
            activeTalkingNpc = null
            activeDialogueIndex = 0
            return false
        }
    }

    fun closeDialogue() {
        activeTalkingNpc = null
        activeDialogueIndex = 0
    }
}
