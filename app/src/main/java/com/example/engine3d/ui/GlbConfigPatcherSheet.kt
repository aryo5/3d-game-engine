package com.example.engine3d.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.engine3d.actions.ActionManager
import com.example.engine3d.actions.InteractionSystem
import com.example.engine3d.actions.InteractableType
import com.example.engine3d.actions.WorldInteractable
import com.example.engine3d.importer.CustomModelManager
import com.example.engine3d.importer.ImportedModelEntry
import com.example.engine3d.importer.ModelTarget
import com.example.engine3d.importer.PlayerConfig
import com.example.engine3d.math.Vec3
import com.example.engine3d.npc.NpcManager
import com.example.engine3d.physics.BarrierManager
import com.example.engine3d.physics.BarrierType
import com.example.engine3d.physics.PhysicsEngine
import com.example.engine3d.physics.WorldBarrier
import com.example.engine3d.renderer.EngineSettings
import com.example.engine3d.terrain.TerrainMesh

enum class CentralStudioTab(val title: String, val icon: ImageVector) {
    MODELS("Model GLB", Icons.Default.FolderOpen),
    ANIMATIONS("Animasi", Icons.Default.PlayArrow),
    PHYSICS("Ukuran & Fisika", Icons.Default.Tune),
    MAP_RADAR("Map Radar", Icons.Default.Place)
}

enum class CharacterAnimSlot(val key: String, val title: String, val icon: String, val description: String) {
    IDLE("idle", "Berdiri Diam (IDLE)", "🧍", "Saat pemain diam tanpa bergerak"),
    WALK("walk", "Berjalan (WALK)", "🚶", "Saat joystick digerakkan pelan"),
    RUN("run", "Berlari Cepat (RUN)", "🏃", "Saat tombol sprint aktif / lari cepat"),
    JUMP("jump", "Melompat (JUMP)", "🦘", "Saat tombol lompat ditekan / di udara"),
    SLASH("slash", "Menyerang (SLASH / ATTACK)", "⚔️", "Saat tombol serang ditekan")
}

enum class PatcherTool {
    SET_SPAWN,
    ADD_SOLID_BLOCK,
    ADD_PORTAL
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlbConfigPatcherSheet(
    customModelManager: CustomModelManager,
    terrainMesh: TerrainMesh,
    barrierManager: BarrierManager,
    npcManager: NpcManager,
    interactionSystem: InteractionSystem,
    playerPos: Vec3,
    settings: EngineSettings,
    physicsEngine: PhysicsEngine,
    actionManager: ActionManager,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(CentralStudioTab.MODELS) }
    var selectedModel by remember {
        mutableStateOf<ImportedModelEntry?>(
            customModelManager.importedModels.firstOrNull {
                it.fileName.equals(customModelManager.activeCharacterFileName, ignoreCase = true)
            } ?: customModelManager.importedModels.firstOrNull()
        )
    }

    LaunchedEffect(customModelManager.importedModels) {
        if (selectedModel == null && customModelManager.importedModels.isNotEmpty()) {
            val activeChar = customModelManager.importedModels.firstOrNull {
                it.fileName.equals(customModelManager.activeCharacterFileName, ignoreCase = true)
            }
            selectedModel = activeChar ?: customModelManager.importedModels.first()
        }
    }

    // Animation binding states
    var idleBind by remember { mutableStateOf("") }
    var walkBind by remember { mutableStateOf("") }
    var runBind by remember { mutableStateOf("") }
    var jumpBind by remember { mutableStateOf("") }
    var slashBind by remember { mutableStateOf("") }
    var manualModelTypeOverride by remember { mutableStateOf<ModelTarget?>(null) }

    // Model configuration offset states (Anti-Jalangkung / Melayang)
    var charScale by remember { mutableStateOf(1.2f) }
    var rotationOffset by remember { mutableStateOf(0f) }
    var heightOffset by remember { mutableStateOf(0f) }
    var collisionRadius by remember { mutableStateOf(0.6f) }
    var collisionHeight by remember { mutableStateOf(1.8f) }
    var walkSpeed by remember { mutableStateOf(6.5f) }
    var runMultiplier by remember { mutableStateOf(1.6f) }
    var jumpImpulse by remember { mutableStateOf(11.5f) }

    var clipForQuickAssign by remember { mutableStateOf<String?>(null) }

    fun cleanClipDisplayName(raw: String): String {
        val clean = raw.substringAfterLast("|").substringAfterLast(":")
        return clean.ifEmpty { raw }
    }

    fun runAutoDetect(clips: List<String>) {
        if (clips.isEmpty()) return
        fun clean(str: String) = str.substringAfterLast("|").substringAfterLast(":").lowercase().replace("_", "").replace("-", "")

        val detectedIdle = clips.firstOrNull {
            val c = clean(it)
            c.contains("idle") || c.contains("stand") || c.contains("breath") || c.contains("stay") || c.contains("wait") || c.contains("loop")
        } ?: clips.firstOrNull()

        val detectedWalk = clips.firstOrNull {
            val c = clean(it)
            c.contains("walk") || c.contains("move") || c.contains("jalan") || c.contains("step") || c.contains("stride") || c.contains("forward")
        } ?: clips.firstOrNull { clean(it).contains("run") } ?: detectedIdle

        val detectedRun = clips.firstOrNull {
            val c = clean(it)
            c.contains("run") || c.contains("sprint") || c.contains("lari") || c.contains("dash") || c.contains("fast") || c.contains("jog")
        } ?: detectedWalk

        val detectedJump = clips.firstOrNull {
            val c = clean(it)
            c.contains("jump") || c.contains("leap") || c.contains("lompat") || c.contains("air") || c.contains("fall")
        } ?: detectedIdle

        val detectedSlash = clips.firstOrNull {
            val c = clean(it)
            c.contains("slash") || c.contains("attack") || c.contains("serang") || c.contains("hit") || c.contains("strike") || c.contains("punch") || c.contains("sword") || c.contains("combo")
        } ?: detectedIdle

        if (detectedIdle != null) idleBind = detectedIdle
        if (detectedWalk != null) walkBind = detectedWalk
        if (detectedRun != null) runBind = detectedRun
        if (detectedJump != null) jumpBind = detectedJump
        if (detectedSlash != null) slashBind = detectedSlash
    }

