package com.example.engine3d.ui

import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.engine3d.importer.StoragePermissionHelper
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InAppFolderPickerDialog(
    initialPath: String,
    onFolderSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    // Default to /sdcard (External Storage) or Downloads or App's internal dir
    val defaultRoot = remember {
        val ext = Environment.getExternalStorageDirectory()
        if (ext.exists() && ext.canRead()) ext else context.filesDir
    }

    var currentDir by remember {
        mutableStateOf(
            if (initialPath.isNotBlank() && File(initialPath).exists()) File(initialPath) else defaultRoot
        )
    }

    var filesList by remember { mutableStateOf(emptyList<File>()) }
    var searchQuery by remember { mutableStateOf("") }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var allFilesAccessGranted by remember {
        mutableStateOf(StoragePermissionHelper.hasAllFilesAccess(context))
    }

    // Refresh files list helper
    val refreshFiles = {
        try {
            val list = currentDir.listFiles()?.toList() ?: emptyList()
            // Sort: Directories first, then files alphabetically
            filesList = list.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
        } catch (e: Exception) {
            filesList = emptyList()
        }
    }

    // Refresh files when directory or permissions change
    LaunchedEffect(currentDir, allFilesAccessGranted) {
        refreshFiles()
    }

    // Poll permission status on return to app
    DisposableEffect(Unit) {
        allFilesAccessGranted = StoragePermissionHelper.hasAllFilesAccess(context)
        onDispose {}
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0C1322)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Title and Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color(0xFF76FF03))
                        Text(
                            text = "Pilih Folder Aset (In-App Browser)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                    }
                }

                // Storage Permission Alert for Android 11+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !allFilesAccessGranted) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF3E2723)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFFFF5252))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Akses File Terbatas",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFFFF8A80)
                                )
                                Text(
                                    "Aktifkan 'Akses Semua File' agar aplikasi dapat memindai GLB di luar folder sandbox aplikasi.",
                                    fontSize = 10.sp,
                                    color = Color.White
                                )
                            }
                            Button(
                                onClick = {
                                    StoragePermissionHelper.launchAllFilesAccessSettings(context)
                                    // Trigger refresh after launch
                                    allFilesAccessGranted = StoragePermissionHelper.hasAllFilesAccess(context)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF76FF03)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("Izinkan", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Breadcrumbs & Navigation Path
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF16233B), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val parent = currentDir.parentFile
                            if (parent != null && parent.exists() && parent.canRead()) {
                                currentDir = parent
                            } else {
                                Toast.makeText(context, "Sudah mencapai folder teratas!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Color(0xFF00E5FF))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = currentDir.absolutePath,
                        fontSize = 11.sp,
                        color = Color(0xFFECEFF1),
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Quick Directory Jump Shortcuts (Fast path selection without deep folder traversal)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val quickDirs = listOf(
                        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Apex3D") to "Apex3D",
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) to "Downloads",
                        File("/sdcard/Download/Termux_GLB") to "Termux",
                        File(context.filesDir, "ApexAssets") to "Internal"
                    )
                    quickDirs.forEach { (dir, label) ->
                        val exists = dir.exists()
                        FilterChip(
                            selected = currentDir.absolutePath == dir.absolutePath,
                            onClick = {
                                if (!dir.exists()) {
                                    dir.mkdirs()
                                }
                                currentDir = dir
                                searchQuery = ""
                            },
                            label = { Text(label, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF76FF03),
                                selectedLabelColor = Color.Black,
                                containerColor = Color(0xFF16233B),
                                labelColor = Color.White
                            )
                        )
                    }
                }

                // Search Bar and Create Folder Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Cari Folder / File...", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00E5FF),
                            unfocusedBorderColor = Color(0xFF263238),
                            focusedLabelColor = Color(0xFF00E5FF)
                        )
                    )

                    Button(
                        onClick = { showCreateFolderDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263238)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.height(54.dp)
                    ) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = "Buat Folder", tint = Color(0xFF76FF03))
                    }
                }

                // Files and Folders List (LazyColumn ensures high performance with thousands of items)
                val filteredList = filesList.filter {
                    it.name.contains(searchQuery, ignoreCase = true)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFF090D1A), RoundedCornerShape(10.dp))
                        .padding(4.dp)
                ) {
                    if (filteredList.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Folder Kosong atau Tidak Dapat Diakses",
                                color = Color(0xFF78909C),
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            items(filteredList) { file ->
                                val isDir = file.isDirectory
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (isDir) {
                                                if (file.canRead()) {
                                                    currentDir = file
                                                    searchQuery = "" // Reset search when entering a new folder
                                                } else {
                                                    Toast.makeText(context, "Folder tidak dapat dibaca!", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isDir) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                                        contentDescription = null,
                                        tint = if (isDir) Color(0xFFFFD600) else Color(0xFF90A4AE),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = file.name,
                                            color = if (isDir) Color.White else Color(0xFFCFD8DC),
                                            fontSize = 13.sp,
                                            fontWeight = if (isDir) FontWeight.Bold else FontWeight.Normal
                                        )
                                        if (!isDir) {
                                            Text(
                                                text = "${(file.length() / 1024)} KB",
                                                color = Color(0xFF78909C),
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                    if (isDir) {
                                        Icon(
                                            Icons.Default.ChevronRight,
                                            contentDescription = null,
                                            tint = Color(0xFF37474F),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Divider(color = Color(0xFF101726), thickness = 0.5.dp)
                            }
                        }
                    }
                }

                // Action Buttons Bottom
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Batal")
                    }

                    Button(
                        onClick = {
                            onFolderSelected(currentDir.absolutePath)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF76FF03)),
                        modifier = Modifier.weight(1.5f).testTag("select_current_folder_button")
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Black)
                        Spacer(Modifier.width(6.dp))
                        Text("Pilih Folder Ini", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Create New Folder Dialog
    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("Buat Folder Baru", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("Nama Folder") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            val newDir = File(currentDir, newFolderName)
                            if (!newDir.exists()) {
                                val success = newDir.mkdirs()
                                if (success) {
                                    Toast.makeText(context, "✓ Folder berhasil dibuat!", Toast.LENGTH_SHORT).show()
                                    refreshFiles()
                                } else {
                                    Toast.makeText(context, "Gagal membuat folder!", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Folder sudah ada!", Toast.LENGTH_SHORT).show()
                            }
                            newFolderName = ""
                            showCreateFolderDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF76FF03))
                ) {
                    Text("Buat", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) {
                    Text("Batal", color = Color.White)
                }
            },
            containerColor = Color(0xFF101726)
        )
    }
}
