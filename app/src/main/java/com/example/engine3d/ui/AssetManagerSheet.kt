package com.example.engine3d.ui

import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.engine3d.importer.BatchImportManager
import com.example.engine3d.importer.BatchImportReport
import com.example.engine3d.importer.CustomModelManager
import com.example.engine3d.importer.FileAccessConfig
import com.example.engine3d.importer.GlbParser
import com.example.engine3d.importer.ImportedModelEntry
import com.example.engine3d.importer.ModelTarget
import com.example.engine3d.importer.SampleExportManager
import com.example.engine3d.importer.StoragePermissionHelper
import com.example.engine3d.npc.NpcBehavior
import com.example.engine3d.npc.NpcEntity
import com.example.engine3d.npc.NpcManager
import com.example.engine3d.physics.BarrierManager
import com.example.engine3d.physics.BarrierType
import com.example.engine3d.physics.WorldBarrier
import com.example.engine3d.terrain.TerrainMesh
import java.io.File
import java.io.FileOutputStream

enum class AssetHubTab(val title: String, val icon: ImageVector) {
    FILES("Impor & Storage", Icons.Default.FileUpload),
    MODELS("Model Terdaftar", Icons.Default.FolderOpen),
    TERRAIN("Terrain & OBB", Icons.Default.Landscape),
    NPCS("NPC & Rintangan", Icons.Default.People)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetManagerSheet(
    customModelManager: CustomModelManager,
    batchImportManager: BatchImportManager,
    terrainMesh: TerrainMesh,
    actionManager: ActionManager,
    npcManager: NpcManager,
    barrierManager: BarrierManager,
    onModelImported: (ImportedModelEntry) -> Unit,
    onTerrainChanged: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(AssetHubTab.FILES) }

    val sampleExportManager = remember {
        SampleExportManager(
            context = context,
            barrierManager = barrierManager,
            actionManager = actionManager,
            npcManager = npcManager,
            customModelManager = customModelManager,
            terrainMesh = terrainMesh
        )
    }

    var selectedTarget by remember { mutableStateOf(ModelTarget.TERRAIN) }
    var showFormatGuide by remember { mutableStateOf(false) }
    var showNpcCreator by remember { mutableStateOf(false) }
    var showBarrierCreator by remember { mutableStateOf(false) }
    var showCustomFileSelector by remember { mutableStateOf(false) }
    var showInAppFocusFolderPicker by remember { mutableStateOf(false) }
    var showInAppWriteFolderPicker by remember { mutableStateOf(false) }
    var lastExportMessage by remember { mutableStateOf<String?>(null) }
    var lastBatchReport by remember { mutableStateOf<BatchImportReport?>(null) }

    val fileAccessConfig = remember { FileAccessConfig(context) }
    var focusPathInput by remember { mutableStateOf(fileAccessConfig.focusFolderPath) }
    var writePathInput by remember { mutableStateOf(fileAccessConfig.writeFolderPath) }

    var hasStoragePermission by remember {
        mutableStateOf(StoragePermissionHelper.isStoragePermissionGranted(context))
    }

    var hasAllFilesAccess by remember {
        mutableStateOf(StoragePermissionHelper.hasAllFilesAccess(context))
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasStoragePermission = isGranted || !StoragePermissionHelper.needsRuntimePermission()
        hasAllFilesAccess = StoragePermissionHelper.hasAllFilesAccess(context)
        if (isGranted) {
            Toast.makeText(context, "✓ Izin penyimpanan aktif!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Izin penyimpanan belum aktif.", Toast.LENGTH_LONG).show()
        }
    }

    DisposableEffect(Unit) {
        hasStoragePermission = StoragePermissionHelper.isStoragePermissionGranted(context)
        hasAllFilesAccess = StoragePermissionHelper.hasAllFilesAccess(context)
        onDispose { }
    }

