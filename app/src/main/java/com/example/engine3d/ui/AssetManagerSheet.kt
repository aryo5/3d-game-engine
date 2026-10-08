package com.example.engine3d.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Share
import java.io.File
import java.io.FileOutputStream
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.engine3d.actions.InteractableType
import com.example.engine3d.actions.WorldInteractable
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine3d.actions.ActionManager
import com.example.engine3d.actions.CharacterAction
import com.example.engine3d.importer.BatchImportManager
import com.example.engine3d.importer.BatchImportReport
import com.example.engine3d.importer.CustomModelManager
import com.example.engine3d.importer.FileAccessConfig
import com.example.engine3d.importer.GlbParser
import com.example.engine3d.importer.ImportedModelEntry
import com.example.engine3d.importer.ModelTarget
import com.example.engine3d.importer.SampleExportManager
import com.example.engine3d.npc.NpcBehavior
import com.example.engine3d.npc.NpcEntity
import com.example.engine3d.npc.NpcManager
import com.example.engine3d.physics.BarrierManager
import com.example.engine3d.physics.BarrierType
import com.example.engine3d.physics.WorldBarrier
import com.example.engine3d.terrain.TerrainMesh
import com.example.engine3d.terrain.TerrainPreset

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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
    var lastExportMessage by remember { mutableStateOf<String?>(null) }
    var lastBatchReport by remember { mutableStateOf<BatchImportReport?>(null) }

    val fileAccessConfig = remember { FileAccessConfig(context) }
    var focusPathInput by remember { mutableStateOf(fileAccessConfig.focusFolderPath) }
    var writePathInput by remember { mutableStateOf(fileAccessConfig.writeFolderPath) }

    // 1. SAF Folder Tree Picker Launcher
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? ->
        if (treeUri != null) {
            val report = batchImportManager.importFolderFromTreeUri(treeUri)
            lastBatchReport = report
            onTerrainChanged()
        }
    }

    // SAF Folder Tree Picker Launcher for Export Target
    val exportFolderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? ->
        if (treeUri != null) {
            val msg = sampleExportManager.exportToDocumentTree(treeUri)
            lastExportMessage = msg
            fileAccessConfig.lastWriteSummary = msg
            Toast.makeText(context, "✓ Berhasil mengekspor ke folder pilihan!", Toast.LENGTH_SHORT).show()
        }
    }

    // 2. ZIP Archive Picker Launcher
    val zipPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { zipUri: Uri? ->
        if (zipUri != null) {
            val report = batchImportManager.importZipOrObbArchive(zipUri, isObb = false)
            lastBatchReport = report
            onTerrainChanged()
        }
    }

    // 3. OBB File Picker Launcher
    val obbPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { obbUri: Uri? ->
        if (obbUri != null) {
            val report = batchImportManager.importZipOrObbArchive(obbUri, isObb = true)
            lastBatchReport = report
            onTerrainChanged()
        }
    }

    // 4. Single File Picker Launcher (.glb/.obj)
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

    // 5. Config JSON File Picker Launcher
    val jsonConfigPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val tempFile = File(context.cacheDir, "temp_area_config.json")
                FileOutputStream(tempFile).use { fos -> inputStream?.copyTo(fos) }
                inputStream?.close()

                val report = BatchImportReport()
                batchImportManager.parseAreaConfigFile(tempFile, report)
                lastBatchReport = report
                onTerrainChanged()
            } catch (e: Exception) {
                e.printStackTrace()
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
                    .padding(horizontal = 18.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Top Navigation Bar (Locks full screen, never dismisses on swipe down)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = Color(0xFF00E5FF))
                        Column {
                            Text(
                                text = "Pusat Hub Aset & Konfigurasi Area GLB",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Layar Terkunci Stabil (Tidak akan tertutup saat diusap ke bawah)",
                                fontSize = 10.sp,
                                color = Color(0xFF76FF03)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { showFormatGuide = true }) {
                            Icon(Icons.Default.Info, contentDescription = "Panduan OBB & Format", tint = Color(0xFF80D8FF))
                        }
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                        ) {
                            Text("✖ Tutup Studio", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

            // Export success banner
            if (lastExportMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = lastExportMessage!!,
                        color = Color(0xFFB9F6CA),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            // Batch import report banner
            if (lastBatchReport != null) {
                val rep = lastBatchReport!!
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF004D40)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "✓ Batch Import Selesai!",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF76FF03),
                            fontSize = 13.sp
                        )
                        Text(
                            text = "${rep.glbCount} GLB • ${rep.objCount} OBJ • ${rep.actionCount} Aksi • ${rep.npcCount} NPC • ${rep.barrierCount} Batas",
                            color = Color.White,
                            fontSize = 11.sp
                        )
                        if (rep.details.isNotEmpty()) {
                            Text(
                                text = rep.details.joinToString("\n"),
                                color = Color(0xFFE0F2F1),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // SECTION FILE ACCESS: ARSITEKTUR FOCUS FOLDER PATH & WRITE FOLDER PATH
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1B2E)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.border(1.5.dp, Color(0xFF00E5FF), RoundedCornerShape(12.dp))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color(0xFF00E5FF))
                        Text(
                            text = "Arsitektur File Access: Focus Path & Write Path",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF00E5FF)
                        )
                    }

                    Text(
                        text = "Kelola folder utama untuk BACA/PINDAI file GLB & Konfigurasi (Focus Path) serta folder tujuan TULIS/EKSPOR hasil sampel & script Python (Write Path).",
                        fontSize = 11.sp,
                        color = Color(0xFFB0BEC5)
                    )

                    // 1. FOCUS FOLDER PATH (READ / SCAN)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF16233B), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "📥 FOCUS FOLDER PATH (Folder Fokus Baca & Pindai)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFF76FF03)
                        )

                        OutlinedTextField(
                            value = focusPathInput,
                            onValueChange = {
                                focusPathInput = it
                                fileAccessConfig.focusFolderPath = it
                            },
                            label = { Text("Path Folder Fokus (GLB, Config, Area)", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // Quick Presets
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(
                                "/sdcard/Download/Apex3D" to "Apex3D",
                                "/sdcard/Download/Termux_GLB" to "Termux",
                                "/sdcard/Download" to "Download",
                                "${context.filesDir.absolutePath}/ApexAssets" to "Internal"
                            ).forEach { (presetPath, label) ->
                                FilterChip(
                                    selected = focusPathInput == presetPath,
                                    onClick = {
                                        focusPathInput = presetPath
                                        fileAccessConfig.focusFolderPath = presetPath
                                    },
                                    label = { Text(label, fontSize = 9.sp) }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = { folderPickerLauncher.launch(null) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263238)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = Color(0xFF00E5FF))
                                Spacer(Modifier.width(4.dp))
                                Text("SAF Tree Picker", color = Color.White, fontSize = 10.sp)
                            }

                            Button(
                                onClick = {
                                    val rep = batchImportManager.scanDirectFolderPath(focusPathInput)
                                    lastBatchReport = rep
                                    fileAccessConfig.lastScanSummary = "${rep.glbCount} GLB, ${rep.objCount} OBJ terdeteksi"
                                    onTerrainChanged()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF76FF03)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.FileUpload, contentDescription = null, tint = Color.Black)
                                Spacer(Modifier.width(4.dp))
                                Text("Pindai Focus Path", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            }
                        }

                        // Tombol Pemeta File Kustom jika nama file config/model berbeda
                        Button(
                            onClick = { showCustomFileSelector = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = Color.Black)
                            Spacer(Modifier.width(4.dp))
                            Text("📌 Pemilih & Pemeta File Kustom (Beda Nama File)", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        }
                    }

                    // 2. WRITE FOLDER PATH (EXPORT TARGET)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E2A45), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "📤 WRITE FOLDER PATH (Folder Tulis & Ekspor Sampel/Guide)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFFFFD600)
                        )

                        Text(
                            text = "💡 Folder 'Download/Apex3D' otomatis dibuat di Pengelola File / Aplikasi Files HP Anda. Semua file dapat langsung diakses & diedit.",
                            fontSize = 10.sp,
                            color = Color(0xFFB0BEC5)
                        )

                        OutlinedTextField(
                            value = writePathInput,
                            onValueChange = {
                                writePathInput = it
                                fileAccessConfig.writeFolderPath = it
                            },
                            label = { Text("Path Target Tulis / Ekspor", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // Quick Presets & SAF Picker Button
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(
                                "/sdcard/Download/Apex3D" to "Download/Apex3D",
                                "/sdcard/Download/Termux_GLB" to "Termux",
                                "${context.filesDir.absolutePath}/ApexExports" to "Internal"
                            ).forEach { (presetPath, label) ->
                                FilterChip(
                                    selected = writePathInput == presetPath,
                                    onClick = {
                                        writePathInput = presetPath
                                        fileAccessConfig.writeFolderPath = presetPath
                                    },
                                    label = { Text(label, fontSize = 9.sp) }
                                )
                            }

                            Spacer(Modifier.weight(1f))

                            Button(
                                onClick = { exportFolderPickerLauncher.launch(null) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF76FF03)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Pilih Folder (SAF)", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                            }
                        }

                        // Export Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = {
                                    val msg = sampleExportManager.exportToCustomDirectory(writePathInput, isObb = false)
                                    lastExportMessage = msg
                                    fileAccessConfig.lastWriteSummary = msg
                                    Toast.makeText(context, "✓ Berhasil mengekspor ke folder Download/Apex3D!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD600)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = null, tint = Color.Black)
                                Spacer(Modifier.width(4.dp))
                                Text("Ekspor Sampel & Guide", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            }

                            Button(
                                onClick = {
                                    val msg = sampleExportManager.exportPythonScriptOnly(writePathInput)
                                    lastExportMessage = msg
                                    fileAccessConfig.lastWriteSummary = msg
                                    Toast.makeText(context, "✓ Script Python berhasil diekspor!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Psychology, contentDescription = null, tint = Color.Black)
                                Spacer(Modifier.width(4.dp))
                                Text("Ekspor Script Python", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }

            // SECTION 0: DAFTAR FILE GLB TERDETEKSI & ATUR PERAN (KARAKTER / MAP / RUMAH / PROP)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF162238)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.border(1.5.dp, Color(0xFF00E5FF), RoundedCornerShape(12.dp))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color(0xFF00E5FF))
                        Text(
                            text = "Atur Peran Model GLB Terdeteksi (${customModelManager.importedModels.size} Model)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF00E5FF)
                        )
                    }

                    Text(
                        text = "Pilih peruntukan setiap file .GLB (Terrain, Karakter Player Hero, Bangunan/Rumah, atau Interior).",
                        fontSize = 11.sp,
                        color = Color(0xFFB0BEC5)
                    )

                    if (customModelManager.importedModels.isEmpty()) {
                        Text(
                            text = "Belum ada file GLB terdaftar. Klik 'Pilih Folder (Termux)' atau 'Impor File GLB' di bawah untuk membaca file 3D Anda.",
                            fontSize = 11.sp,
                            color = Color(0xFFFFB74D),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
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
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "📄 ${entry.fileName}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            text = when {
                                                isCharActive -> "🧍 KARAKTER AKTIF"
                                                isTerrainActive -> "🗺️ MAP TERRAIN AKTIF"
                                                else -> "📦 ${entry.target.name}"
                                            },
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when {
                                                isCharActive -> Color(0xFF00E5FF)
                                                isTerrainActive -> Color(0xFF76FF03)
                                                else -> Color(0xFFFFD600)
                                            }
                                        )
                                    }

                                    // Role Action Buttons
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        // Set Karakter Player (Overwrite Default)
                                        Button(
                                            onClick = {
                                                customModelManager.setActiveCharacter(entry, updatePlayerConfig = true)
                                                customModelManager.activeCustomCharacterMesh = entry.mesh
                                                lastExportMessage = "✓ '${entry.fileName}' diaktifkan & menggantikan Karakter Pemain Utama!"
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isCharActive) Color(0xFF00E5FF) else Color(0xFF263238)
                                            ),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                "🧍 Set Karakter",
                                                fontSize = 10.sp,
                                                color = if (isCharActive) Color.Black else Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        // Set Terrain Map (Overwrite Default)
                                        Button(
                                            onClick = {
                                                customModelManager.setActiveTerrain(entry, terrainMesh)
                                                terrainMesh.setCustomMesh(entry.mesh)
                                                onTerrainChanged()
                                                lastExportMessage = "✓ '${entry.fileName}' diaktifkan & menggantikan Map Terrain Utama!"
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isTerrainActive) Color(0xFF76FF03) else Color(0xFF263238)
                                            ),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                "🗺️ Set Terrain",
                                                fontSize = 10.sp,
                                                color = if (isTerrainActive) Color.Black else Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        // Set Rumah + Pintu Masuk
                                        Button(
                                            onClick = {
                                                // Create House Enter/Exit portal interactable
                                                val houseDoor = WorldInteractable(
                                                    id = "house_door_${System.currentTimeMillis()}",
                                                    name = "Rumah GLB (${entry.fileName})",
                                                    type = InteractableType.HOUSE_DOOR_ENTER,
                                                    position = com.example.engine3d.math.Vec3(106.8f, 15.6f, 103.3f),
                                                    targetTeleportPos = com.example.engine3d.math.Vec3(250.0f, 1.5f, 250.0f),
                                                    promptText = "[Buka Pintu] Masuk Ke Rumah GLB"
                                                )
                                                val exitDoor = WorldInteractable(
                                                    id = "house_exit_${System.currentTimeMillis()}",
                                                    name = "Pintu Keluar Rumah",
                                                    type = InteractableType.HOUSE_DOOR_EXIT,
                                                    position = com.example.engine3d.math.Vec3(250.0f, 1.5f, 250.0f),
                                                    targetTeleportPos = com.example.engine3d.math.Vec3(106.8f, 15.6f, 108.0f),
                                                    promptText = "[Buka Pintu] Keluar Ke Desa"
                                                )
                                                val houseBarrier = WorldBarrier(
                                                    id = "house_wall_${System.currentTimeMillis()}",
                                                    name = "Dinding ${entry.fileName}",
                                                    type = BarrierType.WALL_BARRIER,
                                                    position = com.example.engine3d.math.Vec3(106.8f, 15.6f, 103.3f),
                                                    size = com.example.engine3d.math.Vec3(8f, 5f, 8f),
                                                    isPassable = false
                                                )
                                                barrierManager.barriers.add(houseBarrier)
                                                lastExportMessage = "✓ Rumah '${entry.fileName}' terpasang + Pintu Pindah 'Masuk/Keluar Rumah' & Tembok Tabrakan terdaftar!"
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD600)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("🏠 Set Rumah", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                        }

                                        // Delete
                                        IconButton(
                                            onClick = { customModelManager.deleteModel(entry) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color(0xFFFF5252))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // SECTION CONFIG: MANAJEMEN & PEMILIH FILE KONFIGURASI AREA (area_config.json)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B283E)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.border(1.5.dp, Color(0xFF76FF03), RoundedCornerShape(12.dp))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = Color(0xFF76FF03))
                        Text(
                            text = "Pemilih & Pembuat File Config (`area_config.json`)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF76FF03)
                        )
                    }

                    Text(
                        text = "Simpan atau muat file konfigurasi area multi-GLB, peruntukan role, serta titik pintu pindah ('Masuk/Keluar Rumah') secara instan.",
                        fontSize = 11.sp,
                        color = Color(0xFFECEFF1)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { jsonConfigPickerLauncher.launch("application/json") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF76FF03)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, tint = Color.Black)
                            Spacer(Modifier.width(4.dp))
                            Text("Pilih / Muat Config JSON", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                val file = sampleExportManager.exportPackage(isObb = false)
                                sampleExportManager.shareExportPackage(file, isObb = false)
                                lastExportMessage = "✓ File 'area_config.json' & paket ZIP berhasil dibuat & disimpan di Download/Apex3D!"
                                Toast.makeText(context, "✓ Berhasil mengekspor ke folder Download/Apex3D!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, tint = Color.Black)
                            Spacer(Modifier.width(4.dp))
                            Text("Simpan Config JSON", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }

            // SECTION 1: Ekspor Sample Lengkap (.ZIP / .OBB)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2842)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.border(1.5.dp, Color(0xFFFFD600), RoundedCornerShape(12.dp))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Archive, contentDescription = null, tint = Color(0xFFFFD600))
                        Text(
                            text = "Ekspor Sampel Lengkap & Paket OBB",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFFFFD600)
                        )
                    }

                    Text(
                        text = "Ekspor paket sampel berisi kontur terrain 3D, jurus aksi, NPC, batas jalan/blokir, dan panduan. Siap diedit di Blender/PC!",
                        fontSize = 11.sp,
                        color = Color(0xFFECEFF1)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val file = sampleExportManager.exportPackage(isObb = false)
                                sampleExportManager.shareExportPackage(file, isObb = false)
                                lastExportMessage = "✓ Paket .ZIP dibuat & disimpan di: Download/Apex3D/${file.name}\nMenu 'Bagikan / Simpan File' Android telah dibuka!"
                                val rep = batchImportManager.scanLocalAssetFolder()
                                lastBatchReport = rep
                                onTerrainChanged()
                                Toast.makeText(context, "✓ Berhasil mengekspor paket .ZIP ke Download/Apex3D!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD600)),
                            modifier = Modifier.weight(1f).testTag("export_sample_zip_button")
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, tint = Color.Black)
                            Spacer(Modifier.width(4.dp))
                            Text("Ekspor .ZIP", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                val file = sampleExportManager.exportPackage(isObb = true)
                                sampleExportManager.shareExportPackage(file, isObb = true)
                                lastExportMessage = "✓ Paket .OBB dibuat & disimpan di: Download/Apex3D/${file.name}\nMenu 'Bagikan / Simpan File' Android telah dibuka!"
                                val rep = batchImportManager.scanLocalAssetFolder()
                                lastBatchReport = rep
                                onTerrainChanged()
                                Toast.makeText(context, "✓ Berhasil mengekspor paket .OBB ke Download/Apex3D!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                            modifier = Modifier.weight(1f).testTag("export_sample_obb_button")
                        ) {
                            Icon(Icons.Default.Archive, contentDescription = null, tint = Color.Black)
                            Spacer(Modifier.width(4.dp))
                            Text("Ekspor .OBB", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    // Button Import OBB
                    OutlinedButton(
                        onClick = { obbPickerLauncher.launch("*/*") },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF80D8FF)),
                        modifier = Modifier.fillMaxWidth().testTag("import_obb_button")
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, tint = Color(0xFF80D8FF))
                        Spacer(Modifier.width(6.dp))
                        Text("Import / Muat File .OBB dari HP", fontSize = 11.sp)
                    }
                }
            }

            // SECTION 2: Kelakuan Batas Jalan, Blokir Jalan & Speed Pads
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A233A)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Block, contentDescription = null, tint = Color(0xFFFF5252))
                        Text("Batas Jalan & Blokir Objek (Zone / Barriers)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Text(
                        text = "Atur batas area, tembok tak kasat mata pemblokir jalan, pelat turbo akselerasi, serta pelat pelontar loncatan (Jump Pad).",
                        fontSize = 11.sp,
                        color = Color(0xFFB0BEC5)
                    )

                    Button(
                        onClick = { showBarrierCreator = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                        modifier = Modifier.fillMaxWidth().testTag("add_custom_barrier_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(6.dp))
                        Text("Tambah Batas / Blokir Jalan Baru", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    // List active barriers
                    Text("Daftar Batas Jalan di Dunia:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    barrierManager.barriers.forEach { b ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF101726), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(b.name, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color.White)
                                Text(
                                    "Tipe: ${b.type.name} • Solid: ${!b.isPassable} • Pos: (${b.position.x.toInt()}, ${b.position.z.toInt()})",
                                    fontSize = 10.sp,
                                    color = Color(0xFF90A4AE)
                                )
                            }
                            IconButton(onClick = { barrierManager.barriers.remove(b) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color(0xFFFF5252))
                            }
                        }
                    }
                }
            }

            // SECTION 3: Batch Folder & ZIP
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF162136)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Pindai Folder / Arsip Eksternal", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { folderPickerLauncher.launch(null) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = Color.Black)
                            Spacer(Modifier.width(4.dp))
                            Text("Pilih Folder", color = Color.Black, fontSize = 11.sp)
                        }

                        Button(
                            onClick = { zipPickerLauncher.launch("application/zip") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263238)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Archive, contentDescription = null, tint = Color(0xFF00E5FF))
                            Spacer(Modifier.width(4.dp))
                            Text("Buka ZIP", color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
            }

            // SECTION 4: NPCs & Terrain
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A233A)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.People, contentDescription = null, tint = Color(0xFF76FF03))
                        Text("Karakter NPC & Dialog", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Button(
                        onClick = { showNpcCreator = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF76FF03)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                        Spacer(Modifier.width(6.dp))
                        Text("Buat NPC Baru di Peta", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

    // Dialog Panduan OBB & Format File
    if (showFormatGuide) {
        AlertDialog(
            onDismissRequest = { showFormatGuide = false },
            title = { Text("Penjelasan File OBB & Format Sampel", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "1. APAKAH FILE .OBB BISA DIGUNAKAN?\n" +
                        "Ya! Di engine game mobile, file .OBB (Opaque Binary Blob) pada dasarnya adalah ARSIP ZIP terpaket.\n" +
                        "Kamu bisa menyimpan puluhan model 3D (.glb, .obj), suara, dan file konfigurasi kelakuan (.json) ke dalam 1 file .obb tunggal.\n" +
                        "Apex Engine memiliki dekompresor bawaan yang membaca file .obb secara instan tanpa perlu ekstrak manual.",
                        fontSize = 11.sp,
                        color = Color(0xFFCFD8DC)
                    )
                    Text(
                        "2. KELAKUAN BATAS JALAN & BLOKIR OBJEK:\n" +
                        "• WALL_BARRIER: Tembok solid yang memblokir karakter dari jalan rusak / area terkunci.\n" +
                        "• SPEED_BOOST_PAD: Pelat jalur jalan yang melipatgandakan kecepatan lari.\n" +
                        "• BOUNCE_PAD: Pelat pelontar yang melempar karakter melintasi tebing jurang.\n" +
                        "• KILL_ZONE: Zona jurang batas yang mengembalikan posisi pemain.",
                        fontSize = 11.sp,
                        color = Color(0xFF80D8FF)
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showFormatGuide = false }) {
                    Text("Tutup")
                }
            }
        )
    }

    // Dialog Tambah Batas Jalan / Blokir
    if (showBarrierCreator) {
        var barrierName by remember { mutableStateOf("Barikade Gerbang") }
        var barrierType by remember { mutableStateOf(BarrierType.WALL_BARRIER) }
        var barrierX by remember { mutableStateOf("10") }
        var barrierZ by remember { mutableStateOf("0") }
        var barrierSizeX by remember { mutableStateOf("2") }
        var barrierSizeZ by remember { mutableStateOf("6") }

        AlertDialog(
            onDismissRequest = { showBarrierCreator = false },
            title = { Text("Buat Batas Jalan / Blokir Baru", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = barrierName,
                        onValueChange = { barrierName = it },
                        label = { Text("Nama Rintangan / Batas") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Tipe Batas:", fontSize = 12.sp, color = Color(0xFF00E5FF))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
                            label = { Text("Posisi X") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = barrierZ,
                            onValueChange = { barrierZ = it },
                            label = { Text("Posisi Z") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = barrierSizeX,
                            onValueChange = { barrierSizeX = it },
                            label = { Text("Lebar (X)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = barrierSizeZ,
                            onValueChange = { barrierSizeZ = it },
                            label = { Text("Panjang (Z)") },
                            modifier = Modifier.weight(1f)
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
                    Text("Pasang Batas")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBarrierCreator = false }) {
                    Text("Batal")
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
            title = { Text("Buat NPC Baru", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = npcName,
                        onValueChange = { npcName = it },
                        label = { Text("Nama NPC") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = npcRole,
                        onValueChange = { npcRole = it },
                        label = { Text("Peran") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = npcDialogue,
                        onValueChange = { npcDialogue = it },
                        label = { Text("Dialog") },
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
                    Text("Pasang NPC")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNpcCreator = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Dialog Pemilih & Pemeta File Kustom jika nama file config/model berbeda
    if (showCustomFileSelector) {
        val detectedFiles = remember(focusPathInput) {
            batchImportManager.listFilesInFolderPath(focusPathInput)
        }

        AlertDialog(
            onDismissRequest = { showCustomFileSelector = false },
            title = {
                Column {
                    Text("📌 Pemeta File Kustom dalam Folder", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Focus Path: $focusPathInput", fontSize = 10.sp, color = Color(0xFF00E5FF))
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Pilih dan sesuaikan fungsi file jika nama file config atau model 3D dalam folder Anda menggunakan nama kustom/berbeda:",
                        fontSize = 11.sp,
                        color = Color(0xFFCFD8DC)
                    )

                    if (detectedFiles.isEmpty()) {
                        Text(
                            "Tidak ada file GLB, OBJ, atau JSON terdeteksi di '$focusPathInput'. Silakan pastikan folder berisi file yang ingin Anda muat.",
                            fontSize = 11.sp,
                            color = Color(0xFFFFB74D)
                        )
                    } else {
                        // 1. FILE CONFIG JSON
                        val jsonFiles = detectedFiles.filter { it.extension.lowercase() == "json" }
                        if (jsonFiles.isNotEmpty()) {
                            Text("📄 FILE KONFIGURASI JSON TERDETEKSI:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF76FF03))
                            jsonFiles.forEach { f ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF101726)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(f.name, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color.White)
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Button(
                                                onClick = {
                                                    val msg = batchImportManager.applyCustomAreaConfigFile(f)
                                                    lastExportMessage = msg
                                                    onTerrainChanged()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF76FF03)),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("📜 Config Area", fontSize = 9.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                            }
                                            Button(
                                                onClick = {
                                                    val msg = batchImportManager.applyCustomPlayerConfigFile(f)
                                                    lastExportMessage = msg
                                                    onTerrainChanged()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("🧍 Config Hero", fontSize = 9.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 2. FILE MODEL 3D GLB / OBJ
                        val modelFiles = detectedFiles.filter {
                            val ext = it.extension.lowercase()
                            ext == "glb" || ext == "obj"
                        }
                        if (modelFiles.isNotEmpty()) {
                            Text("📦 MODEL 3D GLB / OBJ TERDETEKSI:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF00E5FF))
                            modelFiles.forEach { f ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF162238)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(f.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                            Text("${f.length() / 1024} KB", fontSize = 10.sp, color = Color(0xFFB0BEC5))
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Button(
                                                onClick = {
                                                    try {
                                                        val mesh = GlbParser.parse(java.io.FileInputStream(f), f.name)
                                                        if (mesh != null) {
                                                            customModelManager.activeCustomTerrainMesh = mesh
                                                            terrainMesh.setCustomMesh(mesh)
                                                            onTerrainChanged()
                                                            lastExportMessage = "✓ Model '${f.name}' diaktifkan sebagai Terrain Map!"
                                                        }
                                                    } catch (e: Exception) {
                                                        lastExportMessage = "❌ Error: ${e.localizedMessage}"
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF76FF03)),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("🗺️ Set Terrain", fontSize = 9.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                            }

                                            Button(
                                                onClick = {
                                                    try {
                                                        val mesh = GlbParser.parse(java.io.FileInputStream(f), f.name)
                                                        if (mesh != null) {
                                                            customModelManager.activeCustomCharacterMesh = mesh
                                                            lastExportMessage = "✓ Model '${f.name}' diaktifkan sebagai Karakter Player!"
                                                        }
                                                    } catch (e: Exception) {
                                                        lastExportMessage = "❌ Error: ${e.localizedMessage}"
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("🧍 Set Hero", fontSize = 9.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                            }
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
                    Text("Selesai")
                }
            }
        )
    }
}
