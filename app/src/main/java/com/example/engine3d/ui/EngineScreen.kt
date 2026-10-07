package com.example.engine3d.ui

import android.annotation.SuppressLint
import android.opengl.GLSurfaceView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.North
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SwitchVideo
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.engine3d.actions.ActionManager
import com.example.engine3d.actions.InteractionSystem
import com.example.engine3d.controller.ExpandedContainerConfig
import com.example.engine3d.controller.ExpandedContainerType
import com.example.engine3d.controller.ExpandedContentType
import com.example.engine3d.controller.GamepadHandler
import com.example.engine3d.controller.HudControlId
import com.example.engine3d.controller.HudElementConfig
import com.example.engine3d.controller.HudPreferences
import com.example.engine3d.core.CameraPresetMode
import com.example.engine3d.importer.BatchImportManager
import com.example.engine3d.importer.CustomModelManager
import com.example.engine3d.npc.NpcManager
import com.example.engine3d.physics.PhysicsEngine
import com.example.engine3d.renderer.Apex3DRenderer
import com.example.engine3d.renderer.EnginePerformanceStats
import com.example.engine3d.renderer.EngineSettings
import com.example.engine3d.terrain.TerrainMesh
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

@SuppressLint("ClickableViewAccessibility")
@Composable
fun EngineScreen(
    settings: EngineSettings = remember { EngineSettings() },
    terrainMesh: TerrainMesh = remember { TerrainMesh() },
    customModelManager: CustomModelManager,
    npcManager: NpcManager,
    physicsEngine: PhysicsEngine = remember { PhysicsEngine(terrainMesh.heightQuery) },
    actionManager: ActionManager = remember { ActionManager(physicsEngine) },
    interactionSystem: InteractionSystem = remember { InteractionSystem() },
    expandedContainerConfig: ExpandedContainerConfig = remember { ExpandedContainerConfig() },
    gamepadHandler: GamepadHandler? = null,
    onExpandedConfigChanged: (ExpandedContainerConfig) -> Unit = {},
    onBackToLobby: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    BackHandler { onBackToLobby() }

    // Core Engine Instances (Passed as parameters)
    val batchImportManager = remember {
        BatchImportManager(
            context = context,
            customModelManager = customModelManager,
            terrainMesh = terrainMesh,
            actionManager = actionManager,
            npcManager = npcManager,
            barrierManager = physicsEngine.barrierManager
        )
    }
    val hudPreferences = remember { HudPreferences(context) }

    // Renderer & Stats
    var stats by remember {
        mutableStateOf(EnginePerformanceStats(60, 16.6f, 0, 0, 0f))
    }
    val renderer = remember {
        Apex3DRenderer(
            context = context,
            settings = settings,
            terrainMesh = terrainMesh,
            physicsEngine = physicsEngine,
            actionManager = actionManager,
            interactionSystem = interactionSystem,
            customModelManager = customModelManager,
            npcManager = npcManager
        ).apply {
            onPerformanceUpdate = { s -> stats = s }
        }
    }

    // Connect Gamepad / Keyboard inputs
    DisposableEffect(gamepadHandler, physicsEngine, actionManager, interactionSystem) {
        gamepadHandler?.let { gp ->
            gp.onJumpPressed = { physicsEngine.jump() }
            gp.onAttackPressed = { actionManager.triggerAction(actionManager.primaryAction) }
            gp.onCrouchToggle = { physicsEngine.isCrouched = !physicsEngine.isCrouched }
            gp.onInteractPressed = {
                val nearbyN = npcManager.nearbyNpc
                if (nearbyN != null) {
                    npcManager.startDialogue(nearbyN)
                } else {
                    interactionSystem.interact(physicsEngine.characterPos)
                }
            }
            gp.onAction1Pressed = { actionManager.triggerAction(actionManager.secondaryAction) }
            gp.onAction2Pressed = { actionManager.triggerAction(actionManager.utilityAction) }
        }
        onDispose {
            gamepadHandler?.let { gp ->
                gp.onJumpPressed = null
                gp.onAttackPressed = null
                gp.onCrouchToggle = null
                gp.onInteractPressed = null
                gp.onAction1Pressed = null
                gp.onAction2Pressed = null
            }
        }
    }

    var glSurfaceView by remember { mutableStateOf<GLSurfaceView?>(null) }

    // UI Sheets & Modes
    var isEditHudMode by remember { mutableStateOf(false) }
    var showStudioMenuSheet by remember { mutableStateOf(false) }
    var showGraphicsSheet by remember { mutableStateOf(false) }
    var showAssetManagerSheet by remember { mutableStateOf(false) }
    var isExpandedMenuOpen by remember { mutableStateOf(false) }

    // HUD Config State
    var activePresetName by remember { mutableStateOf(hudPreferences.getActivePresetName()) }
    var hudConfigs by remember { mutableStateOf(hudPreferences.loadLayout(activePresetName)) }
    var currentCameraMode by remember { mutableStateOf(CameraPresetMode.DYNAMIC_EXPLORATION) }

    // Joystick Touch Offsets
    var joystickThumbOffset by remember { mutableStateOf(Offset.Zero) }
    var isSprintLocked by remember { mutableStateOf(false) }

    // Interaction & NPC Proximity State
    val nearbyObject = interactionSystem.currentNearbyObject
    val nearbyNpc = npcManager.nearbyNpc
    val talkingNpc = npcManager.activeTalkingNpc
    val statusMsg = interactionSystem.statusMessage

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        // 1. OpenGL ES SurfaceView Viewport
        AndroidView(
            factory = { ctx ->
                GLSurfaceView(ctx).apply {
                    setEGLContextClientVersion(2)
                    setRenderer(renderer)
                    renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                    glSurfaceView = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. In-Game HUD Controls
        if (!isEditHudMode) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val screenW = maxWidth.value
                val screenH = maxHeight.value
                val isPortrait = screenW < screenH

                 // 2a. Global / Right-Side Camera Swipe Gesture Surface (Behind HUD Buttons)
                 // Usap bebas di mana saja di area kosong untuk menggeser kamera, serta cubit (pinch) untuk zoom in/out
                 Box(
                     modifier = Modifier
                         .fillMaxSize()
                         .pointerInput(Unit) {
                             detectTransformGestures { centroid, pan, zoom, rotation ->
                                 val sensitivity = 0.28f
                                 // Rotasi kamera dengan arah usap standard (tidak terbalik lagi)
                                 renderer.camera.rotate(
                                     deltaYaw = pan.x * sensitivity,
                                     deltaPitch = -pan.y * sensitivity
                                 )
                                 // Cubit (Pinch) untuk Zoom In / Out secara halus & responsif
                                 if (zoom != 1f) {
                                     val zoomSensitivity = 12.0f
                                     renderer.camera.zoom((1f - zoom) * zoomSensitivity)
                                 }
                             }
                         }
                 )

                hudConfigs.forEach { (id, cfg) ->
                    if (!cfg.isEnabled) return@forEach
                    // Auto-hide Interaksi saat tidak ada NPC atau objek interaktif di dekat karakter agar layar bersih
                    if (id == HudControlId.INTERACT && nearbyNpc == null && nearbyObject == null) return@forEach

                    val baseScale = if (isPortrait) 0.88f else 1.0f
                    val baseSize = when (id) {
                        HudControlId.JOYSTICK -> 118.dp * baseScale
                        HudControlId.LOOK_PAD -> if (isPortrait) 130.dp else 160.dp
                        HudControlId.ATTACK -> 70.dp * baseScale
                        HudControlId.JUMP -> 62.dp * baseScale
                        HudControlId.CROUCH, HudControlId.ACTION_SLAM, HudControlId.ACTION_DASH -> 56.dp * baseScale
                        HudControlId.INTERACT -> 66.dp * baseScale
                        HudControlId.EXPANDED_MENU -> 60.dp * baseScale
                        else -> 46.dp * baseScale
                    }
                    val currentSize = baseSize * cfg.scale
                    val posX = (cfg.xPercent * screenW - currentSize.value / 2f).coerceIn(0f, screenW - currentSize.value)
                    val posY = (cfg.yPercent * screenH - currentSize.value / 2f).coerceIn(0f, screenH - currentSize.value)

                    when (id) {
                        HudControlId.JOYSTICK -> {
                            val maxRadius = (currentSize.value / 2f) - 14f
                            Box(
                                modifier = Modifier
                                    .offset { IntOffset(posX.dp.roundToPx(), posY.dp.roundToPx()) }
                                    .size(currentSize)
                                    .alpha(cfg.alpha)
                                    .clip(CircleShape)
                                    .background(Color(0x55000000))
                                    .border(2.dp, Color(0x6600E5FF), CircleShape)
                                    .pointerInput(Unit) {
                                        detectDragGestures(
                                            onDragEnd = {
                                                if (!isSprintLocked) {
                                                    joystickThumbOffset = Offset.Zero
                                                    renderer.inputStickX = 0f
                                                    renderer.inputStickY = 0f
                                                    physicsEngine.isSprinting = false
                                                }
                                            },
                                            onDragCancel = {
                                                if (!isSprintLocked) {
                                                    joystickThumbOffset = Offset.Zero
                                                    renderer.inputStickX = 0f
                                                    renderer.inputStickY = 0f
                                                    physicsEngine.isSprinting = false
                                                }
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                val next = joystickThumbOffset + dragAmount
                                                val dist = sqrt(next.x * next.x + next.y * next.y)
                                                val clamped = if (dist > maxRadius) {
                                                    Offset(next.x / dist * maxRadius, next.y / dist * maxRadius)
                                                } else next
                                                joystickThumbOffset = clamped

                                                val inX = (clamped.x / maxRadius).coerceIn(-1f, 1f)
                                                val inY = (-clamped.y / maxRadius).coerceIn(-1f, 1f)
                                                renderer.inputStickX = inX
                                                renderer.inputStickY = inY

                                                if (inY > 0.85f && !isSprintLocked) {
                                                    physicsEngine.isSprinting = true
                                                }
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .offset { IntOffset(joystickThumbOffset.x.roundToInt(), joystickThumbOffset.y.roundToInt()) }
                                        .size(currentSize * 0.42f)
                                        .clip(CircleShape)
                                        .background(Color(0xCC00E5FF))
                                        .border(2.dp, Color.White, CircleShape)
                                )
                            }
                        }

                         HudControlId.LOOK_PAD -> {
                             Box(
                                 modifier = Modifier
                                     .offset { IntOffset(posX.dp.roundToPx(), posY.dp.roundToPx()) }
                                     .size(currentSize)
                                     .alpha(cfg.alpha)
                                     .clip(RoundedCornerShape(16.dp))
                                     .background(Color(0x22FFFFFF))
                                     .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                                     .pointerInput(Unit) {
                                         detectTransformGestures { centroid, pan, zoom, rotation ->
                                             val sensitivity = 0.35f
                                             renderer.camera.rotate(
                                                 deltaYaw = pan.x * sensitivity,
                                                 deltaPitch = -pan.y * sensitivity
                                             )
                                             if (zoom != 1f) {
                                                 val zoomSensitivity = 12.0f
                                                 renderer.camera.zoom((1f - zoom) * zoomSensitivity)
                                             }
                                         }
                                     },
                                 contentAlignment = Alignment.Center
                             ) {
                                Text(
                                    text = "Usap Kamera",
                                    color = Color(0x77FFFFFF),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        HudControlId.SPRINT_LOCK -> {
                            HudCircularButton(
                                icon = Icons.AutoMirrored.Filled.DirectionsRun,
                                label = if (isSprintLocked) "Lari: ON" else "Kunci Lari",
                                size = currentSize,
                                posX = posX,
                                posY = posY,
                                alpha = cfg.alpha,
                                bgColor = if (isSprintLocked) Color(0xDD00E676) else Color(0x77263238),
                                onClick = {
                                    isSprintLocked = !isSprintLocked
                                    physicsEngine.isSprinting = isSprintLocked
                                    if (isSprintLocked) {
                                        renderer.inputStickY = 1.0f
                                    } else {
                                        renderer.inputStickY = 0f
                                        joystickThumbOffset = Offset.Zero
                                    }
                                }
                            )
                        }

                        HudControlId.JUMP -> {
                            HudCircularButton(
                                icon = Icons.Default.North,
                                label = "Lompat",
                                size = currentSize,
                                posX = posX,
                                posY = posY,
                                alpha = cfg.alpha,
                                bgColor = Color(0x8800E5FF),
                                onClick = { physicsEngine.jump() }
                            )
                        }

                        HudControlId.CROUCH -> {
                            HudCircularButton(
                                icon = Icons.Default.ArrowDownward,
                                label = if (physicsEngine.isCrouched) "Berdiri" else "Jongkok",
                                size = currentSize,
                                posX = posX,
                                posY = posY,
                                alpha = cfg.alpha,
                                bgColor = if (physicsEngine.isCrouched) Color(0xCCFFD600) else Color(0x7737474F),
                                onClick = { physicsEngine.isCrouched = !physicsEngine.isCrouched }
                            )
                        }

                        HudControlId.ATTACK -> {
                            HudCircularButton(
                                icon = Icons.Default.FlashOn,
                                label = actionManager.primaryAction.name,
                                size = currentSize,
                                posX = posX,
                                posY = posY,
                                alpha = cfg.alpha,
                                bgColor = Color(0xDDFF3D00),
                                onClick = { actionManager.triggerAction(actionManager.primaryAction) }
                            )
                        }

                        HudControlId.ACTION_SLAM -> {
                            HudCircularButton(
                                icon = Icons.Default.Terrain,
                                label = actionManager.secondaryAction.name,
                                size = currentSize,
                                posX = posX,
                                posY = posY,
                                alpha = cfg.alpha,
                                bgColor = Color(0xCCFF9100),
                                onClick = { actionManager.triggerAction(actionManager.secondaryAction) }
                            )
                        }

                        HudControlId.ACTION_DASH -> {
                            HudCircularButton(
                                icon = Icons.Default.Speed,
                                label = actionManager.utilityAction.name,
                                size = currentSize,
                                posX = posX,
                                posY = posY,
                                alpha = cfg.alpha,
                                bgColor = Color(0xCC76FF03),
                                onClick = { actionManager.triggerAction(actionManager.utilityAction) }
                            )
                        }

                        HudControlId.INTERACT -> {
                            val isNpcNear = nearbyNpc != null
                            val isObjectNear = nearbyObject != null

                            val interactLabel = when {
                                isNpcNear -> "Bicara"
                                isObjectNear -> nearbyObject!!.promptText
                                else -> "Interaksi"
                            }
                            val interactIcon = if (isNpcNear) Icons.AutoMirrored.Filled.Chat else Icons.Default.TouchApp

                            HudCircularButton(
                                icon = interactIcon,
                                label = interactLabel,
                                size = currentSize,
                                posX = posX,
                                posY = posY,
                                alpha = if (isNpcNear || isObjectNear) 1.0f else cfg.alpha * 0.5f,
                                bgColor = if (isNpcNear) Color(0xFF00E5FF) else if (isObjectNear) Color(0xEEFFEA00) else Color(0x55455A64),
                                iconTint = if (isNpcNear || isObjectNear) Color.Black else Color.White,
                                textColor = if (isNpcNear || isObjectNear) Color.Black else Color.White,
                                onClick = {
                                    if (isNpcNear) {
                                        npcManager.startDialogue(nearbyNpc!!)
                                    } else if (isObjectNear) {
                                        interactionSystem.interact(physicsEngine.characterPos)
                                    }
                                }
                            )
                        }

                        HudControlId.EXPANDED_MENU -> {
                            HudCircularButton(
                                icon = Icons.Default.Apps,
                                label = if (isExpandedMenuOpen) "Tutup" else "Menu",
                                size = currentSize,
                                posX = posX,
                                posY = posY,
                                alpha = cfg.alpha,
                                bgColor = if (isExpandedMenuOpen) Color(0xFFFFD600) else Color(0xAAFF9100),
                                iconTint = Color.Black,
                                textColor = Color.Black,
                                onClick = { isExpandedMenuOpen = !isExpandedMenuOpen }
                            )
                        }

                        HudControlId.CAMERA_SWITCH -> {
                            val modeLabel = when (currentCameraMode) {
                                CameraPresetMode.DYNAMIC_EXPLORATION -> "AC Cam"
                                CameraPresetMode.STEALTH_CROUCH -> "Stealth"
                                CameraPresetMode.COMBAT_FOCUS -> "Tempur"
                                CameraPresetMode.EAGLE_PANORAMA -> "Elang"
                                CameraPresetMode.FIRST_PERSON -> "FPP"
                            }
                            val modeBgColor = when (currentCameraMode) {
                                CameraPresetMode.DYNAMIC_EXPLORATION -> Color(0x9900E5FF)
                                CameraPresetMode.STEALTH_CROUCH -> Color(0x9976FF03)
                                CameraPresetMode.COMBAT_FOCUS -> Color(0x99FF3D00)
                                CameraPresetMode.EAGLE_PANORAMA -> Color(0x99FFD600)
                                CameraPresetMode.FIRST_PERSON -> Color(0x889C27B0)
                            }
                            HudCircularButton(
                                icon = Icons.Default.SwitchVideo,
                                label = modeLabel,
                                size = currentSize,
                                posX = posX,
                                posY = posY,
                                alpha = cfg.alpha,
                                bgColor = modeBgColor,
                                iconTint = if (currentCameraMode == CameraPresetMode.DYNAMIC_EXPLORATION || currentCameraMode == CameraPresetMode.EAGLE_PANORAMA) Color.Black else Color.White,
                                textColor = if (currentCameraMode == CameraPresetMode.DYNAMIC_EXPLORATION || currentCameraMode == CameraPresetMode.EAGLE_PANORAMA) Color.Black else Color.White,
                                onClick = {
                                    currentCameraMode = renderer.camera.cycleCameraMode()
                                }
                            )
                        }

                        HudControlId.FLASHLIGHT -> {
                            HudCircularButton(
                                icon = Icons.Default.Highlight,
                                label = "Lampu",
                                size = currentSize,
                                posX = posX,
                                posY = posY,
                                alpha = cfg.alpha,
                                bgColor = Color(0x77263238),
                                onClick = {
                                    if (renderer.lighting.pointLights.isNotEmpty()) {
                                        val light = renderer.lighting.pointLights[0]
                                        light.intensity = if (light.intensity > 0.1f) 0.0f else 2.5f
                                    }
                                }
                            )
                        }

                        HudControlId.RESET_POS -> {
                            HudCircularButton(
                                icon = Icons.Default.RestartAlt,
                                label = "Reset",
                                size = currentSize,
                                posX = posX,
                                posY = posY,
                                alpha = cfg.alpha,
                                bgColor = Color(0x77263238),
                                onClick = { physicsEngine.resetCharacterPosition() }
                            )
                        }
                    }
                }

                // Expanded Control Container Overlay
                if (isExpandedMenuOpen) {
                    val expCfg = hudConfigs[HudControlId.EXPANDED_MENU]
                    val originX = if (expCfg != null) (expCfg.xPercent * screenW) else screenW * 0.85f
                    val originY = if (expCfg != null) (expCfg.yPercent * screenH) else screenH * 0.35f

                    RenderExpandedContainer(
                        config = expandedContainerConfig,
                        originX = originX,
                        originY = originY,
                        actionManager = actionManager,
                        interactionSystem = interactionSystem,
                        physicsEngine = physicsEngine,
                        onClose = { isExpandedMenuOpen = false }
                    )
                }
            }

            // 3. Top Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Back to Lobby button
                    IconButton(
                        onClick = onBackToLobby,
                        modifier = Modifier
                            .background(Color(0xD0101726), CircleShape)
                            .border(1.dp, Color(0xFF00E5FF), CircleShape)
                            .testTag("back_to_lobby_button")
                    ) {
                        Icon(Icons.Default.Home, contentDescription = "Kembali ke Lobby", tint = Color(0xFF00E5FF))
                    }

                    // Performance stats widget
                    PerformanceHud(
                        stats = stats,
                        presetName = settings.activePreset.name.take(7)
                    )
                }

                // Engine Unified Settings & Studio Menu Button
                Button(
                    onClick = { showStudioMenuSheet = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xD0101726)),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF)),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("open_studio_menu_button")
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Menu Apex3D", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            // 4. Interactive NPC Dialogue Box
            if (talkingNpc != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 110.dp, start = 20.dp, end = 20.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFA101726)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, Color(0xFF00E5FF), RoundedCornerShape(16.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF00E5FF)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                                    }
                                    Text(
                                        text = "${talkingNpc.name} [${talkingNpc.role}]",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00E5FF),
                                        fontSize = 14.sp
                                    )
                                }

                                IconButton(
                                    onClick = { npcManager.closeDialogue() },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                                }
                            }

                            val currentText = talkingNpc.dialogues.getOrNull(npcManager.activeDialogueIndex) ?: ""
                            Text(
                                text = "\"$currentText\"",
                                color = Color.White,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                val isLast = npcManager.activeDialogueIndex >= talkingNpc.dialogues.size - 1
                                Button(
                                    onClick = { npcManager.nextDialogue() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                                ) {
                                    Text(
                                        text = if (isLast) "Selesai Bicara" else "Lanjut ▾",
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
             } else if (statusMsg != null || nearbyObject != null || nearbyNpc != null || physicsEngine.barrierManager.activeTriggerMessage != null) {
                 // 5. Proximity & Barrier Banner (Move to bottom-center out of the character's way)
                 Box(
                     modifier = Modifier
                         .fillMaxSize()
                         .padding(bottom = 28.dp),
                     contentAlignment = Alignment.BottomCenter
                 ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .background(Color(0xDD0D131F), RoundedCornerShape(12.dp))
                            .border(1.5.dp, Color(0xFF00E5FF), RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        val barrierMsg = physicsEngine.barrierManager.activeTriggerMessage
                        if (barrierMsg != null) {
                            Text(
                                text = barrierMsg,
                                color = Color(0xFFFF5252),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        } else if (statusMsg != null) {
                            Text(
                                text = statusMsg,
                                color = Color(0xFF76FF03),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        } else if (nearbyNpc != null) {
                            Text(
                                text = "Bicara dengan ${nearbyNpc.name} (${nearbyNpc.role})",
                                color = Color(0xFF00E5FF),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Tekan Tombol [Bicara] untuk memulai dialog",
                                color = Color(0xFFFFD600),
                                fontSize = 11.sp
                            )
                        } else if (nearbyObject != null) {
                            Text(
                                text = "Dekat Objek: ${nearbyObject.name}",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Tekan Tombol Interaksi: ${nearbyObject.promptText}",
                                color = Color(0xFFFFD600),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // 6. Full Screen Live HUD Editor (When in edit mode)
        if (isEditHudMode) {
            HudEditorOverlay(
                configs = hudConfigs,
                expandedConfig = expandedContainerConfig,
                onConfigsUpdated = { updated ->
                    hudConfigs = updated
                },
                onExpandedConfigUpdated = { newExp ->
                    onExpandedConfigChanged(newExp)
                },
                onSaveAndExit = {
                    hudPreferences.saveLayout(activePresetName, hudConfigs)
                    isEditHudMode = false
                },
                onCancel = {
                    hudConfigs = hudPreferences.loadLayout(activePresetName)
                    isEditHudMode = false
                }
            )
        }

        // 7. Studio Unified Menu Bottom Sheet
        if (showStudioMenuSheet) {
            StudioMenuSheet(
                settings = settings,
                renderer = renderer,
                physicsEngine = physicsEngine,
                actionManager = actionManager,
                interactionSystem = interactionSystem,
                customModelManager = customModelManager,
                batchImportManager = batchImportManager,
                terrainMesh = terrainMesh,
                npcManager = npcManager,
                barrierManager = physicsEngine.barrierManager,
                hudPreferences = hudPreferences,
                activePresetName = activePresetName,
                onPresetChanged = { name, configs ->
                    activePresetName = name
                    hudConfigs = configs
                },
                onOpenHudEditor = {
                    showStudioMenuSheet = false
                    isEditHudMode = true
                },
                onOpenAssetSheet = {
                    showStudioMenuSheet = false
                    showAssetManagerSheet = true
                },
                onSettingsChanged = {
                    glSurfaceView?.requestRender()
                },
                onDismiss = { showStudioMenuSheet = false }
            )
        }

        // 8. Graphics Settings Bottom Sheet
        if (showGraphicsSheet) {
            GraphicsSettingsSheet(
                settings = settings,
                onSettingsChanged = {
                    glSurfaceView?.requestRender()
                },
                onSpawnPhysicsCrate = {
                    physicsEngine.spawnPhysicsBox(physicsEngine.characterPos)
                },
                onDismiss = { showGraphicsSheet = false }
            )
        }

        // 9. Batch Folder & Asset Manager Bottom Sheet
        if (showAssetManagerSheet) {
            AssetManagerSheet(
                customModelManager = customModelManager,
                batchImportManager = batchImportManager,
                terrainMesh = terrainMesh,
                actionManager = actionManager,
                npcManager = npcManager,
                barrierManager = physicsEngine.barrierManager,
                onModelImported = { entry ->
                    glSurfaceView?.requestRender()
                },
                onTerrainChanged = {
                    glSurfaceView?.requestRender()
                },
                onDismiss = { showAssetManagerSheet = false }
            )
        }
    }
}

@Composable
fun RenderExpandedContainer(
    config: ExpandedContainerConfig,
    originX: Float,
    originY: Float,
    actionManager: ActionManager,
    interactionSystem: InteractionSystem,
    physicsEngine: PhysicsEngine,
    onClose: () -> Unit
) {
    when (config.containerType) {
        ExpandedContainerType.RADIAL_WHEEL -> {
            // Radial Wheel circle around origin button
            val radius = config.containerRadiusDp
            val items = when (config.contentType) {
                ExpandedContentType.ACTIONS_PALETTE -> listOf(
                    Triple("Slash", Icons.Default.FlashOn, Color(0xFF00E5FF)) to { actionManager.triggerAction(actionManager.primaryAction) },
                    Triple("Slam", Icons.Default.Terrain, Color(0xFFFF9100)) to { actionManager.triggerAction(actionManager.secondaryAction) },
                    Triple("Dash", Icons.Default.Speed, Color(0xFF76FF03)) to { actionManager.triggerAction(actionManager.utilityAction) },
                    Triple("Leap", Icons.Default.North, Color(0xFFFFD600)) to {
                        physicsEngine.applyImpulse(com.example.engine3d.math.Vec3(0f, 13f, 0f))
                    }
                )
                ExpandedContentType.QUICK_GADGETS -> listOf(
                    Triple("Hover", Icons.AutoMirrored.Filled.DirectionsRun, Color(0xFF00E5FF)) to { interactionSystem.isHoverboardMounted = !interactionSystem.isHoverboardMounted },
                    Triple("Reset", Icons.Default.RestartAlt, Color(0xFFFF5252)) to { physicsEngine.resetCharacterPosition() },
                    Triple("Lompat", Icons.Default.North, Color(0xFFFFD600)) to { physicsEngine.jump() },
                    Triple("Jongkok", Icons.Default.ArrowDownward, Color(0xFFB0BEC5)) to { physicsEngine.isCrouched = !physicsEngine.isCrouched }
                )
                else -> listOf(
                    Triple("Aksi 1", Icons.Default.FlashOn, Color(0xFF00E5FF)) to { actionManager.triggerAction(actionManager.primaryAction) },
                    Triple("Aksi 2", Icons.Default.Terrain, Color(0xFFFF9100)) to { actionManager.triggerAction(actionManager.secondaryAction) }
                )
            }

            items.forEachIndexed { index, (meta, action) ->
                val angle = (index.toFloat() / items.size) * 2f * Math.PI.toFloat() - (Math.PI / 2f).toFloat()
                val offX = originX + cos(angle) * radius - 24f
                val offY = originY + sin(angle) * radius - 24f

                Box(
                    modifier = Modifier
                        .offset { IntOffset(offX.dp.roundToPx(), offY.dp.roundToPx()) }
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xE0101726))
                        .border(1.5.dp, meta.third, CircleShape)
                        .clickable {
                            action()
                            if (config.autoCloseOnSelect) onClose()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(meta.second, contentDescription = meta.first, tint = meta.third, modifier = Modifier.size(24.dp))
                }
            }
        }
        else -> {
            // Horizontal Bar or Card layout
            Box(
                modifier = Modifier
                    .offset { IntOffset((originX - 110f).coerceAtLeast(10f).dp.roundToPx(), (originY - 60f).coerceAtLeast(40f).dp.roundToPx()) }
                    .background(Color(0xEE0B1220), RoundedCornerShape(12.dp))
                    .border(1.5.dp, Color(0xFF00E5FF), RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            actionManager.triggerAction(actionManager.primaryAction)
                            if (config.autoCloseOnSelect) onClose()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                    ) {
                        Text("Slash", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            actionManager.triggerAction(actionManager.secondaryAction)
                            if (config.autoCloseOnSelect) onClose()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9100))
                    ) {
                        Text("Slam", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            physicsEngine.applyImpulse(com.example.engine3d.math.Vec3(0f, 13f, 0f))
                            if (config.autoCloseOnSelect) onClose()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF76FF03))
                    ) {
                        Text("Super Leap", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun HudCircularButton(
    icon: ImageVector,
    label: String,
    size: androidx.compose.ui.unit.Dp,
    posX: Float,
    posY: Float,
    alpha: Float,
    bgColor: Color,
    iconTint: Color = Color.White,
    textColor: Color = Color.White,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .offset { IntOffset(posX.dp.roundToPx(), posY.dp.roundToPx()) }
            .size(size)
            .alpha(alpha)
            .clip(CircleShape)
            .background(bgColor)
            .border(1.5.dp, Color(0x66FFFFFF), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(size * 0.42f)
            )
            Text(
                text = label,
                color = textColor,
                fontSize = (8f * (size.value / 55f)).coerceIn(7f, 11f).sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}