    // Update bindings when selected model changes
    LaunchedEffect(selectedModel) {
        selectedModel?.let { model ->
            manualModelTypeOverride = model.target
            val clips = model.mesh.animationClips.map { it.name }
            val currentPConfig = customModelManager.playerConfig
            val isCurrentModel = currentPConfig.modelFile.equals(model.fileName, ignoreCase = true)
            val hasValidBoundClips = isCurrentModel &&
                currentPConfig.animIdleName.isNotBlank() &&
                !currentPConfig.animIdleName.startsWith("anim_") &&
                clips.any { it.equals(currentPConfig.animIdleName, ignoreCase = true) }

            if (isCurrentModel) {
                charScale = currentPConfig.scaleX
                rotationOffset = currentPConfig.rotationOffsetYDeg
                heightOffset = currentPConfig.heightOffset
                collisionRadius = currentPConfig.collisionRadius
                collisionHeight = currentPConfig.collisionHeight
                walkSpeed = currentPConfig.walkSpeed
                runMultiplier = currentPConfig.runMultiplier
                jumpImpulse = currentPConfig.jumpImpulse
            } else {
                val rawH = model.mesh.aabb.max.y - model.mesh.aabb.min.y
                val autoScale = if (model.fileName.equals("farmer_harvest_moon.glb", ignoreCase = true)) {
                    1.0f
                } else if (rawH > 0.05f) {
                    (1.8f / rawH).coerceIn(0.01f, 10.0f)
                } else {
                    1.0f
                }
                charScale = autoScale
                rotationOffset = 0f
                heightOffset = if (rawH > 0.05f) (-model.mesh.aabb.min.y * autoScale).coerceIn(-10f, 10f) else 0f
                collisionRadius = 0.6f
                collisionHeight = 1.8f
                walkSpeed = 6.5f
                runMultiplier = 1.6f
                jumpImpulse = 11.5f
            }

            if (hasValidBoundClips) {
                idleBind = currentPConfig.animIdleName
                walkBind = currentPConfig.animWalkName
                runBind = currentPConfig.animRunName
                jumpBind = currentPConfig.animJumpName
                slashBind = currentPConfig.animSlashName
            } else if (clips.isNotEmpty()) {
                runAutoDetect(clips)
            }
        }
    }

