package com.example.engine3d.renderer

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.SystemClock
import com.example.engine3d.actions.ActionManager
import com.example.engine3d.actions.InteractableType
import com.example.engine3d.actions.InteractionSystem
import com.example.engine3d.core.Camera
import com.example.engine3d.core.LightingEnvironment
import com.example.engine3d.core.Mesh
import com.example.engine3d.core.Shader
import com.example.engine3d.importer.CustomModelManager
import com.example.engine3d.importer.ProceduralMeshGenerator
import com.example.engine3d.math.Mat4
import com.example.engine3d.math.Vec3
import com.example.engine3d.npc.NpcEntity
import com.example.engine3d.npc.NpcManager
import com.example.engine3d.physics.PhysicsEngine
import com.example.engine3d.physics.ShapeType
import com.example.engine3d.terrain.TerrainMesh
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.sin

data class EnginePerformanceStats(
    val fps: Int,
    val frameTimeMs: Float,
    val trianglesDrawn: Int,
    val drawCalls: Int,
    val slopeAngle: Float,
    val posX: Float = 0f,
    val posY: Float = 0f,
    val posZ: Float = 0f,
    val headingDeg: Float = 0f
)

class Apex3DRenderer(
    val context: Context,
    val settings: EngineSettings,
    val terrainMesh: TerrainMesh,
    val physicsEngine: PhysicsEngine,
    val actionManager: ActionManager,
    val interactionSystem: InteractionSystem,
    val customModelManager: CustomModelManager,
    val npcManager: NpcManager
) : GLSurfaceView.Renderer {

    val camera = Camera().apply {
        terrainQuery = terrainMesh.heightQuery
        baseDistance = settings.cameraDistance.coerceIn(1.5f, 20f)
        targetDistance = baseDistance
        currentDistance = baseDistance
    }
    val lighting = LightingEnvironment()

    init {
        camera.onZoomChanged = { dist ->
            settings.cameraDistance = dist
        }
        actionManager.onActionTriggered = { act ->
            when (act.animationType) {
                "SLAM" -> camera.triggerShake(intensity = 0.35f, durationSec = 0.28f)
                "SLASH" -> camera.triggerShake(intensity = 0.16f, durationSec = 0.18f)
                "DASH" -> camera.triggerShake(intensity = 0.12f, durationSec = 0.15f)
                else -> camera.triggerShake(intensity = 0.15f, durationSec = 0.18f)
            }
        }
    }

    // Shaders
    private var mainShader: Shader? = null

    // Procedural primitive meshes for character & scene props
    private var boxMesh: Mesh? = null
    private var cylinderMesh: Mesh? = null
    private var bladeMesh: Mesh? = null
    private var sphereMesh: Mesh? = null

    // Controller inputs forwarded to physics
    var inputStickX: Float = 0f
    var inputStickY: Float = 0f

    // Performance tracking
    private var lastFrameTimeNs: Long = 0L
    private var frameCount: Int = 0
    private var fpsTimerNs: Long = 0L
    var currentFps: Int = 60
    val fps: Int get() = currentFps
    private var currentFrameTimeMs: Float = 16.6f
    private var lastTriangles: Int = 0
    private var lastDrawCalls: Int = 0

    var onPerformanceUpdate: ((EnginePerformanceStats) -> Unit)? = null

    // Walk & Character animation phase
    private var walkAnimPhase: Float = 0f
    private val animationPlayer = com.example.engine3d.animation.AnimationPlayer()

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.55f, 0.70f, 0.85f, 1.0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glCullFace(GLES20.GL_BACK)

        mainShader = Shader()

        boxMesh = ProceduralMeshGenerator.createBox("Box", 1f, 1f, 1f, 0.8f, 0.8f, 0.8f)
        cylinderMesh = ProceduralMeshGenerator.createCylinder("Cyl", 0.5f, 1f, 12, 0.7f, 0.7f, 0.75f)
        bladeMesh = ProceduralMeshGenerator.createEnergyBlade()
        sphereMesh = ProceduralMeshGenerator.createCylinder("SphereApprox", 0.6f, 1.2f, 10, 0.3f, 0.9f, 0.4f)

        lastFrameTimeNs = System.nanoTime()
        fpsTimerNs = lastFrameTimeNs
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        camera.setAspectRatio(width, height)

        if (width < height) {
            camera.fovDeg = 72f
        } else {
            camera.fovDeg = 62f
        }
    }

    override fun onDrawFrame(gl: GL10?) {
        val currentNs = System.nanoTime()
        val dtNs = currentNs - lastFrameTimeNs
        lastFrameTimeNs = currentNs
        val dt = (dtNs / 1_000_000_000.0f).coerceIn(0.001f, 0.1f)

        frameCount++
        if (currentNs - fpsTimerNs >= 500_000_000L) {
            val elapsedSec = (currentNs - fpsTimerNs) / 1_000_000_000.0f
            currentFps = (frameCount / elapsedSec).toInt()
            currentFrameTimeMs = (dtNs / 1_000_000.0f)
            frameCount = 0
            fpsTimerNs = currentNs

            onPerformanceUpdate?.invoke(
                EnginePerformanceStats(
                    fps = currentFps,
                    frameTimeMs = currentFrameTimeMs,
                    trianglesDrawn = lastTriangles,
                    drawCalls = lastDrawCalls,
                    slopeAngle = physicsEngine.currentSlopeAngle,
                    posX = physicsEngine.characterPos.x,
                    posY = physicsEngine.characterPos.y,
                    posZ = physicsEngine.characterPos.z,
                    headingDeg = physicsEngine.characterYawDeg
                )
            )
        }

        if (settings.targetFps > 0) {
            val targetFrameNs = 1_000_000_000L / settings.targetFps
            val frameWorkNs = System.nanoTime() - currentNs
            val sleepNs = targetFrameNs - frameWorkNs
            if (sleepNs > 2_000_000L) {
                SystemClock.sleep((sleepNs / 1_000_000L))
            }
        }

        physicsEngine.update(dt, inputStickX, inputStickY, camera.yawDeg)
        actionManager.update(dt)
        interactionSystem.updateProximity(physicsEngine.characterPos, dt)
        npcManager.update(physicsEngine.characterPos, dt)

        camera.farPlane = settings.renderDistance
        val charSpeed = kotlin.math.sqrt(physicsEngine.characterVel.x * physicsEngine.characterVel.x + physicsEngine.characterVel.z * physicsEngine.characterVel.z)
        camera.updateCinematic(
            targetPos = physicsEngine.characterPos,
            characterYawDeg = physicsEngine.characterYawDeg,
            isSprinting = physicsEngine.isSprinting,
            isCrouched = physicsEngine.isCrouched,
            movementSpeed = charSpeed,
            dt = dt
        )

        val horizSpeed = kotlin.math.sqrt(physicsEngine.characterVel.x * physicsEngine.characterVel.x + physicsEngine.characterVel.z * physicsEngine.characterVel.z)
        if (physicsEngine.isGrounded && horizSpeed > 0.5f) {
            walkAnimPhase += dt * horizSpeed * 2.5f
        }

        val fogC = lighting.fogColor
        GLES20.glClearColor(fogC[0], fogC[1], fogC[2], 1.0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        val shader = mainShader ?: return
        shader.use()

        lighting.sunAzimuthDeg = settings.sunAzimuth
        lighting.sunElevationDeg = settings.sunElevation
        val sunDir = lighting.getSunDirection()
        GLES20.glUniform3f(shader.uLightDirLocation, sunDir.x, sunDir.y, sunDir.z)
        GLES20.glUniform3f(shader.uLightColorLocation, lighting.sunColor[0], lighting.sunColor[1], lighting.sunColor[2])
        GLES20.glUniform3f(shader.uAmbientColorLocation, lighting.ambientColor[0], lighting.ambientColor[1], lighting.ambientColor[2])
        GLES20.glUniform3f(shader.uViewPosLocation, camera.position.x, camera.position.y, camera.position.z)

        GLES20.glUniform1i(shader.uFogEnabledLocation, if (settings.enableFog) 1 else 0)
        GLES20.glUniform1f(shader.uFogDensityLocation, settings.fogDensity)
        GLES20.glUniform3f(shader.uFogColorLocation, fogC[0], fogC[1], fogC[2])
        GLES20.glUniform1i(shader.uUseLightingLocation, if (settings.lightingQuality > 0) 1 else 0)
        GLES20.glUniform1i(shader.uShadingQualityLocation, settings.lightingQuality)

        if (lighting.pointLights.isNotEmpty()) {
            val pl = lighting.pointLights[0]
            GLES20.glUniform3f(shader.uPointLightPosLocation, pl.position.x, pl.position.y, pl.position.z)
            GLES20.glUniform3f(shader.uPointLightColorLocation, pl.color[0] * pl.intensity, pl.color[1] * pl.intensity, pl.color[2] * pl.intensity)
            GLES20.glUniform1f(shader.uPointLightRadiusLocation, pl.radius)
        }

        var triCount = 0
        var drawCallCount = 0

        // 1. Render Terrain
        val activeTerrain = customModelManager.activeCustomTerrainMesh ?: terrainMesh.mesh
        val terrainModelMat = Mat4()
        val terrainMvp = Mat4().set(camera.viewProjMatrix).multiply(terrainModelMat)

        GLES20.glUniformMatrix4fv(shader.uMVPMatrixLocation, 1, false, terrainMvp.data, 0)
        GLES20.glUniformMatrix4fv(shader.uModelMatrixLocation, 1, false, terrainModelMat.data, 0)
        GLES20.glUniform4f(shader.uBaseColorLocation, 1f, 1f, 1f, 1f)
        GLES20.glUniform1f(shader.uSpecularStrengthLocation, 0.15f)
        GLES20.glUniform1f(shader.uShininessLocation, 16f)
        GLES20.glUniform1i(shader.uUseVertexColorLocation, if (activeTerrain.colors != null) 1 else 0)

        activeTerrain.render(shader.aPositionLocation, shader.aNormalLocation, shader.aColorLocation, settings.enableWireframe)
        GLES20.glUniform1i(shader.uUseVertexColorLocation, 0)
        triCount += activeTerrain.triangleCount
        drawCallCount++

        // 2. Render World Interactables
        val box = boxMesh ?: return
        for (item in interactionSystem.interactables) {
            val customMesh = if (!item.meshFileName.isNullOrBlank()) {
                customModelManager.importedModels.firstOrNull { it.fileName.equals(item.meshFileName, ignoreCase = true) }?.mesh
            } else null

            if (customMesh != null) {
                val boundMat = Mat4()
                    .translate(
                        item.position.x + item.visualOffset.x,
                        item.position.y + item.visualOffset.y,
                        item.position.z + item.visualOffset.z
                    )
                    .rotate(item.rotationY, 0f, 1f, 0f)
                    .scale(item.visualScale, item.visualScale, item.visualScale)
                val mvp = Mat4().set(camera.viewProjMatrix).multiply(boundMat)
                GLES20.glUniformMatrix4fv(shader.uMVPMatrixLocation, 1, false, mvp.data, 0)
                GLES20.glUniformMatrix4fv(shader.uModelMatrixLocation, 1, false, boundMat.data, 0)
                GLES20.glUniform4f(shader.uBaseColorLocation, 1f, 1f, 1f, 1f)
                GLES20.glUniform1i(shader.uUseVertexColorLocation, if (customMesh.colors != null) 1 else 0)
                customMesh.render(shader.aPositionLocation, shader.aNormalLocation, shader.aColorLocation, settings.enableWireframe)
                GLES20.glUniform1i(shader.uUseVertexColorLocation, 0)
                triCount += customMesh.triangleCount
                drawCallCount++
                continue
            }

            val itemMat = Mat4().translate(item.position.x, item.position.y + 0.5f, item.position.z)

            when (item.type) {
                InteractableType.POWER_BEACON -> {
                    itemMat.scale(0.8f, 3.5f, 0.8f)
                    val mvp = Mat4().set(camera.viewProjMatrix).multiply(itemMat)
                    GLES20.glUniformMatrix4fv(shader.uMVPMatrixLocation, 1, false, mvp.data, 0)
                    GLES20.glUniformMatrix4fv(shader.uModelMatrixLocation, 1, false, itemMat.data, 0)
                    GLES20.glUniform4f(shader.uBaseColorLocation, 0.2f, 0.25f, 0.35f, 1f)
                    box.render(shader.aPositionLocation, shader.aNormalLocation, shader.aColorLocation, settings.enableWireframe)
                    triCount += box.triangleCount
                    drawCallCount++

                    val crystalMat = Mat4().translate(item.position.x, item.position.y + 4.2f + sin(walkAnimPhase.toDouble() * 2).toFloat() * 0.2f, item.position.z)
                        .rotate(walkAnimPhase * 45f, 0f, 1f, 0f)
                        .scale(0.6f, 0.9f, 0.6f)
                    val crystalMvp = Mat4().set(camera.viewProjMatrix).multiply(crystalMat)
                    GLES20.glUniformMatrix4fv(shader.uMVPMatrixLocation, 1, false, crystalMvp.data, 0)
                    GLES20.glUniformMatrix4fv(shader.uModelMatrixLocation, 1, false, crystalMat.data, 0)
                    val glowColor = if (item.isActivated) floatArrayOf(0.1f, 1.0f, 0.9f, 1f) else floatArrayOf(0.9f, 0.4f, 0.1f, 1f)
                    GLES20.glUniform4f(shader.uBaseColorLocation, glowColor[0], glowColor[1], glowColor[2], 1f)
                    box.render(shader.aPositionLocation, shader.aNormalLocation, shader.aColorLocation, settings.enableWireframe)
                    triCount += box.triangleCount
                    drawCallCount++
                }
                InteractableType.SUPPLY_CRATE -> {
                    itemMat.scale(1.2f, 1.2f, 1.2f)
                    val mvp = Mat4().set(camera.viewProjMatrix).multiply(itemMat)
                    GLES20.glUniformMatrix4fv(shader.uMVPMatrixLocation, 1, false, mvp.data, 0)
                    GLES20.glUniformMatrix4fv(shader.uModelMatrixLocation, 1, false, itemMat.data, 0)
                    GLES20.glUniform4f(shader.uBaseColorLocation, 0.85f, 0.65f, 0.15f, 1f)
                    box.render(shader.aPositionLocation, shader.aNormalLocation, shader.aColorLocation, settings.enableWireframe)
                    triCount += box.triangleCount
                    drawCallCount++
                }
                InteractableType.HOVERBOARD -> {
                    if (!interactionSystem.isHoverboardMounted) {
                        itemMat.scale(1.4f, 0.15f, 0.6f)
                        val mvp = Mat4().set(camera.viewProjMatrix).multiply(itemMat)
                        GLES20.glUniformMatrix4fv(shader.uMVPMatrixLocation, 1, false, mvp.data, 0)
                        GLES20.glUniformMatrix4fv(shader.uModelMatrixLocation, 1, false, itemMat.data, 0)
                        GLES20.glUniform4f(shader.uBaseColorLocation, 0.1f, 0.8f, 1.0f, 1f)
                        box.render(shader.aPositionLocation, shader.aNormalLocation, shader.aColorLocation, settings.enableWireframe)
                        triCount += box.triangleCount
                        drawCallCount++
                    }
                }
                InteractableType.QUANTUM_PORTAL -> {
                    itemMat.scale(2.2f, 3.2f, 0.4f)
                    val mvp = Mat4().set(camera.viewProjMatrix).multiply(itemMat)
                    GLES20.glUniformMatrix4fv(shader.uMVPMatrixLocation, 1, false, mvp.data, 0)
                    GLES20.glUniformMatrix4fv(shader.uModelMatrixLocation, 1, false, itemMat.data, 0)
                    GLES20.glUniform4f(shader.uBaseColorLocation, 0.6f, 0.1f, 0.9f, 1f)
                    box.render(shader.aPositionLocation, shader.aNormalLocation, shader.aColorLocation, settings.enableWireframe)
                    triCount += box.triangleCount
                    drawCallCount++
                }
                else -> {}
            }
        }

        // 3. Render Physics Rigid Bodies
        for (rb in physicsEngine.rigidBodies) {
            val rbMat = Mat4().translate(rb.position.x, rb.position.y, rb.position.z)
                .scale(rb.size.x, rb.size.y, rb.size.z)
            val rbMvp = Mat4().set(camera.viewProjMatrix).multiply(rbMat)

            GLES20.glUniformMatrix4fv(shader.uMVPMatrixLocation, 1, false, rbMvp.data, 0)
            GLES20.glUniformMatrix4fv(shader.uModelMatrixLocation, 1, false, rbMat.data, 0)
            GLES20.glUniform4f(shader.uBaseColorLocation, rb.color[0], rb.color[1], rb.color[2], 1f)
            GLES20.glUniform1f(shader.uSpecularStrengthLocation, 0.5f)
            GLES20.glUniform1f(shader.uShininessLocation, 32f)

            if (rb.shape == ShapeType.BOX) {
                box.render(shader.aPositionLocation, shader.aNormalLocation, shader.aColorLocation, settings.enableWireframe)
                triCount += box.triangleCount
            } else {
                val sph = sphereMesh ?: box
                sph.render(shader.aPositionLocation, shader.aNormalLocation, shader.aColorLocation, settings.enableWireframe)
                triCount += sph.triangleCount
            }
            drawCallCount++
        }

        // 3b. Render Barriers & Pads
        for (barrier in physicsEngine.barrierManager.barriers) {
            val bMat = Mat4().translate(barrier.position.x, barrier.position.y, barrier.position.z)
                .scale(barrier.size.x, barrier.size.y, barrier.size.z)
            val bMvp = Mat4().set(camera.viewProjMatrix).multiply(bMat)
            GLES20.glUniformMatrix4fv(shader.uMVPMatrixLocation, 1, false, bMvp.data, 0)
            GLES20.glUniformMatrix4fv(shader.uModelMatrixLocation, 1, false, bMat.data, 0)
            GLES20.glUniform4f(shader.uBaseColorLocation, barrier.color[0], barrier.color[1], barrier.color[2], barrier.color[3])
            box.render(shader.aPositionLocation, shader.aNormalLocation, shader.aColorLocation, settings.enableWireframe || !barrier.isPassable)
            triCount += box.triangleCount
            drawCallCount++
        }

        // 4. Render NPCs
        for (npc in npcManager.npcs) {
            if (npc.customMesh != null) {
                val npcMat = Mat4().translate(npc.position.x, npc.position.y, npc.position.z)
                    .rotate(npc.yawDeg, 0f, 1f, 0f)
                    .scale(1.2f, 1.2f, 1.2f)
                val mvp = Mat4().set(camera.viewProjMatrix).multiply(npcMat)

                val prevCullFace = GLES20.glIsEnabled(GLES20.GL_CULL_FACE)
                if (settings.twoSidedGlbRendering && prevCullFace) {
                    GLES20.glDisable(GLES20.GL_CULL_FACE)
                }

                GLES20.glUniformMatrix4fv(shader.uMVPMatrixLocation, 1, false, mvp.data, 0)
                GLES20.glUniformMatrix4fv(shader.uModelMatrixLocation, 1, false, npcMat.data, 0)
                GLES20.glUniform4f(shader.uBaseColorLocation, npc.tintColor[0], npc.tintColor[1], npc.tintColor[2], 1f)
                GLES20.glUniform1i(shader.uUseVertexColorLocation, if (npc.customMesh!!.colors != null) 1 else 0)
                npc.customMesh!!.render(shader.aPositionLocation, shader.aNormalLocation, shader.aColorLocation, settings.enableWireframe)
                GLES20.glUniform1i(shader.uUseVertexColorLocation, 0)

                if (settings.twoSidedGlbRendering && prevCullFace) {
                    GLES20.glEnable(GLES20.GL_CULL_FACE)
                }

                triCount += npc.customMesh!!.triangleCount
                drawCallCount++
            } else {
                renderNpcCharacter(npc, shader, triCount, drawCallCount).also { (t, d) ->
                    triCount = t
                    drawCallCount = d
                }
            }
        }

        // 5. Render Player Character (SOLID, UNIFIED, SEAMLESS)
        if (!camera.isFirstPerson) {
            val customChar = customModelManager.activeCustomCharacterMesh
            if (customChar != null) {
                val pConfig = customModelManager.playerConfig
                val speed = kotlin.math.sqrt(physicsEngine.characterVel.x * physicsEngine.characterVel.x + physicsEngine.characterVel.z * physicsEngine.characterVel.z)
                val isSlashing = actionManager.isActionInProgress

                val deltaTime = (currentFrameTimeMs / 1000f).coerceIn(0.001f, 0.1f)
                val animPose = animationPlayer.evaluatePose(
                    mesh = customChar,
                    config = pConfig,
                    speed = speed,
                    isGrounded = physicsEngine.isGrounded,
                    isSprinting = physicsEngine.isSprinting,
                    isSlashing = isSlashing,
                    dt = deltaTime
                )

                val facingOffset = if (settings.invertCharacterFacing) 180f else 0f
                val charMat = Mat4()
                    .translate(
                        physicsEngine.characterPos.x + animPose.offsetX,
                        physicsEngine.characterPos.y + pConfig.heightOffset + animPose.offsetY,
                        physicsEngine.characterPos.z + animPose.offsetZ
                    )
                    .rotate(-physicsEngine.characterYawDeg + pConfig.rotationOffsetYDeg + animPose.rotationYDeg + facingOffset, 0f, 1f, 0f)
                    .rotate(animPose.pitchXDeg, 1f, 0f, 0f)
                    .rotate(animPose.rollZDeg, 0f, 0f, 1f)
                    .scale(
                        pConfig.scaleX * animPose.scaleMultX,
                        pConfig.scaleY * animPose.scaleMultY,
                        pConfig.scaleZ * animPose.scaleMultZ
                    )

                val charMvp = Mat4().set(camera.viewProjMatrix).multiply(charMat)

                // Anti backface culling to ensure clothing & hair are 100% solid
                val prevCullFace = GLES20.glIsEnabled(GLES20.GL_CULL_FACE)
                if (settings.twoSidedGlbRendering && prevCullFace) {
                    GLES20.glDisable(GLES20.GL_CULL_FACE)
                }

                GLES20.glUniformMatrix4fv(shader.uMVPMatrixLocation, 1, false, charMvp.data, 0)
                GLES20.glUniformMatrix4fv(shader.uModelMatrixLocation, 1, false, charMat.data, 0)
                GLES20.glUniform4f(shader.uBaseColorLocation, 1f, 1f, 1f, 1f)
                GLES20.glUniform1f(shader.uSpecularStrengthLocation, 0.35f)
                GLES20.glUniform1f(shader.uShininessLocation, 24f)
                GLES20.glUniform1i(shader.uUseVertexColorLocation, if (customChar.colors != null) 1 else 0)

                // Render whole unified mesh (No disjointed bone slices!)
                customChar.render(
                    shader.aPositionLocation,
                    shader.aNormalLocation,
                    shader.aColorLocation,
                    settings.enableWireframe
                )
                GLES20.glUniform1i(shader.uUseVertexColorLocation, 0)

                if (settings.twoSidedGlbRendering && prevCullFace) {
                    GLES20.glEnable(GLES20.GL_CULL_FACE)
                }

                triCount += customChar.triangleCount
                drawCallCount++
            } else {
                renderAnimatedCharacter(shader, triCount, drawCallCount).also { (t, d) ->
                    triCount = t
                    drawCallCount = d
                }
            }
        }

        lastTriangles = triCount
        lastDrawCalls = drawCallCount
    }

    private fun renderNpcCharacter(npc: NpcEntity, shader: Shader, inTriCount: Int, inDrawCalls: Int): Pair<Int, Int> {
        var tri = inTriCount
        var draw = inDrawCalls
        val box = boxMesh ?: return Pair(tri, draw)

        val posX = npc.position.x
        val posY = npc.position.y
        val posZ = npc.position.z
        val yaw = npc.yawDeg

        val torsoMat = Mat4().translate(posX, posY + 1.05f, posZ)
            .rotate(yaw, 0f, 1f, 0f)
            .scale(0.5f, 0.6f, 0.32f)
        val torsoMvp = Mat4().set(camera.viewProjMatrix).multiply(torsoMat)
        GLES20.glUniformMatrix4fv(shader.uMVPMatrixLocation, 1, false, torsoMvp.data, 0)
        GLES20.glUniformMatrix4fv(shader.uModelMatrixLocation, 1, false, torsoMat.data, 0)
        GLES20.glUniform4f(shader.uBaseColorLocation, npc.tintColor[0], npc.tintColor[1], npc.tintColor[2], 1f)
        box.render(shader.aPositionLocation, shader.aNormalLocation, shader.aColorLocation, settings.enableWireframe)
        tri += box.triangleCount
        draw++

        val headMat = Mat4().translate(posX, posY + 1.55f, posZ)
            .rotate(yaw, 0f, 1f, 0f)
            .scale(0.3f, 0.3f, 0.3f)
        val headMvp = Mat4().set(camera.viewProjMatrix).multiply(headMat)
        GLES20.glUniformMatrix4fv(shader.uMVPMatrixLocation, 1, false, headMvp.data, 0)
        GLES20.glUniformMatrix4fv(shader.uModelMatrixLocation, 1, false, headMat.data, 0)
        GLES20.glUniform4f(shader.uBaseColorLocation, 0.9f, 0.85f, 0.75f, 1f)
        box.render(shader.aPositionLocation, shader.aNormalLocation, shader.aColorLocation, settings.enableWireframe)
        tri += box.triangleCount
        draw++

        val markerMat = Mat4().translate(posX, posY + 2.1f, posZ)
            .rotate(walkAnimPhase * 50f, 0f, 1f, 0f)
            .scale(0.18f, 0.18f, 0.18f)
        val markerMvp = Mat4().set(camera.viewProjMatrix).multiply(markerMat)
        GLES20.glUniformMatrix4fv(shader.uMVPMatrixLocation, 1, false, markerMvp.data, 0)
        GLES20.glUniformMatrix4fv(shader.uModelMatrixLocation, 1, false, markerMat.data, 0)
        GLES20.glUniform4f(shader.uBaseColorLocation, 0.2f, 1.0f, 0.8f, 1f)
        box.render(shader.aPositionLocation, shader.aNormalLocation, shader.aColorLocation, settings.enableWireframe)
        tri += box.triangleCount
        draw++

        return Pair(tri, draw)
    }

    private fun renderAnimatedCharacter(shader: Shader, inTriCount: Int, inDrawCalls: Int): Pair<Int, Int> {
        var tri = inTriCount
        var draw = inDrawCalls
        val box = boxMesh ?: return Pair(tri, draw)
        val blade = bladeMesh ?: return Pair(tri, draw)

        val posX = physicsEngine.characterPos.x
        val posY = physicsEngine.characterPos.y
        val posZ = physicsEngine.characterPos.z
        val facingOffset = if (settings.invertCharacterFacing) 180f else 0f
        val yaw = -physicsEngine.characterYawDeg + facingOffset

        val crouchScaleY = if (physicsEngine.isCrouched) 0.65f else 1.0f
        val legSwing = sin(walkAnimPhase.toDouble()).toFloat() * 25f

        if (interactionSystem.isHoverboardMounted) {
            val boardMat = Mat4().translate(posX, posY + 0.15f, posZ)
                .rotate(yaw, 0f, 1f, 0f)
                .scale(0.7f, 0.12f, 1.5f)
            val boardMvp = Mat4().set(camera.viewProjMatrix).multiply(boardMat)
            GLES20.glUniformMatrix4fv(shader.uMVPMatrixLocation, 1, false, boardMvp.data, 0)
            GLES20.glUniformMatrix4fv(shader.uModelMatrixLocation, 1, false, boardMat.data, 0)
            GLES20.glUniform4f(shader.uBaseColorLocation, 0.05f, 0.9f, 1.0f, 1f)
            box.render(shader.aPositionLocation, shader.aNormalLocation, shader.aColorLocation, settings.enableWireframe)
            tri += box.triangleCount
            draw++
        }

        val torsoMat = Mat4().translate(posX, posY + (1.1f * crouchScaleY), posZ)
            .rotate(yaw, 0f, 1f, 0f)
            .scale(0.55f, 0.65f * crouchScaleY, 0.35f)
        val torsoMvp = Mat4().set(camera.viewProjMatrix).multiply(torsoMat)
        GLES20.glUniformMatrix4fv(shader.uMVPMatrixLocation, 1, false, torsoMvp.data, 0)
        GLES20.glUniformMatrix4fv(shader.uModelMatrixLocation, 1, false, torsoMat.data, 0)
        GLES20.glUniform4f(shader.uBaseColorLocation, 0.15f, 0.20f, 0.28f, 1f)
        box.render(shader.aPositionLocation, shader.aNormalLocation, shader.aColorLocation, settings.enableWireframe)
        tri += box.triangleCount
        draw++

        val headMat = Mat4().translate(posX, posY + (1.6f * crouchScaleY), posZ)
            .rotate(yaw, 0f, 1f, 0f)
            .scale(0.32f, 0.32f, 0.32f)
        val headMvp = Mat4().set(camera.viewProjMatrix).multiply(headMat)
        GLES20.glUniformMatrix4fv(shader.uMVPMatrixLocation, 1, false, headMvp.data, 0)
        GLES20.glUniformMatrix4fv(shader.uModelMatrixLocation, 1, false, headMat.data, 0)
        GLES20.glUniform4f(shader.uBaseColorLocation, 0.85f, 0.85f, 0.9f, 1f)
        box.render(shader.aPositionLocation, shader.aNormalLocation, shader.aColorLocation, settings.enableWireframe)
        tri += box.triangleCount
        draw++

        val visorMat = Mat4().translate(posX, posY + (1.62f * crouchScaleY), posZ)
            .rotate(yaw, 0f, 1f, 0f)
            .translate(0f, 0f, -0.17f)
            .scale(0.25f, 0.08f, 0.05f)
        val visorMvp = Mat4().set(camera.viewProjMatrix).multiply(visorMat)
        GLES20.glUniformMatrix4fv(shader.uMVPMatrixLocation, 1, false, visorMvp.data, 0)
        GLES20.glUniformMatrix4fv(shader.uModelMatrixLocation, 1, false, visorMat.data, 0)
        GLES20.glUniform4f(shader.uBaseColorLocation, 0f, 0.95f, 1f, 1f)
        box.render(shader.aPositionLocation, shader.aNormalLocation, shader.aColorLocation, settings.enableWireframe)
        tri += box.triangleCount
        draw++

        val legLMat = Mat4().translate(posX, posY + 0.4f * crouchScaleY, posZ)
            .rotate(yaw, 0f, 1f, 0f)
            .translate(-0.16f, 0f, 0f)
            .rotate(legSwing, 1f, 0f, 0f)
            .scale(0.18f, 0.75f * crouchScaleY, 0.18f)
        val legLMvp = Mat4().set(camera.viewProjMatrix).multiply(legLMat)
        GLES20.glUniformMatrix4fv(shader.uMVPMatrixLocation, 1, false, legLMvp.data, 0)
        GLES20.glUniformMatrix4fv(shader.uModelMatrixLocation, 1, false, legLMat.data, 0)
        GLES20.glUniform4f(shader.uBaseColorLocation, 0.25f, 0.28f, 0.35f, 1f)
        box.render(shader.aPositionLocation, shader.aNormalLocation, shader.aColorLocation, settings.enableWireframe)
        tri += box.triangleCount
        draw++

        val legRMat = Mat4().translate(posX, posY + 0.4f * crouchScaleY, posZ)
            .rotate(yaw, 0f, 1f, 0f)
            .translate(0.16f, 0f, 0f)
            .rotate(-legSwing, 1f, 0f, 0f)
            .scale(0.18f, 0.75f * crouchScaleY, 0.18f)
        val legRMvp = Mat4().set(camera.viewProjMatrix).multiply(legRMat)
        GLES20.glUniformMatrix4fv(shader.uMVPMatrixLocation, 1, false, legRMvp.data, 0)
        GLES20.glUniformMatrix4fv(shader.uModelMatrixLocation, 1, false, legRMat.data, 0)
        box.render(shader.aPositionLocation, shader.aNormalLocation, shader.aColorLocation, settings.enableWireframe)
        tri += box.triangleCount
        draw++

        val armSwing = if (actionManager.isActionInProgress) {
            actionManager.swingAngle
        } else {
            -legSwing * 0.8f
        }

        val armRMat = Mat4().translate(posX, posY + (1.2f * crouchScaleY), posZ)
            .rotate(yaw, 0f, 1f, 0f)
            .translate(0.38f, 0f, 0f)
            .rotate(armSwing, 1f, 0f, 0f)
            .scale(0.14f, 0.55f, 0.14f)
        val armRMvp = Mat4().set(camera.viewProjMatrix).multiply(armRMat)
        GLES20.glUniformMatrix4fv(shader.uMVPMatrixLocation, 1, false, armRMvp.data, 0)
        GLES20.glUniformMatrix4fv(shader.uModelMatrixLocation, 1, false, armRMat.data, 0)
        GLES20.glUniform4f(shader.uBaseColorLocation, 0.25f, 0.28f, 0.35f, 1f)
        box.render(shader.aPositionLocation, shader.aNormalLocation, shader.aColorLocation, settings.enableWireframe)
        tri += box.triangleCount
        draw++

        val swordMat = Mat4().translate(posX, posY + (1.2f * crouchScaleY), posZ)
            .rotate(yaw, 0f, 1f, 0f)
            .translate(0.40f, -0.25f, -0.1f)
            .rotate(armSwing - 45f, 1f, 0f, 0f)
            .scale(1.2f, 1.2f, 1.2f)
        val swordMvp = Mat4().set(camera.viewProjMatrix).multiply(swordMat)
        GLES20.glUniformMatrix4fv(shader.uMVPMatrixLocation, 1, false, swordMvp.data, 0)
        GLES20.glUniformMatrix4fv(shader.uModelMatrixLocation, 1, false, swordMat.data, 0)
        GLES20.glUniform4f(shader.uBaseColorLocation, 0f, 0.9f, 1f, 1f)
        blade.render(shader.aPositionLocation, shader.aNormalLocation, shader.aColorLocation, settings.enableWireframe)
        tri += blade.triangleCount
        draw++

        return Pair(tri, draw)
    }
}