    // SAF Launchers
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? ->
        if (treeUri != null) {
            val report = batchImportManager.importFolderFromTreeUri(treeUri)
            lastBatchReport = report
            onTerrainChanged()
        }
    }

    val zipPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { zipUri: Uri? ->
        if (zipUri != null) {
            val report = batchImportManager.importZipOrObbArchive(zipUri, isObb = false)
            lastBatchReport = report
            onTerrainChanged()
        }
    }

    val obbPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { obbUri: Uri? ->
        if (obbUri != null) {
            val report = batchImportManager.importZipOrObbArchive(obbUri, isObb = true)
            lastBatchReport = report
            onTerrainChanged()
        }
    }

    val modelPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val entry = customModelManager.importFromUri(uri, selectedTarget)
            if (entry != null) {
                if (selectedTarget == ModelTarget.TERRAIN) {
                    terrainMesh.setCustomMesh(entry.mesh)
                    onTerrainChanged()
                }
                onModelImported(entry)
            }
        }
    }

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
            color = Color(0xF80B111E)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                // 1. Top Header Bar (Fixed)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color(0x3300E5FF), CircleShape)
                                .border(1.dp, Color(0xFF00E5FF), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
                        }
                        Column {
                            Text(
                                text = "Pusat Hub Aset & Impor GLB",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Kelola Model 3D, Arsip ZIP/OBB, Terrain & NPC",
                                fontSize = 10.sp,
                                color = Color(0xFF76FF03),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = { showFormatGuide = true }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Info, contentDescription = "Panduan Format", tint = Color(0xFF80D8FF), modifier = Modifier.size(18.dp))
                        }
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("✖ Tutup", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))

                // 2. Active Model Status Quick Banner
                val activeHeroName = customModelManager.importedModels.firstOrNull { it.mesh == customModelManager.activeCustomCharacterMesh }?.fileName ?: "Farmer (Bawaan)"
                val activeMapName = customModelManager.importedModels.firstOrNull { it.mesh == customModelManager.activeCustomTerrainMesh }?.fileName ?: "Procedural Island"

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF101726),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4400E5FF))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("👑 Hero:", fontSize = 10.sp, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                            Text(activeHeroName, fontSize = 10.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("🗺️ Map:", fontSize = 10.sp, color = Color(0xFF76FF03), fontWeight = FontWeight.Bold)
                            Text(activeMapName, fontSize = 10.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // 3. Tab Navigation Bar (Scrollable to prevent truncation on any screen)
                ScrollableTabRow(
                    selectedTabIndex = activeTab.ordinal,
                    containerColor = Color(0xFF101827),
                    contentColor = Color(0xFF00E5FF),
                    edgePadding = 4.dp,
                    indicator = { tabPositions ->
                        if (activeTab.ordinal < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[activeTab.ordinal]),
                                color = Color(0xFF00E5FF)
                            )
                        }
                    }
                ) {
                    AssetHubTab.values().forEach { tab ->
                        val isSelected = activeTab == tab
                        Tab(
                            selected = isSelected,
                            onClick = { activeTab = tab },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        tab.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
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

                Spacer(Modifier.height(8.dp))

                // 4. Bounded Tab Content (Isolated scroll per tab)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (activeTab) {
                        AssetHubTab.FILES -> {
                            // TAB 1: FILE IMPORT & STORAGE PERMISSIONS
                            if (!hasStoragePermission || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !hasAllFilesAccess)) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1B0E)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.border(1.dp, Color(0xFFFFB74D), RoundedCornerShape(10.dp))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFFFFB74D))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Izin Akses Penyimpanan File", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFFFFCC80))
                                            Text(
                                                "Berikan izin agar engine dapat membaca model GLB dari memori perangkat.",
                                                fontSize = 10.sp,
                                                color = Color.White
                                            )
                                        }
                                        Button(
                                            onClick = {
                                                if (StoragePermissionHelper.needsRuntimePermission() && !hasStoragePermission) {
                                                    storagePermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !hasAllFilesAccess) {
                                                    StoragePermissionHelper.launchAllFilesAccessSettings(context)
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB74D)),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Text("Beri Izin", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            // Notification Reports
                            lastBatchReport?.let { report ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "✓ Berhasil memuat ${report.glbCount + report.objCount} model 3D!",
                                            fontSize = 11.sp,
                                            color = Color.White,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(onClick = { lastBatchReport = null }, modifier = Modifier.size(22.dp)) {
                                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }

                            lastExportMessage?.let { msg ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF004D40)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(msg, fontSize = 11.sp, color = Color.White, modifier = Modifier.weight(1f))
                                        IconButton(onClick = { lastExportMessage = null }, modifier = Modifier.size(22.dp)) {
                                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }

                            // Folder Fokus & Target Tulis Card
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1B2E)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.border(1.dp, Color(0xFF1E2B47), RoundedCornerShape(10.dp))
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("📁 Folder Fokus & Penyimpanan In-App", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF00E5FF))

                                    OutlinedTextField(
                                        value = focusPathInput,
                                        onValueChange = { focusPathInput = it; fileAccessConfig.focusFolderPath = it },
                                        label = { Text("Path Folder Fokus (GLB, Config)", fontSize = 10.sp) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Button(
                                            onClick = { showInAppFocusFolderPicker = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263238)),
                                            modifier = Modifier.weight(1f).height(32.dp),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("Pilih Folder", color = Color.White, fontSize = 10.sp)
                                        }

                                        Button(
                                            onClick = {
                                                val rep = batchImportManager.scanDirectFolderPath(focusPathInput)
                                                lastBatchReport = rep
                                                onTerrainChanged()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF76FF03)),
                                            modifier = Modifier.weight(1f).height(32.dp),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("Pindai Path", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                        }
                                    }

                                    Button(
                                        onClick = { showCustomFileSelector = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                                        modifier = Modifier.fillMaxWidth().height(32.dp),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("📌 Pemilih & Pemeta File Kustom", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                    }
                                }
                            }

                            // Quick File Pickers Card
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF131E33)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.border(1.dp, Color(0xFF1E2B47), RoundedCornerShape(10.dp))
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("📥 Impor File Mandiri (.GLB, .OBJ, .ZIP)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF76FF03))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                selectedTarget = ModelTarget.CHARACTER
                                                modelPickerLauncher.launch("*/*")
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                                            modifier = Modifier.weight(1f).height(34.dp),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.FileUpload, contentDescription = null, tint = Color.Black, modifier = Modifier.size(13.dp))
                                            Spacer(Modifier.width(3.dp))
                                            Text("GLB Hero", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = {
                                                selectedTarget = ModelTarget.TERRAIN
                                                modelPickerLauncher.launch("*/*")
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF76FF03)),
                                            modifier = Modifier.weight(1f).height(34.dp),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.FileUpload, contentDescription = null, tint = Color.Black, modifier = Modifier.size(13.dp))
                                            Spacer(Modifier.width(3.dp))
                                            Text("GLB Map", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Button(
                                            onClick = { zipPickerLauncher.launch("application/zip") },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263238)),
                                            modifier = Modifier.weight(1f).height(32.dp),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.Archive, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(13.dp))
                                            Spacer(Modifier.width(3.dp))
                                            Text("Arsip .ZIP", color = Color.White, fontSize = 10.sp)
                                        }

                                        Button(
                                            onClick = { folderPickerLauncher.launch(null) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263238)),
                                            modifier = Modifier.weight(1f).height(32.dp),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color(0xFFFFD600), modifier = Modifier.size(13.dp))
                                            Spacer(Modifier.width(3.dp))
                                            Text("Folder SAF", color = Color.White, fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }

                        AssetHubTab.MODELS -> {
                            // TAB 2: REGISTERED MODELS & ROLE MAPPING
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF162238)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "📦 Model GLB Terdaftar (${customModelManager.importedModels.size})",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color(0xFF00E5FF)
                                        )
                                        Text(
                                            text = "Atur peran Hero / Map",
                                            fontSize = 9.sp,
                                            color = Color(0xFFB0BEC5)
                                        )
                                    }

                                    if (customModelManager.importedModels.isEmpty()) {
                                        Surface(
                                            color = Color(0xFF101726),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(16.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(Icons.Default.Inventory2, contentDescription = null, tint = Color(0xFFFFB74D), modifier = Modifier.size(28.dp))
                                                Text(
                                                    text = "Belum ada file GLB kustom terdaftar.",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFFFB74D)
                                                )
                                                Text(
                                                    text = "Buka tab 'Impor & Storage' untuk menambahkan model GLB/OBJ dari memori perangkat.",
                                                    fontSize = 10.sp,
                                                    color = Color(0xFFB0BEC5)
                                                )
                                                Button(
                                                    onClick = { activeTab = AssetHubTab.FILES },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                                                    modifier = Modifier.height(28.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                ) {
                                                    Text("Ke Tab Impor ➔", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    } else {
                                        customModelManager.importedModels.forEach { entry ->
                                            val isTerrainActive = customModelManager.activeCustomTerrainMesh == entry.mesh
                                            val isCharActive = customModelManager.activeCustomCharacterMesh == entry.mesh

                                            Card(
                                                colors = CardDefaults.cardColors(
                                                    containerColor = when {
                                                        isCharActive -> Color(0xFF1B3A4B)
                                                        isTerrainActive -> Color(0xFF1B4D2E)
                                                        else -> Color(0xFF101726)
                                                    }
                                                ),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.fillMaxWidth().border(
                                                    width = 1.dp,
                                                    color = when {
                                                        isCharActive -> Color(0xFF00E5FF)
                                                        isTerrainActive -> Color(0xFF76FF03)
                                                        else -> Color(0xFF263238)
                                                    },
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                            ) {
                                                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "📄 ${entry.fileName}",
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 12.sp,
                                                            color = Color.White,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            modifier = Modifier.weight(1f, fill = false)
                                                        )
                                                        Text(
                                                            text = when {
                                                                isCharActive -> "👑 HERO AKTIF"
                                                                isTerrainActive -> "🗺️ MAP AKTIF"
                                                                else -> "📦 ${entry.target.name}"
                                                            },
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = when {
                                                                isCharActive -> Color(0xFF00E5FF)
                                                                isTerrainActive -> Color(0xFF76FF03)
                                                                else -> Color(0xFFFFD600)
                                                            }
                                                        )
                                                    }

                                                    Text(
                                                        text = "Format: ${entry.format} • ${entry.mesh.vertexCount} Vertices",
                                                        fontSize = 9.sp,
                                                        color = Color(0xFF90A4AE),
                                                        fontFamily = FontFamily.Monospace
                                                    )

                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                                    ) {
                                                        Button(
                                                            onClick = {
                                                                customModelManager.setActiveCharacter(entry, updatePlayerConfig = true)
                                                                customModelManager.activeCustomCharacterMesh = entry.mesh
                                                                lastExportMessage = "✓ '${entry.fileName}' aktif sebagai Karakter Pemain!"
                                                            },
                                                            colors = ButtonDefaults.buttonColors(
                                                                containerColor = if (isCharActive) Color(0xFF00E5FF) else Color(0xFF263238)
                                                            ),
                                                            modifier = Modifier.weight(1f).height(30.dp),
                                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                                        ) {
                                                            Text("🧍 Set Hero", fontSize = 9.sp, color = if (isCharActive) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                                                        }

                                                        Button(
                                                            onClick = {
                                                                if (entry.mesh.textureBitmap == null && entry.format == "GLB") {
                                                                    customModelManager.reloadModel(entry.fileName)
                                                                }
                                                                val activeEntry = customModelManager.importedModels.firstOrNull { it.fileName == entry.fileName } ?: entry
                                                                customModelManager.setActiveTerrain(activeEntry, terrainMesh)
                                                                terrainMesh.setCustomMesh(activeEntry.mesh)
                                                                onTerrainChanged()
                                                                lastExportMessage = "✓ '${entry.fileName}' aktif sebagai Terrain Map!"
                                                            },
                                                            colors = ButtonDefaults.buttonColors(
                                                                containerColor = if (isTerrainActive) Color(0xFF76FF03) else Color(0xFF263238)
                                                            ),
                                                            modifier = Modifier.weight(1f).height(30.dp),
                                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                                        ) {
                                                            Text("🗺️ Set Map", fontSize = 9.sp, color = if (isTerrainActive) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                                                        }

                                                        IconButton(
                                                            onClick = { customModelManager.deleteModel(entry) },
                                                            modifier = Modifier.size(30.dp)
                                                        ) {
                                                            Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        AssetHubTab.TERRAIN -> {
                            // TAB 3: TERRAIN EXPORT & OBB PACKS
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2842)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.border(1.dp, Color(0xFFFFD600).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("🗺️ Ekspor Paket Sampel Lengkap & OBB", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFFFD600))
                                    Text(
                                        "Ekspor paket sampel berisi kontur terrain 3D, NPC, jurus aksi, dan batas rintangan yang siap diedit di Blender/PC.",
                                        fontSize = 10.sp,
                                        color = Color(0xFFECEFF1)
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                val msg = sampleExportManager.exportToCustomDirectory(writePathInput, isObb = false)
                                                lastExportMessage = msg
                                                val rep = batchImportManager.scanLocalAssetFolder()
                                                lastBatchReport = rep
                                                onTerrainChanged()
                                                Toast.makeText(context, "✓ Berhasil mengekspor paket .ZIP!", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD600)),
                                            modifier = Modifier.weight(1f).height(32.dp),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.FileDownload, contentDescription = null, tint = Color.Black, modifier = Modifier.size(13.dp))
                                            Spacer(Modifier.width(3.dp))
                                            Text("Ekspor .ZIP", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                        }

                                        Button(
                                            onClick = {
                                                val msg = sampleExportManager.exportToCustomDirectory(writePathInput, isObb = true)
                                                lastExportMessage = msg
                                                val rep = batchImportManager.scanLocalAssetFolder()
                                                lastBatchReport = rep
                                                onTerrainChanged()
                                                Toast.makeText(context, "✓ Berhasil mengekspor paket .OBB!", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                                            modifier = Modifier.weight(1f).height(32.dp),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.Archive, contentDescription = null, tint = Color.Black, modifier = Modifier.size(13.dp))
                                            Spacer(Modifier.width(3.dp))
                                            Text("Ekspor .OBB", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = { obbPickerLauncher.launch("*/*") },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF80D8FF)),
                                        modifier = Modifier.fillMaxWidth().height(32.dp),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Default.FileUpload, contentDescription = null, tint = Color(0xFF80D8FF), modifier = Modifier.size(13.dp))
                                        Spacer(Modifier.width(3.dp))
                                        Text("Impor Arsip .OBB dari HP", fontSize = 10.sp)
                                    }
                                }
                            }
                        }

                        AssetHubTab.NPCS -> {
                            // TAB 4: NPCS & BARRIERS
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A233A)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.border(1.dp, Color(0xFF1E2B47), RoundedCornerShape(10.dp))
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("🚧 Batas Jalan & Rintangan", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFFF5252))
                                        Button(
                                            onClick = { showBarrierCreator = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                                            modifier = Modifier.height(28.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                            Spacer(Modifier.width(2.dp))
                                            Text("Tambah", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                        }
                                    }

                                    // List active barriers
                                    if (barrierManager.barriers.isNotEmpty()) {
                                        barrierManager.barriers.forEach { b ->
                                            Surface(
                                                color = Color(0xFF101726),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(b.name, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                        Text("Tipe: ${b.type.name} • (${b.position.x.toInt()}, ${b.position.z.toInt()})", fontSize = 9.sp, color = Color(0xFF90A4AE))
                                                    }
                                                    IconButton(onClick = { barrierManager.barriers.remove(b) }, modifier = Modifier.size(24.dp)) {
                                                        Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color(0xFFFF5252), modifier = Modifier.size(14.dp))
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        Text("Belum ada rintangan terpasang.", fontSize = 10.sp, color = Color(0xFF90A4AE))
                                    }

                                    HorizontalDivider(color = Color(0xFF263238), modifier = Modifier.padding(vertical = 2.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("👥 Karakter NPC & Dialog", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF76FF03))
                                        Button(
                                            onClick = { showNpcCreator = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF76FF03)),
                                            modifier = Modifier.height(28.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(12.dp))
                                            Spacer(Modifier.width(2.dp))
                                            Text("Buat NPC", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                        }
                                    }

                                    if (npcManager.npcs.isNotEmpty()) {
                                        npcManager.npcs.forEach { npc ->
                                            Surface(
                                                color = Color(0xFF101726),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text("${npc.name} [${npc.role}]", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = Color(0xFF76FF03), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                        Text("\"${npc.dialogues.firstOrNull() ?: ""}\"", fontSize = 9.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    }
                                                    IconButton(onClick = { npcManager.removeNpc(npc) }, modifier = Modifier.size(24.dp)) {
                                                        Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color(0xFFFF5252), modifier = Modifier.size(14.dp))
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        Text("Belum ada NPC terpasang.", fontSize = 10.sp, color = Color(0xFF90A4AE))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Panduan Format
    if (showFormatGuide) {
        AlertDialog(
            onDismissRequest = { showFormatGuide = false },
            title = { Text("Penjelasan File OBB & Format Sampel", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "1. FILE .OBB:\nDi engine Apex3D, file .OBB adalah arsip terpaket yang berisi model 3D (.glb, .obj), suara, dan konfigurasi (.json) yang dibaca langsung secara instan.",
                        fontSize = 11.sp,
                        color = Color(0xFFCFD8DC)
                    )
                    Text(
                        "2. RINTANGAN & ZONA:\n• WALL_BARRIER: Tembok solid pemblokir jalan.\n• SPEED_BOOST_PAD: Pelat pengganda kecepatan.\n• BOUNCE_PAD: Pelat pelontar loncatan tinggi.",
                        fontSize = 11.sp,
                        color = Color(0xFF80D8FF)
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showFormatGuide = false }) {
                    Text("Tutup", fontSize = 11.sp)
                }
            }
        )
    }

    // Dialog Tambah Batas Jalan
    if (showBarrierCreator) {
        var barrierName by remember { mutableStateOf("Barikade Gerbang") }
        var barrierType by remember { mutableStateOf(BarrierType.WALL_BARRIER) }
        var barrierX by remember { mutableStateOf("10") }
        var barrierZ by remember { mutableStateOf("0") }
        var barrierSizeX by remember { mutableStateOf("2") }
        var barrierSizeZ by remember { mutableStateOf("6") }

        AlertDialog(
            onDismissRequest = { showBarrierCreator = false },
            title = { Text("Buat Batas Jalan Baru", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = barrierName,
                        onValueChange = { barrierName = it },
                        label = { Text("Nama Rintangan", fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            BarrierType.WALL_BARRIER to "Tembok Blokir",
                            BarrierType.SPEED_BOOST_PAD to "Speed Pad",
                            BarrierType.BOUNCE_PAD to "Pelontar"
                        ).forEach { (t, label) ->
                            FilterChip(
                                selected = barrierType == t,
                                onClick = { barrierType = t },
                                label = { Text(label, fontSize = 9.sp) }
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = barrierX,
                            onValueChange = { barrierX = it },
                            label = { Text("Posisi X", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = barrierZ,
                            onValueChange = { barrierZ = it },
                            label = { Text("Posisi Z", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = barrierSizeX,
                            onValueChange = { barrierSizeX = it },
                            label = { Text("Lebar (X)", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = barrierSizeZ,
                            onValueChange = { barrierSizeZ = it },
                            label = { Text("Panjang (Z)", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val px = barrierX.toFloatOrNull() ?: 0f
                        val pz = barrierZ.toFloatOrNull() ?: 0f
                        val sx = barrierSizeX.toFloatOrNull() ?: 2f
                        val sz = barrierSizeZ.toFloatOrNull() ?: 6f
                        val newB = WorldBarrier(
                            id = "barrier_${System.currentTimeMillis()}",
                            name = barrierName,
                            type = barrierType,
                            position = com.example.engine3d.math.Vec3(px, 1.5f, pz),
                            size = com.example.engine3d.math.Vec3(sx, 3.5f, sz),
                            isPassable = barrierType != BarrierType.WALL_BARRIER,
                            speedMultiplier = if (barrierType == BarrierType.SPEED_BOOST_PAD) 2.2f else 1.0f,
                            bounceImpulse = if (barrierType == BarrierType.BOUNCE_PAD) 14.0f else 0.0f
                        )
                        barrierManager.barriers.add(newB)
                        showBarrierCreator = false
                    }
                ) {
                    Text("Pasang Batas", fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBarrierCreator = false }) {
                    Text("Batal", fontSize = 11.sp)
                }
            }
        )
    }

    // Modal Buat NPC
    if (showNpcCreator) {
        var npcName by remember { mutableStateOf("Kapten Pengawal") }
        var npcRole by remember { mutableStateOf("Penjaga") }
        var npcDialogue by remember { mutableStateOf("Patuhi batas jalan dan barikade demi keselamatan!") }

        AlertDialog(
            onDismissRequest = { showNpcCreator = false },
            title = { Text("Buat NPC Baru", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = npcName,
                        onValueChange = { npcName = it },
                        label = { Text("Nama NPC", fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = npcRole,
                        onValueChange = { npcRole = it },
                        label = { Text("Peran", fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = npcDialogue,
                        onValueChange = { npcDialogue = it },
                        label = { Text("Dialog", fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newNpc = NpcEntity(
                            id = "npc_${System.currentTimeMillis()}",
                            name = npcName,
                            role = npcRole,
                            position = com.example.engine3d.math.Vec3(4f, 0f, -2f),
                            behavior = NpcBehavior.LOOK_AT_PLAYER,
                            dialogues = mutableListOf(npcDialogue)
                        )
                        npcManager.addNpc(newNpc)
                        showNpcCreator = false
                    }
                ) {
                    Text("Pasang NPC", fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNpcCreator = false }) {
                    Text("Batal", fontSize = 11.sp)
                }
            }
        )
    }

    // Custom File Selector Dialog
    if (showCustomFileSelector) {
        val detectedFiles = remember(focusPathInput) {
            batchImportManager.listFilesInFolderPath(focusPathInput)
        }

        AlertDialog(
            onDismissRequest = { showCustomFileSelector = false },
            title = {
                Column {
                    Text("📌 Pemeta File Kustom", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Focus Path: $focusPathInput", fontSize = 10.sp, color = Color(0xFF00E5FF))
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (detectedFiles.isEmpty()) {
                        Text(
                            "Tidak ada file GLB, OBJ, atau JSON terdeteksi di '$focusPathInput'.",
                            fontSize = 11.sp,
                            color = Color(0xFFFFB74D)
                        )
                    } else {
                        val modelFiles = detectedFiles.filter { it.extension.lowercase() in listOf("glb", "obj") }
                        modelFiles.forEach { f ->
                            Surface(
                                color = Color(0xFF162238),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(f.name, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Button(
                                            onClick = {
                                                try {
                                                    val mesh = GlbParser.parse(java.io.FileInputStream(f), f.name)
                                                    if (mesh != null) {
                                                        customModelManager.activeCustomTerrainMesh = mesh
                                                        terrainMesh.setCustomMesh(mesh)
                                                        onTerrainChanged()
                                                        lastExportMessage = "✓ '${f.name}' aktif sebagai Terrain!"
                                                    }
                                                } catch (e: Exception) {
                                                    lastExportMessage = "❌ Error: ${e.localizedMessage}"
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF76FF03)),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                            modifier = Modifier.height(26.dp)
                                        ) {
                                            Text("Map", fontSize = 9.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = {
                                                try {
                                                    val mesh = GlbParser.parse(java.io.FileInputStream(f), f.name)
                                                    if (mesh != null) {
                                                        customModelManager.activeCustomCharacterMesh = mesh
                                                        lastExportMessage = "✓ '${f.name}' aktif sebagai Karakter!"
                                                    }
                                                } catch (e: Exception) {
                                                    lastExportMessage = "❌ Error: ${e.localizedMessage}"
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                            modifier = Modifier.height(26.dp)
                                        ) {
                                            Text("Hero", fontSize = 9.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showCustomFileSelector = false }) {
                    Text("Selesai", fontSize = 11.sp)
                }
            }
        )
    }

    if (showInAppFocusFolderPicker) {
        InAppFolderPickerDialog(
            initialPath = focusPathInput,
            onFolderSelected = { selectedPath ->
                focusPathInput = selectedPath
                fileAccessConfig.focusFolderPath = selectedPath
                showInAppFocusFolderPicker = false
                val rep = batchImportManager.scanDirectFolderPath(selectedPath)
                lastBatchReport = rep
                onTerrainChanged()
            },
            onDismiss = { showInAppFocusFolderPicker = false }
        )
    }

    if (showInAppWriteFolderPicker) {
        InAppFolderPickerDialog(
            initialPath = writePathInput,
            onFolderSelected = { selectedPath ->
                writePathInput = selectedPath
                fileAccessConfig.writeFolderPath = selectedPath
                showInAppWriteFolderPicker = false
            },
            onDismiss = { showInAppWriteFolderPicker = false }
        )
    }
}