    // Map patcher states
    var activeTool by remember { mutableStateOf(PatcherTool.SET_SPAWN) }
    var customPortalTargetGbl by remember { mutableStateOf("interior.glb") }
    var scaleMultiplier by remember { mutableStateOf(6f) }
    val worldMin = -120f
    val worldMax = 120f
    val worldSize = worldMax - worldMin
    var mapUpdateTrigger by remember { mutableStateOf(0) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xF80A0E17)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                // Top Header (Fixed at top)
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
                                .size(34.dp)
                                .background(Color(0x3300E5FF), CircleShape)
                                .border(1.dp, Color(0xFF00E5FF), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Build,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Central Studio Hub",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Pusat Konfigurasi GLB, Animasi Gerak, Fisika & Map",
                                fontSize = 10.sp,
                                color = Color(0xFF76FF03)
                            )
                        }
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("✖ Tutup", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Active Model Indicator Bar (Always visible)
                val currentModel = selectedModel
                Surface(
                    color = Color(0xFF101726),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF1E2B45)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("📦 Model Aktif:", fontSize = 11.sp, color = Color(0xFF90A4AE))
                            Text(
                                text = currentModel?.fileName ?: "(Belum ada model dipilih)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (currentModel != null) Color(0xFF00E5FF) else Color(0xFFFFB74D)
                            )
                        }

                        if (currentModel != null) {
                            val isChar = (manualModelTypeOverride ?: currentModel.target) == ModelTarget.CHARACTER
                            Box(
                                modifier = Modifier
                                    .background(if (isChar) Color(0xFF1565C0) else Color(0xFF2E7D32), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isChar) "🧍 KARAKTER" else "🗺️ TERRAIN",
                                    fontSize = 9.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Studio Tab Navigation with ScrollableTabRow to prevent title squeeze/clip
                ScrollableTabRow(
                    selectedTabIndex = activeTab.ordinal,
                    containerColor = Color(0xFF101827),
                    contentColor = Color(0xFF00E5FF),
                    edgePadding = 10.dp,
                    indicator = { tabPositions ->
                        if (activeTab.ordinal < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[activeTab.ordinal]),
                                color = Color(0xFF00E5FF)
                            )
                        }
                    },
                    divider = {}
                ) {
                    CentralStudioTab.values().forEach { tab ->
                        val isSelected = activeTab == tab
                        Tab(
                            selected = isSelected,
                            onClick = { activeTab = tab },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        tab.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp),
                                        tint = if (isSelected) Color(0xFF00E5FF) else Color(0xFF90A4AE)
                                    )
                                    Text(
                                        tab.title,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color(0xFF00E5FF) else Color(0xFF90A4AE),
                                        maxLines = 1
                                    )
                                }
                            }
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Scrollable Content per Tab (Each tab has its own localized, tidy scroll)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (activeTab) {
                        CentralStudioTab.MODELS -> {
                            // TAB 1: MODEL GLB SELECTION & OVERWRITE
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF131E33)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "📦 Pilih File GLB Untuk Dikonfigurasi",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF00E5FF)
                                    )

                                    if (customModelManager.importedModels.isEmpty()) {
                                        Text(
                                            text = "⚠️ Belum ada file GLB kustom yang diimpor. Buka 'Pusat Impor & Kustomisasi Aset' di lobby untuk memuat file GLB Anda.",
                                            color = Color(0xFFFFB74D),
                                            fontSize = 11.sp
                                        )
                                    } else {
                                        // Quick Button for Default Farmer Harvest Moon
                                        val defaultFarmer = customModelManager.importedModels.firstOrNull {
                                            it.fileName.equals("farmer_harvest_moon.glb", ignoreCase = true)
                                        }
                                        if (defaultFarmer != null) {
                                            Button(
                                                onClick = {
                                                    selectedModel = defaultFarmer
                                                    customModelManager.setActiveCharacter(defaultFarmer, updatePlayerConfig = true)
                                                    customModelManager.activeCustomCharacterMesh = defaultFarmer.mesh
                                                    Toast.makeText(context, "👨‍🌾 Karakter Farmer Harvest Moon diaktifkan!", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF76FF03)),
                                                modifier = Modifier.fillMaxWidth().height(36.dp)
                                            ) {
                                                Text(
                                                    "👨‍🌾 Aktifkan Karakter Default: Farmer Harvest Moon",
                                                    color = Color.Black,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }

                                        // Structured Vertical Model List with bounded scroll (Never clipped or pushed off screen)
                                        Text(
                                            text = "Daftar Model Terdeteksi (${customModelManager.importedModels.size} file):",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF00E5FF)
                                        )

                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 220.dp)
                                                .verticalScroll(rememberScrollState()),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            customModelManager.importedModels.forEach { model ->
                                                val isSelected = selectedModel == model
                                                val isChar = customModelManager.activeCharacterFileName.equals(model.fileName, ignoreCase = true)
                                                val isTerrain = customModelManager.activeTerrainFileName.equals(model.fileName, ignoreCase = true)

                                                Surface(
                                                    onClick = {
                                                        if (model.mesh.textureBitmap == null && model.format == "GLB") {
                                                            customModelManager.reloadModel(model.fileName)
                                                            selectedModel = customModelManager.importedModels.firstOrNull { it.fileName == model.fileName } ?: model
                                                        } else {
                                                            selectedModel = model
                                                        }
                                                    },
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (isSelected) Color(0xFF162A45) else Color(0xFF0F1726),
                                                    border = BorderStroke(
                                                        1.2.dp,
                                                        if (isSelected) Color(0xFF00E5FF) else Color(0xFF1E2B45)
                                                    ),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                            modifier = Modifier.weight(1f)
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(28.dp)
                                                                    .clip(RoundedCornerShape(6.dp))
                                                                    .background(if (isSelected) Color(0xFF00E5FF) else Color(0xFF1E2B45)),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Text(
                                                                    text = if (isChar) "👑" else if (isTerrain) "🗺️" else "📦",
                                                                    fontSize = 13.sp
                                                                )
                                                            }

                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Text(
                                                                    text = model.fileName,
                                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                                    fontSize = 12.sp,
                                                                    color = if (isSelected) Color(0xFF00E5FF) else Color.White,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                                val texTag = if (model.mesh.textureBitmap != null) " · 🎨 ${model.mesh.textureBitmap!!.width}px" else ""
                                                                Text(
                                                                    text = "${model.format.uppercase()} · ${model.mesh.vertexCount} vtx$texTag",
                                                                    fontSize = 10.sp,
                                                                    color = if (model.mesh.textureBitmap != null) Color(0xFF76FF03) else Color(0xFF90A4AE)
                                                                )
                                                            }
                                                        }

                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                        ) {
                                                            if (isChar) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .background(Color(0x3300E5FF), RoundedCornerShape(4.dp))
                                                                        .border(1.dp, Color(0xFF00E5FF), RoundedCornerShape(4.dp))
                                                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                                                ) {
                                                                    Text("HERO", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF))
                                                                }
                                                            }
                                                            if (isTerrain) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .background(Color(0x3376FF03), RoundedCornerShape(4.dp))
                                                                        .border(1.dp, Color(0xFF76FF03), RoundedCornerShape(4.dp))
                                                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                                                ) {
                                                                    Text("MAP", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF76FF03))
                                                                }
                                                            }
                                                            RadioButton(
                                                                selected = isSelected,
                                                                onClick = { selectedModel = model },
                                                                colors = RadioButtonDefaults.colors(
                                                                    selectedColor = Color(0xFF00E5FF),
                                                                    unselectedColor = Color(0xFF90A4AE)
                                                                ),
                                                                modifier = Modifier.size(24.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Active Model Card & Role Selector
                            selectedModel?.let { model ->
                                val isMapModel = (manualModelTypeOverride ?: model.target) == ModelTarget.TERRAIN
                                val isPrimaryChar = customModelManager.activeCharacterFileName.equals(model.fileName, ignoreCase = true)
                                val isPrimaryTerrain = customModelManager.activeTerrainFileName.equals(model.fileName, ignoreCase = true)

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF101726)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = "🔍 Rincian Model: ${model.fileName}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color.White
                                        )

                                        val texInfo = if (model.mesh.textureBitmap != null) {
                                            "🎨 Tekstur: ${model.mesh.textureBitmap!!.width}x${model.mesh.textureBitmap!!.height} px (Aktif)"
                                        } else if (model.mesh.texCoords != null) {
                                            "🎨 UV Map Siap"
                                        } else {
                                            "🎨 Warna: Vertex Color / PBR"
                                        }

                                        Text(
                                            text = "Format: ${model.format} • Vertex: ${model.mesh.vertexCount} • $texInfo",
                                            fontSize = 11.sp,
                                            color = if (model.mesh.textureBitmap != null) Color(0xFF76FF03) else Color(0xFF90A4AE)
                                        )

                                        Text(
                                            text = "Tentukan Peran Model Ini:",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            listOf(
                                                ModelTarget.CHARACTER to "🧍 Karakter / Hero",
                                                ModelTarget.TERRAIN to "🗺️ Permukaan / Terrain"
                                            ).forEach { (targetType, label) ->
                                                val isCurrent = (manualModelTypeOverride ?: model.target) == targetType
                                                FilterChip(
                                                    selected = isCurrent,
                                                    onClick = {
                                                        manualModelTypeOverride = targetType
                                                        if (targetType == ModelTarget.TERRAIN) {
                                                            if (model.mesh.textureBitmap == null && model.format == "GLB") {
                                                                customModelManager.reloadModel(model.fileName)
                                                            }
                                                            val activeEntry = customModelManager.importedModels.firstOrNull { it.fileName == model.fileName } ?: model
                                                            customModelManager.setActiveTerrain(activeEntry, terrainMesh)
                                                        } else {
                                                            customModelManager.setActiveCharacter(model, updatePlayerConfig = true)
                                                        }
                                                    },
                                                    label = { Text(label, fontSize = 11.sp) },
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = if (targetType == ModelTarget.TERRAIN) Color(0xFF76FF03) else Color(0xFF00E5FF),
                                                        selectedLabelColor = Color.Black,
                                                        containerColor = Color(0xFF1E2B47),
                                                        labelColor = Color.White
                                                    ),
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }

                                        HorizontalDivider(color = Color(0xFF1E2B47), modifier = Modifier.padding(vertical = 4.dp))

                                        Text(
                                            text = "⚡ Kontrol Jadikan Aset Utama (Overwrite):",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFFD600)
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    customModelManager.setActiveCharacter(model, updatePlayerConfig = true)
                                                    customModelManager.activeCustomCharacterMesh = model.mesh
                                                    Toast.makeText(context, "👑 Model '${model.fileName}' aktif sebagai Hero Utama!", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isPrimaryChar) Color(0xFF00E5FF) else Color(0xFF1E2B47)
                                                ),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = if (isPrimaryChar) "👑 Hero Utama (Aktif)" else "⭐ Overwrite Hero",
                                                    color = if (isPrimaryChar) Color.Black else Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            Button(
                                                onClick = {
                                                    if (model.mesh.textureBitmap == null && model.format == "GLB") {
                                                        customModelManager.reloadModel(model.fileName)
                                                    }
                                                    val activeEntry = customModelManager.importedModels.firstOrNull { it.fileName == model.fileName } ?: model
                                                    customModelManager.setActiveTerrain(activeEntry, terrainMesh)
                                                    terrainMesh.setCustomMesh(activeEntry.mesh)
                                                    Toast.makeText(context, "🗺️ Model '${model.fileName}' aktif sebagai Map Utama!", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isPrimaryTerrain) Color(0xFF76FF03) else Color(0xFF1E2B47)
                                                ),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = if (isPrimaryTerrain) "🗺️ Map Utama (Aktif)" else "⭐ Overwrite Map",
                                                    color = if (isPrimaryTerrain) Color.Black else Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        CentralStudioTab.ANIMATIONS -> {
                            // TAB 2: ANIMATION BINDING
                            val model = selectedModel
                            if (model == null) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131E33)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "Pilih file GLB terlebih dahulu pada tab 'Model GLB'.",
                                        modifier = Modifier.padding(16.dp),
                                        color = Color(0xFFFFB74D),
                                        fontSize = 11.sp
                                    )
                                }
                            } else {
                                val clips = model.mesh.animationClips.map { it.name }

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF101726)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = "🏃 Pemetaan & Penyambungan Animasi Karakter",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color(0xFF76FF03)
                                        )
                                        Text(
                                            text = "Model: ${model.fileName} (${clips.size} klip animasi ditemukan)",
                                            fontSize = 10.sp,
                                            color = Color(0xFFB0BEC5)
                                        )

                                        // Preset Buttons in a Clean Horizontal Scroll Row
                                        Text(
                                            text = "Preset Cepat:",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFFD600)
                                        )

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    runAutoDetect(clips)
                                                    Toast.makeText(context, "⚡ Deteksi otomatis klip berhasil!", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                modifier = Modifier.height(30.dp)
                                            ) {
                                                Text("⚡ Auto-Match", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }

                                            Button(
                                                onClick = {
                                                    idleBind = clips.firstOrNull { it.contains("idle", ignoreCase = true) } ?: clips.firstOrNull() ?: ""
                                                    walkBind = clips.firstOrNull { it.contains("walk", ignoreCase = true) } ?: clips.firstOrNull() ?: ""
                                                    runBind = clips.firstOrNull { it.contains("run", ignoreCase = true) || it.contains("sprint", ignoreCase = true) } ?: walkBind
                                                    jumpBind = clips.firstOrNull { it.contains("jump", ignoreCase = true) || it.contains("leap", ignoreCase = true) } ?: idleBind
                                                    slashBind = clips.firstOrNull { it.contains("attack", ignoreCase = true) || it.contains("slash", ignoreCase = true) || it.contains("hit", ignoreCase = true) } ?: idleBind
                                                    Toast.makeText(context, "Preset Mixamo diterapkan!", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                modifier = Modifier.height(30.dp)
                                            ) {
                                                Text("🏃 Mixamo Style", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }

                                            Button(
                                                onClick = {
                                                    idleBind = clips.firstOrNull { it == "Idle" || it == "IDLE" || it.contains("idle") } ?: ""
                                                    walkBind = clips.firstOrNull { it == "Walk" || it == "WALK" || it.contains("walk") } ?: ""
                                                    runBind = clips.firstOrNull { it == "Run" || it == "RUN" || it.contains("run") } ?: walkBind
                                                    jumpBind = clips.firstOrNull { it == "Jump" || it == "JUMP" || it.contains("jump") } ?: idleBind
                                                    slashBind = clips.firstOrNull { it == "Attack" || it == "Slash" || it.contains("attack") } ?: idleBind
                                                    Toast.makeText(context, "Preset Blender/Unity diterapkan!", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00796B)),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                modifier = Modifier.height(30.dp)
                                            ) {
                                                Text("🎨 Blender/Unity", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    idleBind = ""
                                                    walkBind = ""
                                                    runBind = ""
                                                    jumpBind = ""
                                                    slashBind = ""
                                                    clipForQuickAssign = null
                                                    Toast.makeText(context, "Binding direset", Toast.LENGTH_SHORT).show()
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                modifier = Modifier.height(30.dp)
                                            ) {
                                                Text("Reset", color = Color(0xFFFF5252), fontSize = 9.sp)
                                            }
                                        }

                                        // Horizontal Clips Selector
                                        if (clips.isNotEmpty()) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Klip Internal GLB (Ketuk untuk pasang cepat):",
                                                    fontSize = 10.sp,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "Geser →",
                                                    fontSize = 9.sp,
                                                    color = Color(0xFF00E5FF)
                                                )
                                            }

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                clips.forEach { name ->
                                                    val cleanName = cleanClipDisplayName(name)
                                                    val assignedBadge = when (name) {
                                                        idleBind -> "IDLE"
                                                        walkBind -> "WALK"
                                                        runBind -> "RUN"
                                                        jumpBind -> "JUMP"
                                                        slashBind -> "SLASH"
                                                        else -> null
                                                    }

                                                    Surface(
                                                        onClick = {
                                                            clipForQuickAssign = if (clipForQuickAssign == name) null else name
                                                        },
                                                        color = if (assignedBadge != null) Color(0xFF1B5E20) else if (clipForQuickAssign == name) Color(0xFF006064) else Color(0xFF1E2B47),
                                                        border = BorderStroke(
                                                            1.dp,
                                                            if (clipForQuickAssign == name) Color(0xFF00E5FF) else if (assignedBadge != null) Color(0xFF76FF03) else Color(0xFF37474F)
                                                        ),
                                                        shape = RoundedCornerShape(6.dp)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                        ) {
                                                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = if (assignedBadge != null) Color(0xFF76FF03) else Color(0xFF00E5FF), modifier = Modifier.size(12.dp))
                                                            Text(cleanName, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                            if (assignedBadge != null) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .background(Color(0xFF76FF03), RoundedCornerShape(3.dp))
                                                                        .padding(horizontal = 3.dp, vertical = 1.dp)
                                                                ) {
                                                                    Text(assignedBadge, fontSize = 8.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            // Quick Assign Bar
                                            if (clipForQuickAssign != null) {
                                                val qClip = clipForQuickAssign!!
                                                val qClean = cleanClipDisplayName(qClip)
                                                Surface(
                                                    color = Color(0xFF162A45),
                                                    shape = RoundedCornerShape(6.dp),
                                                    border = BorderStroke(1.dp, Color(0xFF00E5FF)),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text("⚡ Pasang '$qClean' ke Slot:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF))
                                                            IconButton(onClick = { clipForQuickAssign = null }, modifier = Modifier.size(20.dp)) {
                                                                Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                                            }
                                                        }

                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .horizontalScroll(rememberScrollState()),
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            CharacterAnimSlot.values().forEach { slot ->
                                                                val isSlotSet = when (slot) {
                                                                    CharacterAnimSlot.IDLE -> idleBind == qClip
                                                                    CharacterAnimSlot.WALK -> walkBind == qClip
                                                                    CharacterAnimSlot.RUN -> runBind == qClip
                                                                    CharacterAnimSlot.JUMP -> jumpBind == qClip
                                                                    CharacterAnimSlot.SLASH -> slashBind == qClip
                                                                }
                                                                Button(
                                                                    onClick = {
                                                                        when (slot) {
                                                                            CharacterAnimSlot.IDLE -> idleBind = qClip
                                                                            CharacterAnimSlot.WALK -> walkBind = qClip
                                                                            CharacterAnimSlot.RUN -> runBind = qClip
                                                                            CharacterAnimSlot.JUMP -> jumpBind == qClip
                                                                            CharacterAnimSlot.SLASH -> slashBind = qClip
                                                                        }
                                                                        clipForQuickAssign = null
                                                                        Toast.makeText(context, "✓ Dipasang ke ${slot.title}", Toast.LENGTH_SHORT).show()
                                                                    },
                                                                    colors = ButtonDefaults.buttonColors(
                                                                        containerColor = if (isSlotSet) Color(0xFF2E7D32) else Color(0xFF00E5FF)
                                                                    ),
                                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                                    modifier = Modifier.height(26.dp)
                                                                ) {
                                                                    Text("${slot.icon} ${slot.key.uppercase()}", color = if (isSlotSet) Color.White else Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(Modifier.height(4.dp))

                                        // Detailed Bind Cards
                                        Text(
                                            text = "Slot Status Gerakan Karakter:",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )

                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            AnimationBindCard(
                                                slotTitle = "Berdiri Diam (IDLE)",
                                                slotIcon = "🧍",
                                                slotDescription = "Saat pemain diam tanpa input",
                                                currentValue = idleBind,
                                                availableClips = clips,
                                                cleanClipDisplayName = ::cleanClipDisplayName,
                                                onSelectClip = { idleBind = it },
                                                onClearClick = { idleBind = "" }
                                            )
                                            AnimationBindCard(
                                                slotTitle = "Berjalan (WALK)",
                                                slotIcon = "🚶",
                                                slotDescription = "Saat joystick digerakkan santai",
                                                currentValue = walkBind,
                                                availableClips = clips,
                                                cleanClipDisplayName = ::cleanClipDisplayName,
                                                onSelectClip = { walkBind = it },
                                                onClearClick = { walkBind = "" }
                                            )
                                            AnimationBindCard(
                                                slotTitle = "Berlari Cepat (RUN)",
                                                slotIcon = "🏃",
                                                slotDescription = "Saat tombol sprint aktif",
                                                currentValue = runBind,
                                                availableClips = clips,
                                                cleanClipDisplayName = ::cleanClipDisplayName,
                                                onSelectClip = { runBind = it },
                                                onClearClick = { runBind = "" }
                                            )
                                            AnimationBindCard(
                                                slotTitle = "Melompat (JUMP)",
                                                slotIcon = "🦘",
                                                slotDescription = "Saat di udara / melompat",
                                                currentValue = jumpBind,
                                                availableClips = clips,
                                                cleanClipDisplayName = ::cleanClipDisplayName,
                                                onSelectClip = { jumpBind = it },
                                                onClearClick = { jumpBind = "" }
                                            )
                                            AnimationBindCard(
                                                slotTitle = "Menyerang (SLASH / ATTACK)",
                                                slotIcon = "⚔️",
                                                slotDescription = "Saat tombol serang ditekan",
                                                currentValue = slashBind,
                                                availableClips = clips,
                                                cleanClipDisplayName = ::cleanClipDisplayName,
                                                onSelectClip = { slashBind = it },
                                                onClearClick = { slashBind = "" }
                                            )
                                        }

                                        Spacer(Modifier.height(4.dp))

                                        Button(
                                            onClick = {
                                                val config = customModelManager.playerConfig.copy(
                                                    characterName = model.fileName.substringBeforeLast("."),
                                                    modelFile = model.fileName,
                                                    scaleX = charScale,
                                                    scaleY = charScale,
                                                    scaleZ = charScale,
                                                    rotationOffsetYDeg = rotationOffset,
                                                    heightOffset = heightOffset,
                                                    collisionRadius = collisionRadius,
                                                    collisionHeight = collisionHeight,
                                                    walkSpeed = walkSpeed,
                                                    runMultiplier = runMultiplier,
                                                    jumpImpulse = jumpImpulse,
                                                    animIdleName = idleBind,
                                                    animWalkName = walkBind,
                                                    animRunName = runBind,
                                                    animJumpName = jumpBind,
                                                    animSlashName = slashBind
                                                )
                                                customModelManager.savePlayerConfig(config)
                                                customModelManager.setActiveCharacter(model, updatePlayerConfig = true)
                                                customModelManager.activeCustomCharacterMesh = model.mesh
                                                Toast.makeText(context, "✓ Berhasil Menyimpan & Menghubungkan Animasi!", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF76FF03)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(44.dp)
                                                .testTag("apply_animations_patch_button")
                                        ) {
                                            Text("✓ Simpan & Hubungkan Animasi Karakter", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }

                        CentralStudioTab.PHYSICS -> {
                            // TAB 3: MODEL SCALE, HEIGHT OFFSET (ANTI-JALANGKUNG), & PHYSICS
                            val model = selectedModel
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF131E33)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text("📏", fontSize = 16.sp)
                                        Text(
                                            "Penyesuaian Skala & Posisi (Anti-Jalangkung)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color(0xFF00E5FF)
                                        )
                                    }

                                    Text(
                                        "Atur Tinggi Offset (Y) agar kaki karakter pas menempel di tanah dan tidak melayang.",
                                        fontSize = 10.sp,
                                        color = Color(0xFFB0BEC5)
                                    )

                                    // 1. Skala Model
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text("Skala / Ukuran Model 3D:", fontSize = 11.sp, color = Color.White)
                                            Text("${"%.3f".format(charScale)}x", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }

                                        Slider(
                                            value = charScale,
                                            onValueChange = { charScale = it },
                                            valueRange = 0.01f..5.0f,
                                            colors = SliderDefaults.colors(thumbColor = Color(0xFF00E5FF), activeTrackColor = Color(0xFF00E5FF))
                                        )

                                        // Quick scale chips
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            listOf(
                                                0.01f to "0.01x (FBX CM)",
                                                0.05f to "0.05x",
                                                0.1f to "0.10x",
                                                0.3f to "0.30x",
                                                0.5f to "0.50x",
                                                1.0f to "1.00x",
                                                1.5f to "1.50x",
                                                2.0f to "2.00x"
                                            ).forEach { (scaleVal, label) ->
                                                val isClose = kotlin.math.abs(charScale - scaleVal) < 0.005f
                                                FilterChip(
                                                    selected = isClose,
                                                    onClick = { charScale = scaleVal },
                                                    label = { Text(label, fontSize = 10.sp) },
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = Color(0xFF00E5FF),
                                                        selectedLabelColor = Color.Black,
                                                        containerColor = Color(0xFF1E2B47),
                                                        labelColor = Color.White
                                                    ),
                                                    modifier = Modifier.height(26.dp)
                                                )
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedButton(
                                                onClick = { charScale = (charScale - 0.05f).coerceAtLeast(0.01f) },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.height(26.dp)
                                            ) {
                                                Text("-0.05x", fontSize = 10.sp, color = Color(0xFF00E5FF))
                                            }
                                            Text(
                                                "Rentang 0.01x - 5.0x untuk model cm / FBX",
                                                fontSize = 9.sp,
                                                color = Color(0xFF90A4AE)
                                            )
                                            OutlinedButton(
                                                onClick = { charScale = (charScale + 0.05f).coerceAtMost(5.0f) },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.height(26.dp)
                                            ) {
                                                Text("+0.05x", fontSize = 10.sp, color = Color(0xFF00E5FF))
                                            }
                                        }
                                    }

                                    // 2. Tinggi Offset Y (Anti-Melayang)
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Tinggi Offset (Y) - Anti-Melayang:", fontSize = 11.sp, color = Color.White)
                                            Text("${"%.2f".format(heightOffset)} meter", color = Color(0xFF76FF03), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                        Slider(
                                            value = heightOffset,
                                            onValueChange = { heightOffset = it },
                                            valueRange = -10.0f..10.0f,
                                            colors = SliderDefaults.colors(thumbColor = Color(0xFF76FF03), activeTrackColor = Color(0xFF76FF03))
                                        )
                                    }

                                    // 3. Rotasi Offset
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Rotasi Offset Hadap Depan (Y-Axis):", fontSize = 11.sp, color = Color.White)
                                            Text("${rotationOffset.toInt()}°", color = Color(0xFFFFD600), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                        Slider(
                                            value = rotationOffset,
                                            onValueChange = { rotationOffset = it },
                                            valueRange = 0f..360f,
                                            colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD600), activeTrackColor = Color(0xFFFFD600))
                                        )
                                    }

                                    HorizontalDivider(color = Color(0xFF1E2B47), modifier = Modifier.padding(vertical = 4.dp))

                                    Text(
                                        "🛡️ Kolisi Tabrakan & Gerak Karakter:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF76FF03)
                                    )

                                    // 4. Radius Kolisi
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Radius Kolisi Tabrakan Fisika:", fontSize = 11.sp, color = Color.White)
                                            Text("${"%.2f".format(collisionRadius)}m", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                        Slider(
                                            value = collisionRadius,
                                            onValueChange = { collisionRadius = it },
                                            valueRange = 0.2f..2.0f,
                                            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White)
                                        )
                                    }

                                    // 5. Tinggi Kolisi
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Tinggi Kolisi Tabrakan Fisika:", fontSize = 11.sp, color = Color.White)
                                            Text("${"%.2f".format(collisionHeight)}m", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                        Slider(
                                            value = collisionHeight,
                                            onValueChange = { collisionHeight = it },
                                            valueRange = 0.5f..3.5f,
                                            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White)
                                        )
                                    }

                                    // 6. Kecepatan Jalan
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Kecepatan Jalan:", fontSize = 11.sp, color = Color.White)
                                            Text("${"%.1f".format(walkSpeed)} m/s", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                        Slider(
                                            value = walkSpeed,
                                            onValueChange = { walkSpeed = it },
                                            valueRange = 2.0f..15.0f,
                                            colors = SliderDefaults.colors(thumbColor = Color(0xFF00E5FF), activeTrackColor = Color(0xFF00E5FF))
                                        )
                                    }

                                    // 7. Multiplier Lari
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Multiplier Lari Sprint:", fontSize = 11.sp, color = Color.White)
                                            Text("${"%.1f".format(runMultiplier)}x", color = Color(0xFF76FF03), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                        Slider(
                                            value = runMultiplier,
                                            onValueChange = { runMultiplier = it },
                                            valueRange = 1.1f..2.5f,
                                            colors = SliderDefaults.colors(thumbColor = Color(0xFF76FF03), activeTrackColor = Color(0xFF76FF03))
                                        )
                                    }

                                    // 8. Dorongan Lompatan
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Kekuatan Dorongan Melompat:", fontSize = 11.sp, color = Color.White)
                                            Text("${"%.1f".format(jumpImpulse)}", color = Color(0xFFFFD600), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                        Slider(
                                            value = jumpImpulse,
                                            onValueChange = { jumpImpulse = it },
                                            valueRange = 5.0f..20.0f,
                                            colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD600), activeTrackColor = Color(0xFFFFD600))
                                        )
                                    }

                                    Spacer(Modifier.height(4.dp))

                                    Button(
                                        onClick = {
                                            if (model != null) {
                                                val config = customModelManager.playerConfig.copy(
                                                    characterName = model.fileName.substringBeforeLast("."),
                                                    modelFile = model.fileName,
                                                    scaleX = charScale,
                                                    scaleY = charScale,
                                                    scaleZ = charScale,
                                                    rotationOffsetYDeg = rotationOffset,
                                                    heightOffset = heightOffset,
                                                    collisionRadius = collisionRadius,
                                                    collisionHeight = collisionHeight,
                                                    walkSpeed = walkSpeed,
                                                    runMultiplier = runMultiplier,
                                                    jumpImpulse = jumpImpulse
                                                )
                                                customModelManager.savePlayerConfig(config)
                                                customModelManager.setActiveCharacter(model, updatePlayerConfig = true)
                                            }
                                            Toast.makeText(context, "✓ Berhasil Menyimpan Pengaturan Skala & Fisika!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                                        modifier = Modifier.fillMaxWidth().height(42.dp)
                                    ) {
                                        Text("✓ Simpan Skala & Fisika Karakter", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        CentralStudioTab.MAP_RADAR -> {
                            // TAB 4: MAP & RADAR ORBIT PATCHER
                            val model = selectedModel
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF101726)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "🗺️ Map Patcher Visual & Bintik Radar Orbit",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF00E5FF)
                                    )
                                    Text(
                                        text = "Ketuk radar visual di bawah untuk menempatkan titik spawn, zona pembatas tabrakan solid, atau portal pintu.",
                                        fontSize = 10.sp,
                                        color = Color(0xFFB0BEC5)
                                    )

                                    // Tool Selector
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        listOf(
                                            PatcherTool.SET_SPAWN to "📍 Set Spawn",
                                            PatcherTool.ADD_SOLID_BLOCK to "🧱 Blok Solid",
                                            PatcherTool.ADD_PORTAL to "🌀 Tambah Portal"
                                        ).forEach { (tool, label) ->
                                            Button(
                                                onClick = { activeTool = tool },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (activeTool == tool) Color(0xFF00E5FF) else Color(0xFF1E2B45)
                                                ),
                                                modifier = Modifier.weight(1f),
                                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = label,
                                                    color = if (activeTool == tool) Color.Black else Color.White,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    // Parameter per tool
                                    when (activeTool) {
                                        PatcherTool.ADD_SOLID_BLOCK -> {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Lebar Blok: ${scaleMultiplier.toInt()}m", fontSize = 11.sp, color = Color.White)
                                                Slider(
                                                    value = scaleMultiplier,
                                                    onValueChange = { scaleMultiplier = it },
                                                    valueRange = 2f..16f,
                                                    modifier = Modifier.width(160.dp)
                                                )
                                            }
                                        }
                                        PatcherTool.ADD_PORTAL -> {
                                            var portalExpanded by remember { mutableStateOf(false) }
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color(0xFF1E2B45), RoundedCornerShape(6.dp))
                                                    .border(1.dp, Color(0xFF37474F), RoundedCornerShape(6.dp))
                                                    .clickable { portalExpanded = true }
                                                    .padding(8.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = if (customPortalTargetGbl == "null") "❌ Tanpa Teleport" else "🗺️ Target: $customPortalTargetGbl",
                                                        fontSize = 11.sp,
                                                        color = Color(0xFF00E5FF)
                                                    )
                                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
                                                }

                                                DropdownMenu(
                                                    expanded = portalExpanded,
                                                    onDismissRequest = { portalExpanded = false },
                                                    modifier = Modifier.background(Color(0xFF101726))
                                                ) {
                                                    DropdownMenuItem(
                                                        text = { Text("❌ Tanpa Teleport", color = Color.White, fontSize = 11.sp) },
                                                        onClick = {
                                                            customPortalTargetGbl = "null"
                                                            portalExpanded = false
                                                        }
                                                    )
                                                    customModelManager.importedModels.forEach { m ->
                                                        DropdownMenuItem(
                                                            text = { Text("🗺️ ${m.fileName}", color = Color.White, fontSize = 11.sp) },
                                                            onClick = {
                                                                customPortalTargetGbl = m.fileName
                                                                portalExpanded = false
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        PatcherTool.SET_SPAWN -> {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Button(
                                                    onClick = {
                                                        playerPos.set(0f, terrainMesh.heightQuery.sampleSurface(0f, 0f).height + 1f, 0f)
                                                        mapUpdateTrigger++
                                                        Toast.makeText(context, "📍 Spawn ke Pusat (0, 0)", Toast.LENGTH_SHORT).show()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2B45)),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text("Spawn Pusat (0,0)", fontSize = 10.sp)
                                                }

                                                Button(
                                                    onClick = {
                                                        val maxB = 110f
                                                        playerPos.set(maxB, terrainMesh.heightQuery.sampleSurface(maxB, maxB).height + 1f, maxB)
                                                        mapUpdateTrigger++
                                                        Toast.makeText(context, "📍 Spawn ke Batas (110, 110)", Toast.LENGTH_SHORT).show()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2B45)),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text("Spawn Batas (110,110)", fontSize = 10.sp)
                                                }
                                            }
                                        }
                                    }

                                    // RADAR CANVAS (Bounded cleanly so it fits on screen without pushing anything off)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 260.dp)
                                            .aspectRatio(1f)
                                            .align(Alignment.CenterHorizontally)
                                            .background(Color(0xFF070B12), RoundedCornerShape(8.dp))
                                            .border(1.5.dp, Color(0xFF00E5FF), RoundedCornerShape(8.dp))
                                            .pointerInput(mapUpdateTrigger) {
                                                detectTapGestures { offset ->
                                                    val wx = (offset.x / size.width.toFloat()) * worldSize + worldMin
                                                    val wz = (offset.y / size.height.toFloat()) * worldSize + worldMin
                                                    val wy = terrainMesh.heightQuery.sampleSurface(wx, wz).height

                                                    when (activeTool) {
                                                        PatcherTool.SET_SPAWN -> {
                                                            playerPos.set(wx, wy + 1f, wz)
                                                            Toast.makeText(context, "📍 Titik Lahir dipindahkan ke (${String.format("%.1f", wx)}, ${String.format("%.1f", wz)})", Toast.LENGTH_SHORT).show()
                                                        }
                                                        PatcherTool.ADD_SOLID_BLOCK -> {
                                                            val bId = "patched_wall_${System.currentTimeMillis()}"
                                                            val barrier = WorldBarrier(
                                                                id = bId,
                                                                name = "Tembok Patched",
                                                                type = BarrierType.WALL_BARRIER,
                                                                position = Vec3(wx, wy + 2f, wz),
                                                                size = Vec3(scaleMultiplier, 5f, scaleMultiplier),
                                                                isPassable = false,
                                                                warningMessage = "🚫 Batas Blokir: Solid Block!"
                                                            )
                                                            barrierManager.addBarrier(barrier)
                                                            Toast.makeText(context, "🧱 Blok Solid ${scaleMultiplier.toInt()}m ditambahkan!", Toast.LENGTH_SHORT).show()
                                                        }
                                                        PatcherTool.ADD_PORTAL -> {
                                                            val portalId = "patched_portal_${System.currentTimeMillis()}"
                                                            val portal = WorldInteractable(
                                                                id = portalId,
                                                                name = "Portal Pintu ke $customPortalTargetGbl",
                                                                type = InteractableType.HOUSE_DOOR_ENTER,
                                                                position = Vec3(wx, wy, wz),
                                                                targetTeleportPos = Vec3(250.0f, 1.5f, 250.0f),
                                                                promptText = "[Buka Pintu] Masuk Ke $customPortalTargetGbl"
                                                            )
                                                            interactionSystem.interactables.add(portal)
                                                            Toast.makeText(context, "🌀 Portal ke $customPortalTargetGbl terdaftar!", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                    mapUpdateTrigger++
                                                }
                                            }
                                    ) {
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            val canvasSize = size
                                            val cx = canvasSize.width / 2f
                                            val cy = canvasSize.height / 2f

                                            val rings = listOf(0.2f, 0.4f, 0.6f, 0.8f, 0.95f)
                                            rings.forEach { r ->
                                                drawCircle(
                                                    color = Color(0x2200E5FF),
                                                    radius = cx * r,
                                                    center = Offset(cx, cy),
                                                    style = Stroke(width = 1f)
                                                )
                                            }

                                            drawLine(
                                                color = Color(0x2200E5FF),
                                                start = Offset(0f, cy),
                                                end = Offset(canvasSize.width, cy),
                                                strokeWidth = 1f
                                            )
                                            drawLine(
                                                color = Color(0x2200E5FF),
                                                start = Offset(cx, 0f),
                                                end = Offset(cx, canvasSize.height),
                                                strokeWidth = 1f
                                            )

                                            // Draw terrain sample dots
                                            val terrainVerts = terrainMesh.mesh.vertices
                                            if (terrainVerts.isNotEmpty()) {
                                                val vertSize = terrainVerts.size
                                                val stepSize = (vertSize / 3000).coerceAtLeast(3) * 3
                                                var i = 0
                                                while (i < vertSize - 2) {
                                                    val wx = terrainVerts[i]
                                                    val wz = terrainVerts[i + 2]
                                                    val offset = Offset(
                                                        x = ((wx - worldMin) / worldSize) * canvasSize.width,
                                                        y = ((wz - worldMin) / worldSize) * canvasSize.height
                                                    )
                                                    drawCircle(color = Color(0x3376FF03), radius = 1.5f, center = offset)
                                                    i += stepSize
                                                }
                                            }

                                            // Boundary
                                            val limMin = -115f
                                            val limMax = 115f
                                            val bOffsetMin = Offset(((limMin - worldMin) / worldSize) * canvasSize.width, ((limMin - worldMin) / worldSize) * canvasSize.height)
                                            val bOffsetMax = Offset(((limMax - worldMin) / worldSize) * canvasSize.width, ((limMax - worldMin) / worldSize) * canvasSize.height)
                                            drawRect(
                                                color = Color(0x88FF5252),
                                                topLeft = bOffsetMin,
                                                size = Size(bOffsetMax.x - bOffsetMin.x, bOffsetMax.y - bOffsetMin.y),
                                                style = Stroke(width = 2f)
                                            )

                                            // Solid barriers
                                            barrierManager.barriers.forEach { b ->
                                                if (b.type == BarrierType.WALL_BARRIER) {
                                                    val halfX = b.size.x * 0.5f
                                                    val halfZ = b.size.z * 0.5f
                                                    val minOffset = Offset(((b.position.x - halfX - worldMin) / worldSize) * canvasSize.width, ((b.position.z - halfZ - worldMin) / worldSize) * canvasSize.height)
                                                    val maxOffset = Offset(((b.position.x + halfX - worldMin) / worldSize) * canvasSize.width, ((b.position.z + halfZ - worldMin) / worldSize) * canvasSize.height)
                                                    drawRect(
                                                        color = if (b.id.startsWith("world_limit")) Color(0x44FF5252) else Color(0xBBFFD600),
                                                        topLeft = minOffset,
                                                        size = Size(maxOffset.x - minOffset.x, maxOffset.y - minOffset.y)
                                                    )
                                                }
                                            }

                                            // Portals
                                            interactionSystem.interactables.forEach { item ->
                                                val pOffset = Offset(((item.position.x - worldMin) / worldSize) * canvasSize.width, ((item.position.z - worldMin) / worldSize) * canvasSize.height)
                                                val isPortal = item.targetTeleportPos != null ||
                                                    item.type == InteractableType.QUANTUM_PORTAL ||
                                                    item.type == InteractableType.HOUSE_DOOR_ENTER ||
                                                    item.type == InteractableType.HOUSE_DOOR_EXIT

                                                if (isPortal) {
                                                    drawCircle(color = Color(0x66E040FB), radius = 10f, center = pOffset)
                                                    drawCircle(color = Color(0xFF00E5FF), radius = 6f, center = pOffset, style = Stroke(2f))
                                                } else {
                                                    drawCircle(color = Color(0xFFFFD600), radius = 4f, center = pOffset)
                                                }
                                            }

                                            // Player start position
                                            val pOffset = Offset(((playerPos.x - worldMin) / worldSize) * canvasSize.width, ((playerPos.z - worldMin) / worldSize) * canvasSize.height)
                                            drawCircle(color = Color(0xFF76FF03), radius = 8f, center = pOffset)
                                            drawCircle(color = Color.Black, radius = 3f, center = pOffset)
                                        }
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Button(
                                            onClick = {
                                                barrierManager.barriers.removeAll { !it.id.startsWith("world_limit") }
                                                interactionSystem.interactables.removeAll { !it.id.startsWith("portal_") && !it.id.startsWith("house_") }
                                                mapUpdateTrigger++
                                                Toast.makeText(context, "🧹 Objek & rintangan bukit bawaan dibersihkan!", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("🧹 Bersihkan Objek Bawaan", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = {
                                                if (model != null) {
                                                    if (model.mesh.textureBitmap == null && model.format == "GLB") {
                                                        customModelManager.reloadModel(model.fileName)
                                                    }
                                                    val activeEntry = customModelManager.importedModels.firstOrNull { it.fileName == model.fileName } ?: model
                                                    customModelManager.setActiveTerrain(activeEntry, terrainMesh)
                                                    terrainMesh.setCustomMesh(activeEntry.mesh)
                                                }
                                                Toast.makeText(context, "✓ Sukses Mempatch & Menyimpan Config Map!", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                                            modifier = Modifier.weight(1.2f).testTag("save_map_patch_button")
                                        ) {
                                            Text("Patch & Simpan Map", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnimationBindCard(
    slotTitle: String,
    slotIcon: String,
    slotDescription: String,
    currentValue: String,
    availableClips: List<String>,
    cleanClipDisplayName: (String) -> String,
    onSelectClip: (String) -> Unit,
    onClearClick: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var customTextInput by remember { mutableStateOf("") }
    var showCustomInput by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF162035)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (currentValue.isNotEmpty()) Color(0xFF00E5FF).copy(alpha = 0.6f) else Color(0xFF263238),
                RoundedCornerShape(8.dp)
            )
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(slotIcon, fontSize = 18.sp)
                    Column {
                        Text(
                            text = slotTitle,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (currentValue.isNotEmpty()) {
                            val clean = cleanClipDisplayName(currentValue)
                            Text(
                                text = "✓ $clean",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF76FF03)
                            )
                        } else {
                            Text(
                                text = "(Belum dipilih - Animasi Prosedural)",
                                fontSize = 10.sp,
                                color = Color(0xFFFFB74D)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (currentValue.isNotEmpty()) {
                        IconButton(
                            onClick = onClearClick,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Hapus binding",
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Button(
                        onClick = { isExpanded = !isExpanded },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isExpanded) Color(0xFF006064) else if (currentValue.isEmpty()) Color(0xFF00E5FF) else Color(0xFF1E2B45)
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text(
                            text = if (isExpanded) "Tutup ▴" else if (currentValue.isEmpty()) "Pilih Klip ▾" else "Ganti ▾",
                            color = if (!isExpanded && currentValue.isEmpty()) Color.Black else Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Inline Expansion Drawer
            if (isExpanded) {
                HorizontalDivider(color = Color(0xFF263238))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Daftar Klip Model (Ketuk klip untuk memilih langsung):",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E5FF)
                    )

                    if (availableClips.isEmpty()) {
                        Text(
                            text = "Tidak ada klip terdeteksi di file GLB ini.",
                            fontSize = 10.sp,
                            color = Color(0xFF90A4AE)
                        )
                    } else {
                        availableClips.forEach { clip ->
                            val cleanName = cleanClipDisplayName(clip)
                            val isSelected = currentValue == clip
                            Surface(
                                onClick = {
                                    onSelectClip(clip)
                                    isExpanded = false
                                },
                                color = if (isSelected) Color(0xFF1B5E20) else Color(0xFF0D1826),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) Color(0xFF76FF03) else Color(0xFF263238)
                                ),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = if (isSelected) Color(0xFF76FF03) else Color(0xFF00E5FF),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = cleanName,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color(0xFF76FF03) else Color.White
                                        )
                                    }
                                    if (isSelected) {
                                        Text("✓ Terpilih", color = Color(0xFF76FF03), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    } else {
                                        Text("Pilih", color = Color(0xFF00E5FF), fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }

                    Surface(
                        onClick = {
                            onClearClick()
                            isExpanded = false
                        },
                        color = Color(0xFF261214),
                        border = BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("❌", fontSize = 11.sp)
                            Text("Kosongkan / Nonaktifkan Animasi Slot Ini", color = Color(0xFFFF8A80), fontSize = 10.sp)
                        }
                    }

                    if (!showCustomInput) {
                        TextButton(
                            onClick = { showCustomInput = true },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("✏️ Ketik nama klip manual...", fontSize = 10.sp, color = Color(0xFF80D8FF))
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedTextField(
                                value = customTextInput,
                                onValueChange = { customTextInput = it },
                                label = { Text("Nama Klip Kustom", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Button(
                                onClick = {
                                    if (customTextInput.isNotBlank()) {
                                        onSelectClip(customTextInput.trim())
                                        isExpanded = false
                                        showCustomInput = false
                                        customTextInput = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                            ) {
                                Text("Terapkan", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
