package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine3d.actions.CharacterAction
import com.example.engine3d.importer.SampleExportManager
import com.example.engine3d.math.Mat4
import com.example.engine3d.math.Vec3
import com.example.engine3d.npc.NpcEntity
import com.example.engine3d.physics.BarrierManager
import com.example.engine3d.physics.BarrierType
import com.example.engine3d.physics.PhysicsEngine
import com.example.engine3d.physics.WorldBarrier
import com.example.engine3d.terrain.TerrainMesh
import com.example.engine3d.terrain.TerrainPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Apex3D Engine", appName)
    }

    @Test
    fun `test 3d vector math operations`() {
        val v1 = Vec3(1f, 2f, 3f)
        val v2 = Vec3(4f, 5f, 6f)
        val sum = v1.add(v2)
        assertEquals(5f, sum.x, 0.001f)
        assertEquals(7f, sum.y, 0.001f)
        assertEquals(9f, sum.z, 0.001f)

        val dot = v1.dot(v2)
        assertEquals(32f, dot, 0.001f)
    }

    @Test
    fun `test terrain contour height query`() {
        val terrain = TerrainMesh.generateTerrain(TerrainPreset.HIGHLAND_HILLS)
        assertNotNull(terrain)
        assertTrue(terrain.vertexCount > 0)

        val query = com.example.engine3d.terrain.TerrainHeightQuery()
        query.buildSpatialIndex(terrain)

        val surface = query.sampleSurface(0f, 0f)
        assertTrue(surface.slopeAngleDeg >= 0f)
    }

    @Test
    fun `test custom character action json serialization`() {
        val action = CharacterAction.DEFAULT_SLASH
        val json = action.toJson()
        assertTrue(json.contains("Energy Slash"))

        val parsed = CharacterAction.fromJson(json)
        assertNotNull(parsed)
        assertEquals(action.name, parsed?.name)
        assertEquals(action.animationType, parsed?.animationType)
    }

    @Test
    fun `test custom npc serialization`() {
        val npc = NpcEntity(
            id = "npc_test_1",
            name = "Test Guard",
            role = "Warrior",
            position = Vec3(5f, 0f, 10f),
            dialogues = mutableListOf("Ready for battle!")
        )
        val json = npc.toJson()
        val parsed = NpcEntity.fromJson(json)
        assertNotNull(parsed)
        assertEquals("Test Guard", parsed?.name)
        assertEquals("Warrior", parsed?.role)
        assertEquals(1, parsed?.dialogues?.size)
    }

    @Test
    fun `test barrier collision and road blocker`() {
        val barrierMgr = BarrierManager()
        assertTrue(barrierMgr.barriers.isNotEmpty())

        // Approaching barrier located at x=14.0 (halfX = 0.6) from left
        val charPos = Vec3(13.6f, 1.5f, 0f)
        val charVel = Vec3(5f, 0f, 0f)

        barrierMgr.resolveCharacterCollision(charPos, charVel, 0.45f, 0.016f)
        // Solid blocking wall should push character outside (x <= 13.0) and zero forward velocity
        assertTrue(charPos.x <= 13.0f)
        assertEquals(0f, charVel.x, 0.001f)
    }

    @Test
    fun `test sample export kit generation`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val barrierMgr = BarrierManager()
        val exporter = SampleExportManager(context, barrierMgr)

        val sampleFiles = exporter.generateAllGameFiles()
        assertTrue(sampleFiles.containsKey("manifest.json"))
        assertTrue(sampleFiles.containsKey("game_actions.json"))
        assertTrue(sampleFiles.containsKey("game_npcs.json"))
        assertTrue(sampleFiles.containsKey("game_world_barriers.json"))
        assertTrue(sampleFiles.containsKey("sample_terrain_contour.obj"))
        assertTrue(sampleFiles.containsKey("PANDUAN_OBB_DAN_FORMAT.txt"))

        val zipFile = exporter.exportPackage(isObb = false)
        assertTrue(zipFile.exists())
        assertTrue(zipFile.length() > 0)
    }

    @Test
    fun `test assassin creed shadows open world camera system`() {
        val camera = com.example.engine3d.core.Camera()
        assertEquals(com.example.engine3d.core.CameraPresetMode.DYNAMIC_EXPLORATION, camera.mode)
        assertTrue(camera.shoulderOffset > 0f)

        // Test dynamic sprinting pull back and crouch stealth framing
        val playerPos = Vec3(0f, 2f, 0f)
        camera.updateCinematic(playerPos, 0f, isSprinting = true, isCrouched = false, movementSpeed = 6f, dt = 0.05f)
        assertTrue(camera.targetDistance > camera.baseDistance)

        // Test cycle camera modes
        val nextMode = camera.cycleCameraMode()
        assertEquals(com.example.engine3d.core.CameraPresetMode.STEALTH_CROUCH, nextMode)
        assertTrue(camera.baseDistance < 4.5f)

        // Test screen shake trigger
        camera.triggerShake(intensity = 0.3f, durationSec = 0.2f)
    }

    @Test
    fun `test joystick camera-relative movement direction`() {
        val terrain = TerrainMesh()
        val physics = PhysicsEngine(terrain.heightQuery)

        // Joystick UP (moveInputY = 1.0f) with camera looking North (yaw = 0 deg)
        physics.update(dt = 0.05f, moveInputX = 0f, moveInputY = 1.0f, cameraYawDeg = 0f)
        // Should move forward in -Z direction
        assertTrue(physics.characterVel.z < 0f)
        assertEquals(0f, physics.characterVel.x, 0.05f)

        // Joystick RIGHT (moveInputX = 1.0f) with camera looking North (yaw = 0 deg)
        physics.update(dt = 0.05f, moveInputX = 1.0f, moveInputY = 0f, cameraYawDeg = 0f)
        // Should move right in +X direction
        assertTrue(physics.characterVel.x > 0f)
    }

    @Test
    fun `test plan file import parser for grid map`() {
        val terrain = TerrainMesh()
        val physics = PhysicsEngine(terrain.heightQuery)
        val npcMgr = com.example.engine3d.npc.NpcManager(terrain.heightQuery)
        val interactionSys = com.example.engine3d.actions.InteractionSystem()

        val planText = """
            # . . #
            . S B .
            . P . N
        """.trimIndent()

        val report = com.example.engine3d.importer.PlanFileImporter.importTextPlan(
            planText = planText,
            gridCellSize = 6.0f,
            terrainQuery = terrain.heightQuery,
            barrierManager = physics.barrierManager,
            npcManager = npcMgr,
            interactionSystem = interactionSys,
            playerPos = physics.characterPos
        )

        assertEquals(2, report.wallsCount)
        assertEquals(1, report.speedPadsCount)
        assertEquals(1, report.bouncePadsCount)
        assertEquals(1, report.npcsCount)
        assertTrue(report.playerSpawned)
    }

    @Test
    fun `test plan file import parser for json config`() {
        val terrain = TerrainMesh()
        val physics = PhysicsEngine(terrain.heightQuery)
        val npcMgr = com.example.engine3d.npc.NpcManager(terrain.heightQuery)
        val interactionSys = com.example.engine3d.actions.InteractionSystem()

        val jsonText = """
            {
              "barriers": [
                {
                  "id": "test_wall_json",
                  "name": "JSON Wall",
                  "type": "WALL_BARRIER",
                  "x": 10.0,
                  "z": -12.0,
                  "sizeX": 4.0,
                  "sizeY": 3.0,
                  "sizeZ": 4.0
                }
              ],
              "npcs": [
                {
                  "id": "test_npc_json",
                  "name": "Test NPC",
                  "role": "Trader",
                  "x": -5.0,
                  "z": 8.0,
                  "behavior": "IDLE"
                }
              ]
            }
        """.trimIndent()

        val report = com.example.engine3d.importer.PlanFileImporter.importJsonPlan(
            jsonText = jsonText,
            terrainQuery = terrain.heightQuery,
            barrierManager = physics.barrierManager,
            npcManager = npcMgr,
            interactionSystem = interactionSys,
            playerPos = physics.characterPos
        )

        assertEquals(1, report.wallsCount)
        assertEquals(1, report.npcsCount)
    }
}
