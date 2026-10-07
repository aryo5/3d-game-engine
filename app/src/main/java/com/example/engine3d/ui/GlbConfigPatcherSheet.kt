package com.example.engine3d.ui

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import com.example.engine3d.physics.WorldBarrier
import com.example.engine3d.terrain.TerrainMesh

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

    // Update bindings when selected model changes
    LaunchedEffect(selectedModel) {
        selectedModel?.let { model ->
            manualModelTypeOverride = model.target
            val clips = model.mesh.animationClips
            idleBind = clips.firstOrNull { it.name.contains("idle", ignoreCase = true) }?.name ?: ""
            walkBind = clips.firstOrNull { it.name.contains("walk", ignoreCase = true) || it.name.contains("move", ignoreCase = true) }?.name ?: ""
            runBind = clips.firstOrNull { it.name.contains("run", ignoreCase = true) || it.name.contains("sprint", ignoreCase = true) }?.name ?: ""
            jumpBind = clips.firstOrNull { it.name.contains("jump", ignoreCase = true) }?.name ?: ""
            slashBind = clips.firstOrNull { it.name.contains("slash", ignoreCase = true) || it.name.contains("attack", ignoreCase = true) }?.name ?: ""
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
                                                customModelManager.activeCustomTerrainMesh = model.mesh
                                                terrainMesh.setCustomMesh(model.mesh)
                                            } else {
                                                customModelManager.activeCustomCharacterMesh = model.mesh
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

                            Divider(color = Color(0xFF1F2B45))

                            // 2A. CHARACTER ANIMATION BINDER PANEL
                            if (!isMapModel || hasAnimations) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = "🏃 Pemetaan & Penyambungan Animasi Karakter",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF76FF03)
                                    )
                                    Text(
                                        text = "Hubungkan secara manual klip animasi internal yang diekstrak dari GLB ini ke status gerakan fisik game.",
                                        fontSize = 10.sp,
                                        color = Color(0xFFB0BEC5)
                                    )

                                    if (model.mesh.animationClips.isEmpty()) {
                                        Text(
                                            text = "ℹ️ Tidak ditemukan klip animasi internal di file GLB ini. Karakter ini akan beranimasi secara prosedural.",
                                            fontSize = 10.sp,
                                            color = Color(0xFFFFB74D)
                                        )
                                    } else {
                                        val clipNames = model.mesh.animationClips.map { it.name }
                                        
                                        // Display visual chips of extracted clips
                                        Text(text = "Klip Animasi Internal GLB (${clipNames.size} clips):", fontSize = 10.sp, color = Color.White)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            clipNames.forEach { name ->
                                                Box(
                                                    modifier = Modifier
                                                        .background(Color(0xFF263238), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(name, fontSize = 9.sp, color = Color(0xFF80D8FF), fontFamily = FontFamily.Monospace)
                                                }
                                            }
                                        }

                                        Spacer(Modifier.height(4.dp))

                                        // Dropdown bindings select
                                        AnimationBindSelector("Klip Berdiri Diam (IDLE)", idleBind, clipNames) { idleBind = it }
                                        AnimationBindSelector("Klip Berjalan (WALK)", walkBind, clipNames) { walkBind = it }
                                        AnimationBindSelector("Klip Berlari cepat (RUN)", runBind, clipNames) { runBind = it }
                                        AnimationBindSelector("Klip Melompat (JUMP)", jumpBind, clipNames) { jumpBind = it }
                                        AnimationBindSelector("Klip Menyerang/Jurus (SLASH)", slashBind, clipNames) { slashBind = it }

                                        Button(
                                            onClick = {
                                                val config = customModelManager.playerConfig.copy(
                                                    characterName = model.fileName.substringBeforeLast("."),
                                                    modelFile = model.fileName,
                                                    animIdleName = idleBind,
                                                    animWalkName = walkBind,
                                                    animRunName = runBind,
                                                    animJumpName = jumpBind,
                                                    animSlashName = slashBind
                                                )
                                                customModelManager.playerConfig = config
                                                customModelManager.activeCustomCharacterMesh = model.mesh
                                                Toast.makeText(context, "✓ Sukses Memetakan & Menyambungkan Animasi Karakter!", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF76FF03)),
                                            modifier = Modifier.fillMaxWidth().testTag("apply_animations_patch_button")
                                        ) {
                                            Text("Apply & Hubungkan Animasi Karakter", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
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

                                            // Draw portals
                                            interactionSystem.interactables.forEach { item ->
                                                val pOffset = Offset(((item.position.x - worldMin) / worldSize) * canvasSize.width, ((item.position.z - worldMin) / worldSize) * canvasSize.height)
                                                drawCircle(
                                                    color = Color(0xFF00E5FF),
                                                    radius = 8f,
                                                    center = pOffset
                                                )
                                                drawCircle(
                                                    color = Color.White,
                                                    radius = 4f,
                                                    center = pOffset
                                                )
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
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Button(
                                            onClick = {
                                                barrierManager.barriers.removeAll { !it.id.startsWith("world_limit") }
                                                interactionSystem.interactables.removeAll { !it.id.startsWith("beacon_") && !it.id.startsWith("crate_") && !it.id.startsWith("hoverboard_") && !it.id.startsWith("portal_") }
                                                mapUpdateTrigger++
                                                Toast.makeText(context, "🗑️ Semua blok kustom berhasil dibersihkan!", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Reset Blok Kustom", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = {
                                                customModelManager.activeCustomTerrainMesh = model.mesh
                                                terrainMesh.setCustomMesh(model.mesh)
                                                Toast.makeText(context, "✓ Sukses Mempatch & Menyimpan Config Map ke Game!", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                                            modifier = Modifier.weight(1.5f).testTag("save_map_patch_button")
                                        ) {
                                            Text("Patch & Simpan Config Map", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
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
fun AnimationBindSelector(
    label: String,
    currentValue: String,
    clips: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E2B45), RoundedCornerShape(6.dp))
                .border(1.dp, Color(0xFF37474F), RoundedCornerShape(6.dp))
                .clickable { expanded = true }
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentValue.ifEmpty { "(Belum dipetakan, Klik untuk pilih)" },
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (currentValue.isEmpty()) Color(0xFFFFB74D) else Color(0xFF76FF03)
                )
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(Color(0xFF101726))
            ) {
                DropdownMenuItem(
                    text = { Text("(Nihil / Null)", color = Color.White, fontSize = 11.sp) },
                    onClick = {
                        onSelect("")
                        expanded = false
                    }
                )
                clips.forEach { name ->
                    DropdownMenuItem(
                        text = { Text(name, color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                        onClick = {
                            onSelect(name)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
