package com.example.engine3d.ui

import android.net.Uri
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
    val scrollState = rememberScrollState()

    var selectedModel by remember { mutableStateOf<ImportedModelEntry?>(null) }
    
    // Automatically select first model if available
    LaunchedEffect(customModelManager.importedModels) {
        if (selectedModel == null && customModelManager.importedModels.isNotEmpty()) {
            selectedModel = customModelManager.importedModels.first()
        }
    }

    // Animation binding states
    var idleBind by remember { mutableStateOf("") }
    var walkBind by remember { mutableStateOf("") }
    var runBind by remember { mutableStateOf("") }
    var jumpBind by remember { mutableStateOf("") }
    var slashBind by remember { mutableStateOf("") }
    var manualModelTypeOverride by remember { mutableStateOf<ModelTarget?>(null) }

    // Model configuration offset states (Gaya Jalangkung Anti-Tenggelam / Melayang)
    var charScale by remember { mutableStateOf(1.2f) }
    var rotationOffset by remember { mutableStateOf(0f) }
    var heightOffset by remember { mutableStateOf(0f) }
    var collisionRadius by remember { mutableStateOf(0.6f) }
    var collisionHeight by remember { mutableStateOf(1.8f) }
    var walkSpeed by remember { mutableStateOf(6.5f) }
    var runMultiplier by remember { mutableStateOf(1.6f) }
    var jumpImpulse by remember { mutableStateOf(11.5f) }

    var activePickingSlot by remember { mutableStateOf<CharacterAnimSlot?>(null) }
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
                charScale = 1.0f
                rotationOffset = 0f
                heightOffset = 0f
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
    var scaleMultiplier by remember { mutableStateOf(6f) } // width/depth of placed blocks
    
    // Canvas dimensions mapping
    val worldMin = -120f
    val worldMax = 120f
    val worldSize = worldMax - worldMin

    // Forces canvas recomposition when elements are added
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
                    .padding(horizontal = 18.dp, vertical = 12.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Top header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Build, contentDescription = null, tint = Color(0xFF00E5FF))
                        Column {
                            Text(
                                text = "Pusat Hub Pintar GLB & Patcher Konfigurasi",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Autodeteksi isi file GLB, petakan animasi secara visual, serta rancang collision map & portal langsung.",
                                fontSize = 10.sp,
                                color = Color(0xFF76FF03)
                            )
                        }
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                    ) {
                        Text("✖ Tutup", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // 1. CHOOSE ACTIVE GLB MODEL
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131E33)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.border(1.dp, Color(0xFF00E5FF), RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "📦 Pilih File GLB Untuk Dikonfigurasi / Dipatch",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF00E5FF)
                        )

                        if (customModelManager.importedModels.isEmpty()) {
                            Text(
                                text = "⚠️ Belum ada file GLB kustom yang diimpor. Silakan buka 'Pusat Impor & Kustomisasi Aset' di lobby terlebih dahulu untuk memuat file GLB Anda.",
                                color = Color(0xFFFFB74D),
                                fontSize = 11.sp
                            )
                        } else {
                            // Quick Button for Default Farmer Harvest Moon GLB
                            val defaultFarmerEntry = customModelManager.importedModels.firstOrNull { it.fileName.equals("farmer_harvest_moon.glb", ignoreCase = true) }
                            if (defaultFarmerEntry != null) {
                                Button(
                                    onClick = {
                                        selectedModel = defaultFarmerEntry
                                        customModelManager.setActiveCharacter(defaultFarmerEntry, updatePlayerConfig = true)
                                        customModelManager.activeCustomCharacterMesh = defaultFarmerEntry.mesh
                                        Toast.makeText(context, "👨‍🌾 Karakter Farmer Harvest Moon diaktifkan!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF76FF03)),
                                    modifier = Modifier.fillMaxWidth().height(36.dp)
                                ) {
                                    Text("👨‍🌾 Aktifkan Karakter Default: Farmer Harvest Moon", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }

                            // Model selection chips
                            Text(text = "Daftar GLB Terdeteksi:", fontSize = 11.sp, color = Color(0xFF90A4AE))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                customModelManager.importedModels.forEach { model ->
                                    val isSelected = selectedModel == model
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedModel = model },
                                        label = { Text(model.fileName, fontSize = 10.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF76FF03),
                                            selectedLabelColor = Color.Black,
                                            containerColor = Color(0xFF1E2B47),
                                            labelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. MODEL TYPE ANALYSIS & OPTIONS PANEL
                selectedModel?.let { model ->
                    val hasAnimations = model.mesh.animationClips.isNotEmpty()
                    val isMapModel = (manualModelTypeOverride ?: model.target) == ModelTarget.TERRAIN

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF101726)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Autodetected results
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🔍 Hasil Analisis Otomatis File GLB",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                                Box(
                                    modifier = Modifier
                                        .background(if (isMapModel) Color(0xFF2E7D32) else Color(0xFF1565C0), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = if (isMapModel) "🗺️ MODEL TERRAIN/MAP" else "🧍 MODEL KARAKTER / HERO",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Text(
                                text = "Nama File: ${model.fileName} • Format: ${model.format} • Vertex Count: ${model.mesh.vertexCount} • Anim Clips: ${model.mesh.animationClips.size}",
                                fontSize = 11.sp,
                                color = Color(0xFF90A4AE)
                            )

                            Text(
                                text = "Pilih Peran Model Ini Secara Manual (Sangat Penting):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(top = 4.dp)
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
                                                customModelManager.setActiveTerrain(model, terrainMesh)
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

                            // Dedicated Overwrite Default Controls
                            val isPrimaryChar = customModelManager.activeCharacterFileName.equals(model.fileName, ignoreCase = true)
                            val isPrimaryTerrain = customModelManager.activeTerrainFileName.equals(model.fileName, ignoreCase = true)

                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1220)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFFFD600).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "⚡ Kontrol Overwrite Penuh (Hilangkan Konflik Aset Bawaan):",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFFD600)
                                    )
                                    Text(
                                        text = "Tombol ini mengganti total hero atau permukaan bukit bawaan dengan aset import Anda secara permanen.",
                                        fontSize = 9.sp,
                                        color = Color(0xFFB0BEC5)
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                customModelManager.setActiveCharacter(model, updatePlayerConfig = true)
                                                customModelManager.activeCustomCharacterMesh = model.mesh
                                                Toast.makeText(context, "👑 Model '${model.fileName}' kini aktif menggantikan Karakter Hero Default!", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isPrimaryChar) Color(0xFF00E5FF) else Color(0xFF1E2B47)
                                            ),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = if (isPrimaryChar) "👑 Hero Utama (Aktif)" else "⭐ Overwrite Hero Default",
                                                color = if (isPrimaryChar) Color.Black else Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                customModelManager.setActiveTerrain(model, terrainMesh)
                                                terrainMesh.setCustomMesh(model.mesh)
                                                Toast.makeText(context, "🗺️ Model '${model.fileName}' kini aktif menggantikan Permukaan/Terrain Default!", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isPrimaryTerrain) Color(0xFF76FF03) else Color(0xFF1E2B47)
                                            ),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = if (isPrimaryTerrain) "🗺️ Map Utama (Aktif)" else "⭐ Overwrite Map Default",
                                                color = if (isPrimaryTerrain) Color.Black else Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            Divider(color = Color(0xFF1F2B45))

                            // 2A. CHARACTER ANIMATION BINDER PANEL
                            if (!isMapModel || hasAnimations) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "🏃 Pemetaan & Penyambungan Animasi Karakter",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = Color(0xFF76FF03)
                                            )
                                            Text(
                                                text = "Hubungkan klip animasi GLB ke status gerakan fisik game.",
                                                fontSize = 10.sp,
                                                color = Color(0xFFB0BEC5)
                                            )
                                        }

                                        if (model.mesh.animationClips.isNotEmpty()) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Button(
                                                    onClick = {
                                                        runAutoDetect(model.mesh.animationClips.map { it.name })
                                                        Toast.makeText(context, "⚡ Deteksi otomatis klip berhasil diterapkan!", Toast.LENGTH_SHORT).show()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                                    modifier = Modifier.height(34.dp)
                                                ) {
                                                    Text("⚡ Auto-Match", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                                }

                                                OutlinedButton(
                                                    onClick = {
                                                        idleBind = ""
                                                        walkBind = ""
                                                        runBind = ""
                                                        jumpBind = ""
                                                        slashBind = ""
                                                        clipForQuickAssign = null
                                                        Toast.makeText(context, "Binding animasi direset", Toast.LENGTH_SHORT).show()
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                                    modifier = Modifier.height(34.dp)
                                                ) {
                                                    Text("Reset", color = Color(0xFFFF5252), fontSize = 10.sp)
                                                }
                                            }
                                        }

                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color(0xFF1E2B47), RoundedCornerShape(8.dp))
                                                .padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                "Satu-Ketuk Preset Animasi Cepat (Gaya Mixamo / Blender / Synty):",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFFFD600)
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                val clips = model.mesh.animationClips.map { it.name }
                                                
                                                // 1. Mixamo Preset
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
                                                    modifier = Modifier.height(28.dp)
                                                ) {
                                                    Text("🏃 Mixamo Style", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                }

                                                // 2. Blender / Unity Capitalized
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
                                                    modifier = Modifier.height(28.dp)
                                                ) {
                                                    Text("🎨 Blender / Unity Rig", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                }

                                                // 3. Reset to Standard System Defaults (For fallback or unrigged custom)
                                                Button(
                                                    onClick = {
                                                        idleBind = "anim_idle"
                                                        walkBind = "anim_walk"
                                                        runBind = "anim_run"
                                                        jumpBind = "anim_jump"
                                                        slashBind = "anim_slash"
                                                        Toast.makeText(context, "Direset ke nama animasi bawaan sistem!", Toast.LENGTH_SHORT).show()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD84315)),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(28.dp)
                                                ) {
                                                    Text("⚙️ Bawaan Sistem", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }

                                    if (model.mesh.animationClips.isEmpty()) {
                                        Text(
                                            text = "ℹ️ Tidak ditemukan klip animasi internal di file GLB ini. Karakter ini akan beranimasi secara prosedural.",
                                            fontSize = 10.sp,
                                            color = Color(0xFFFFB74D)
                                        )
                                    } else {
                                        val clipNames = model.mesh.animationClips.map { it.name }

                                        // Horizontal scrollable chips for all clips
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Klip Animasi Internal GLB (${clipNames.size} klip - Ketuk klip untuk pasang cepat):",
                                                    fontSize = 10.sp,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "Geser ke samping →",
                                                    fontSize = 9.sp,
                                                    color = Color(0xFF80D8FF)
                                                )
                                            }

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                clipNames.forEach { name ->
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
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            Icon(
                                                                Icons.Default.PlayArrow,
                                                                contentDescription = null,
                                                                tint = if (assignedBadge != null) Color(0xFF76FF03) else Color(0xFF00E5FF),
                                                                modifier = Modifier.size(12.dp)
                                                            )
                                                            Column {
                                                                Text(
                                                                    text = cleanName,
                                                                    fontSize = 11.sp,
                                                                    color = Color.White,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                                if (cleanName != name) {
                                                                    Text(
                                                                        text = name,
                                                                        fontSize = 8.sp,
                                                                        color = Color(0xFF80D8FF).copy(alpha = 0.6f),
                                                                        fontFamily = FontFamily.Monospace,
                                                                        maxLines = 1
                                                                    )
                                                                }
                                                            }
                                                            if (assignedBadge != null) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .background(Color(0xFF76FF03), RoundedCornerShape(3.dp))
                                                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                                                ) {
                                                                    Text(
                                                                        text = assignedBadge,
                                                                        fontSize = 8.sp,
                                                                        color = Color.Black,
                                                                        fontWeight = FontWeight.Bold
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            // INLINE Quick Assign Bar (Never uses popup AlertDialog!)
                                            if (clipForQuickAssign != null) {
                                                val qClip = clipForQuickAssign!!
                                                val qClean = cleanClipDisplayName(qClip)
                                                Card(
                                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF162A45)),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .border(1.dp, Color(0xFF00E5FF), RoundedCornerShape(8.dp))
                                                        .padding(vertical = 4.dp)
                                                ) {
                                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = "⚡ Pasang Klip '$qClean' ke Status:",
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color(0xFF00E5FF)
                                                            )
                                                            IconButton(
                                                                onClick = { clipForQuickAssign = null },
                                                                modifier = Modifier.size(24.dp)
                                                            ) {
                                                                Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
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
                                                                            CharacterAnimSlot.JUMP -> jumpBind = qClip
                                                                            CharacterAnimSlot.SLASH -> slashBind = qClip
                                                                        }
                                                                        clipForQuickAssign = null
                                                                        Toast.makeText(context, "✓ Klip '$qClean' dipasang ke ${slot.title}", Toast.LENGTH_SHORT).show()
                                                                    },
                                                                    colors = ButtonDefaults.buttonColors(
                                                                        containerColor = if (isSlotSet) Color(0xFF2E7D32) else Color(0xFF00E5FF)
                                                                    ),
                                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                                    modifier = Modifier.height(30.dp)
                                                                ) {
                                                                    Text(
                                                                        "${slot.icon} ${slot.key.uppercase()}",
                                                                        color = if (isSlotSet) Color.White else Color.Black,
                                                                        fontSize = 9.sp,
                                                                        fontWeight = FontWeight.Bold
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(Modifier.height(4.dp))

                                        // Action Slots Bind Cards (Inline Selection - No Dialogs)
                                        Text(
                                            text = "Slot Status Gerakan Karakter (Ketuk 'Pilih Klip' untuk memilih):",
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
                                                availableClips = clipNames,
                                                cleanClipDisplayName = ::cleanClipDisplayName,
                                                onSelectClip = { idleBind = it },
                                                onClearClick = { idleBind = "" }
                                            )
                                            AnimationBindCard(
                                                slotTitle = "Berjalan (WALK)",
                                                slotIcon = "🚶",
                                                slotDescription = "Saat joystick digerakkan santai",
                                                currentValue = walkBind,
                                                availableClips = clipNames,
                                                cleanClipDisplayName = ::cleanClipDisplayName,
                                                onSelectClip = { walkBind = it },
                                                onClearClick = { walkBind = "" }
                                            )
                                            AnimationBindCard(
                                                slotTitle = "Berlari Cepat (RUN)",
                                                slotIcon = "🏃",
                                                slotDescription = "Saat tombol sprint aktif / lari cepat",
                                                currentValue = runBind,
                                                availableClips = clipNames,
                                                cleanClipDisplayName = ::cleanClipDisplayName,
                                                onSelectClip = { runBind = it },
                                                onClearClick = { runBind = "" }
                                            )
                                            AnimationBindCard(
                                                slotTitle = "Melompat (JUMP)",
                                                slotIcon = "🦘",
                                                slotDescription = "Saat tombol lompat ditekan / melayang",
                                                currentValue = jumpBind,
                                                availableClips = clipNames,
                                                cleanClipDisplayName = ::cleanClipDisplayName,
                                                onSelectClip = { jumpBind = it },
                                                onClearClick = { jumpBind = "" }
                                            )
                                            AnimationBindCard(
                                                slotTitle = "Menyerang (SLASH / ATTACK)",
                                                slotIcon = "⚔️",
                                                slotDescription = "Saat tombol aksi serang ditekan",
                                                currentValue = slashBind,
                                                availableClips = clipNames,
                                                cleanClipDisplayName = ::cleanClipDisplayName,
                                                onSelectClip = { slashBind = it },
                                                onClearClick = { slashBind = "" }
                                            )
                                        }

                                        // Section: Penyesuaian Ukuran, Tinggi & Fisika Karakter (Anti-Melayang / Jalangkung)
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF162032)),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF00E5FF), RoundedCornerShape(10.dp))
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text("📏", fontSize = 16.sp)
                                                    Text(
                                                        "Penyesuaian Skala & Posisi Model 3D",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        color = Color(0xFF00E5FF)
                                                    )
                                                }

                                                Text(
                                                    "Bila model karakter Anda melayang seperti jalangkung atau terkubur, sesuaikan Tinggi Offset (Y) di bawah ini agar kakinya pas menempel di tanah.",
                                                    fontSize = 10.sp,
                                                    color = Color(0xFFB0BEC5),
                                                    lineHeight = 14.sp
                                                )

                                                // Slider: Skala Karakter (charScale)
                                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text("Skala / Ukuran Model 3D:", fontSize = 11.sp)
                                                        Text("${"%.2f".format(charScale)}x", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                    }
                                                    Slider(
                                                        value = charScale,
                                                        onValueChange = { charScale = it },
                                                        valueRange = 0.3f..3.5f,
                                                        colors = SliderDefaults.colors(thumbColor = Color(0xFF00E5FF), activeTrackColor = Color(0xFF00E5FF))
                                                    )
                                                }

                                                // Slider: Tinggi Offset (heightOffset) -> SOLVES JALANGKUNG MELAYANG
                                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text("Tinggi Offset (Y) - Anti Melayang:", fontSize = 11.sp)
                                                        Text("${"%.2f".format(heightOffset)} meter", color = Color(0xFF76FF03), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                    }
                                                    Slider(
                                                        value = heightOffset,
                                                        onValueChange = { heightOffset = it },
                                                        valueRange = -2.5f..2.5f,
                                                        colors = SliderDefaults.colors(thumbColor = Color(0xFF76FF03), activeTrackColor = Color(0xFF76FF03))
                                                    )
                                                }

                                                // Slider: Rotasi Offset (rotationOffset)
                                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text("Rotasi Offset Hadap Depan (Y-Axis):", fontSize = 11.sp)
                                                        Text("${rotationOffset.toInt()}°", color = Color(0xFFFFD600), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                    }
                                                    Slider(
                                                        value = rotationOffset,
                                                        onValueChange = { rotationOffset = it },
                                                        valueRange = 0f..360f,
                                                        colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD600), activeTrackColor = Color(0xFFFFD600))
                                                    )
                                                }

                                                // Slider: Radius Fisika Tabrakan (collisionRadius)
                                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text("Radius Kolisi Tabrakan Fisika:", fontSize = 11.sp)
                                                        Text("${"%.2f".format(collisionRadius)}m", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                    }
                                                    Slider(
                                                        value = collisionRadius,
                                                        onValueChange = { collisionRadius = it },
                                                        valueRange = 0.2f..2.0f,
                                                        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White)
                                                    )
                                                }

                                                // Slider: Tinggi Fisika Tabrakan (collisionHeight)
                                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text("Tinggi Kolisi Tabrakan Fisika:", fontSize = 11.sp)
                                                        Text("${"%.2f".format(collisionHeight)}m", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                    }
                                                    Slider(
                                                        value = collisionHeight,
                                                        onValueChange = { collisionHeight = it },
                                                        valueRange = 0.5f..3.5f,
                                                        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White)
                                                    )
                                                }

                                                // Slider: Kecepatan Jalan (walkSpeed)
                                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text("Kecepatan Berjalan Karakter:", fontSize = 11.sp)
                                                        Text("${"%.1f".format(walkSpeed)} m/s", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                    }
                                                    Slider(
                                                        value = walkSpeed,
                                                        onValueChange = { walkSpeed = it },
                                                        valueRange = 2.0f..15.0f,
                                                        colors = SliderDefaults.colors(thumbColor = Color(0xFF00E5FF), activeTrackColor = Color(0xFF00E5FF))
                                                    )
                                                }

                                                // Slider: Multiplier Kecepatan Lari (runMultiplier)
                                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text("Multiplier Lari Cepat (Sprint):", fontSize = 11.sp)
                                                        Text("${"%.1f".format(runMultiplier)}x", color = Color(0xFF76FF03), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                    }
                                                    Slider(
                                                        value = runMultiplier,
                                                        onValueChange = { runMultiplier = it },
                                                        valueRange = 1.1f..2.5f,
                                                        colors = SliderDefaults.colors(thumbColor = Color(0xFF76FF03), activeTrackColor = Color(0xFF76FF03))
                                                    )
                                                }

                                                // Slider: Kekuatan Lompatan (jumpImpulse)
                                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text("Kekuatan Dorongan Melompat:", fontSize = 11.sp)
                                                        Text("${"%.1f".format(jumpImpulse)}", color = Color(0xFFFFD600), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                    }
                                                    Slider(
                                                        value = jumpImpulse,
                                                        onValueChange = { jumpImpulse = it },
                                                        valueRange = 5.0f..20.0f,
                                                        colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD600), activeTrackColor = Color(0xFFFFD600))
                                                    )
                                                }
                                            }
                                        }

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
                                                Toast.makeText(context, "✓ Berhasil Menyimpan & Menghubungkan Animasi Karakter!", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF76FF03)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(46.dp)
                                                .testTag("apply_animations_patch_button")
                                        ) {
                                            Text(
                                                "✓ Simpan & Hubungkan Animasi Karakter",
                                                color = Color.Black,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }

                            // 2B. MAP CONFIG & SOLID BLOCK COLLISION RADAR PATCHER
                            if (isMapModel) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = "🗺️ Map Patcher Visual & Bintik Radar Orbit",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF00E5FF)
                                    )
                                    Text(
                                        text = "Tampilan orbit dari atas. Ketuk pada peta visual di bawah untuk langsung menempatkan titik spawn, zona pembatas tabrakan solid, atau portal pintu pindah.",
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
                                            PatcherTool.ADD_SOLID_BLOCK to "🧱 Blok Solid (Wall)",
                                            PatcherTool.ADD_PORTAL to "🌀 Tambah Portal"
                                        ).forEach { (tool, label) ->
                                            Button(
                                                onClick = { activeTool = tool },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (activeTool == tool) Color(0xFF00E5FF) else Color(0xFF1E2B45)
                                                ),
                                                modifier = Modifier.weight(1f)
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

                                    // Block scale / Target file parameters depending on tool
                                    when (activeTool) {
                                        PatcherTool.ADD_SOLID_BLOCK -> {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Lebar Blok Pembatas: ${scaleMultiplier.toInt()}m", fontSize = 11.sp, color = Color.White)
                                                Slider(
                                                    value = scaleMultiplier,
                                                    onValueChange = { scaleMultiplier = it },
                                                    valueRange = 2f..16f,
                                                    modifier = Modifier.width(180.dp)
                                                )
                                            }
                                        }
                                        PatcherTool.ADD_PORTAL -> {
                                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text("Pilih Target Pintu / Portal Teleportasi:", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                
                                                var portalExpanded by remember { mutableStateOf(false) }
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(Color(0xFF1E2B45), RoundedCornerShape(6.dp))
                                                        .border(1.dp, Color(0xFF37474F), RoundedCornerShape(6.dp))
                                                        .clickable { portalExpanded = true }
                                                        .padding(10.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = if (customPortalTargetGbl == "null") "❌ Datar Null (Hanya trigger rintangan/flat)" else "🗺️ $customPortalTargetGbl",
                                                            fontSize = 11.sp,
                                                            color = if (customPortalTargetGbl == "null") Color(0xFFFF5252) else Color(0xFF00E5FF)
                                                        )
                                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
                                                    }
                                                    
                                                    DropdownMenu(
                                                        expanded = portalExpanded,
                                                        onDismissRequest = { portalExpanded = false },
                                                        modifier = Modifier.background(Color(0xFF101726))
                                                    ) {
                                                        DropdownMenuItem(
                                                            text = { Text("❌ Datar Null (Tanpa Teleport)", color = Color.White, fontSize = 11.sp) },
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
                                        }
                                        PatcherTool.SET_SPAWN -> {
                                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text("Posisikan Spawn Player Secara Cepat:", fontSize = 11.sp, color = Color.White)
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Button(
                                                        onClick = {
                                                            playerPos.set(0f, terrainMesh.heightQuery.sampleSurface(0f, 0f).height + 1f, 0f)
                                                            mapUpdateTrigger++
                                                            Toast.makeText(context, "📍 Spawn diset ke Pusat Peta (0, 0)", Toast.LENGTH_SHORT).show()
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2B45)),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Text("Spawn Tengah (0,0)", fontSize = 10.sp)
                                                    }
                                                    
                                                    Button(
                                                        onClick = {
                                                            val maxB = 110f
                                                            playerPos.set(maxB, terrainMesh.heightQuery.sampleSurface(maxB, maxB).height + 1f, maxB)
                                                            mapUpdateTrigger++
                                                            Toast.makeText(context, "📍 Spawn diset ke Batas Maksimal (110, 110)", Toast.LENGTH_SHORT).show()
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2B45)),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Text("Batas Maksimal (110,110)", fontSize = 10.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // VISUAL RADAR CANVAS (ORBIT VIEW FROM ABOVE)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1f)
                                            .background(Color(0xFF070B12), RoundedCornerShape(8.dp))
                                            .border(1.5.dp, Color(0xFF00E5FF), RoundedCornerShape(8.dp))
                                            .pointerInput(mapUpdateTrigger) {
                                                detectTapGestures { offset ->
                                                    // Map canvas pixel coordinate to 3D world space coordinates
                                                    val size = Size(size.width.toFloat(), size.height.toFloat())
                                                    val wx = (offset.x / size.width) * worldSize + worldMin
                                                    val wz = (offset.y / size.height) * worldSize + worldMin

                                                    val s = terrainMesh.heightQuery.sampleSurface(wx, wz)
                                                    val wy = s.height

                                                    when (activeTool) {
                                                        PatcherTool.SET_SPAWN -> {
                                                            playerPos.set(wx, wy + 1f, wz)
                                                            Toast.makeText(context, "📍 Titik Lahir Player dipindahkan ke (${String.format("%.1f", wx)}, ${String.format("%.1f", wz)})", Toast.LENGTH_SHORT).show()
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
                                                            Toast.makeText(context, "🧱 Blok Solid ukuran ${scaleMultiplier.toInt()}m ditambahkan!", Toast.LENGTH_SHORT).show()
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
                                                            Toast.makeText(context, "🌀 Portal Teleportasi ke $customPortalTargetGbl terdaftar!", Toast.LENGTH_SHORT).show()
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

                                            // Draw concentric radar range circles
                                            val rings = listOf(0.2f, 0.4f, 0.6f, 0.8f, 0.95f)
                                            rings.forEach { r ->
                                                drawCircle(
                                                    color = Color(0x2200E5FF),
                                                    radius = cx * r,
                                                    center = Offset(cx, cy),
                                                    style = Stroke(width = 1f)
                                                )
                                            }

                                            // Draw horizontal and vertical radar crosshair lines
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

                                            // Draw topographic topographic dots outline of terrain mesh!
                                            // Downsample to prevent canvas lag
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
                                                    drawCircle(
                                                        color = Color(0x3376FF03),
                                                        radius = 1.5f,
                                                        center = offset
                                                    )
                                                    i += stepSize
                                                }
                                            }

                                            // Draw boundaries / limits box
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

                                            // Draw solid barriers
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

                                            // Draw teleportation portals with distinct glowing rings
                                            interactionSystem.interactables.forEach { item ->
                                                val pOffset = Offset(((item.position.x - worldMin) / worldSize) * canvasSize.width, ((item.position.z - worldMin) / worldSize) * canvasSize.height)
                                                val isPortal = item.targetTeleportPos != null ||
                                                    item.type == InteractableType.QUANTUM_PORTAL ||
                                                    item.type == InteractableType.HOUSE_DOOR_ENTER ||
                                                    item.type == InteractableType.HOUSE_DOOR_EXIT

                                                if (isPortal) {
                                                    drawCircle(color = Color(0x66E040FB), radius = 12f, center = pOffset)
                                                    drawCircle(color = Color(0xFF00E5FF), radius = 7f, center = pOffset, style = Stroke(2f))
                                                    drawCircle(color = Color.White, radius = 3f, center = pOffset)
                                                } else {
                                                    drawCircle(color = Color(0xFFFFD600), radius = 5f, center = pOffset)
                                                    drawCircle(color = Color.Black, radius = 2f, center = pOffset)
                                                }
                                            }

                                            // Draw Player Start Point
                                            val pOffset = Offset(((playerPos.x - worldMin) / worldSize) * canvasSize.width, ((playerPos.z - worldMin) / worldSize) * canvasSize.height)
                                            drawCircle(
                                                color = Color(0xFF76FF03),
                                                radius = 10f,
                                                center = pOffset
                                            )
                                            drawCircle(
                                                color = Color.Black,
                                                radius = 3f,
                                                center = pOffset
                                            )
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
                                                Toast.makeText(context, "🧹 Objek & rintangan bukit bawaan dibersihkan untuk peta kustom!", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("🧹 Bersihkan Objek Bawaan", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = {
                                                customModelManager.setActiveTerrain(model, terrainMesh)
                                                terrainMesh.setCustomMesh(model.mesh)
                                                Toast.makeText(context, "✓ Sukses Mempatch & Menyimpan Config Map ke Game!", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                                            modifier = Modifier.weight(1.3f).testTag("save_map_patch_button")
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
                    Text(slotIcon, fontSize = 20.sp)
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
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF76FF03)
                            )
                            if (clean != currentValue) {
                                Text(
                                    text = currentValue,
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF80D8FF).copy(alpha = 0.7f),
                                    maxLines = 1
                                )
                            }
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
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Hapus binding",
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Button(
                        onClick = { isExpanded = !isExpanded },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isExpanded) Color(0xFF006064) else if (currentValue.isEmpty()) Color(0xFF00E5FF) else Color(0xFF1E2B45)
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
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

            // Inline Expansion Drawer - Lists all clips without opening any popup window!
            if (isExpanded) {
                Divider(color = Color(0xFF263238))
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
                        // Chips/Buttons for each available clip
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
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
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
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Column {
                                            Text(
                                                text = cleanName,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color(0xFF76FF03) else Color.White
                                            )
                                            if (cleanName != clip) {
                                                Text(
                                                    text = clip,
                                                    fontSize = 8.sp,
                                                    color = Color(0xFF80D8FF).copy(alpha = 0.6f),
                                                    fontFamily = FontFamily.Monospace,
                                                    maxLines = 1
                                                )
                                            }
                                        }
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

                    // Clear / Disable option
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

                    // Optional manual custom text field
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
