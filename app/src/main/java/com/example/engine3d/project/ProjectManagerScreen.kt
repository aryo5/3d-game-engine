package com.example.engine3d.project

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProjectManagerDialog(
    onDismiss: () -> Unit,
    onLaunchProject: (ProjectInfo) -> Unit
) {
    val context = LocalContext.current
    var projects by remember { mutableStateOf(ProjectManager.listProjects(context)) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newProjectName by remember { mutableStateOf("") }
    var newProjectDesc by remember { mutableStateOf("") }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f),
            colors = CardDefaults.cardColors(containerColor = Color(0xF6141824)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.5.dp, Color(0xFF38BDF8))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("🎮 Godot Project Manager", color = Color(0xFF61AFEF), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Lokasi: Documents/Apex3D_Projects/", color = Color(0xFF98C379), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                    IconButton(onClick = onDismiss) {
                        Text("✕", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Daftar Proyek
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(projects) { project ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2330)),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF2E384D))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(project.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text(dateFormat.format(Date(project.lastModified)), color = Color(0xFFABB2BF), fontSize = 9.sp)
                                }

                                Text(project.description, color = Color(0xFF828997), fontSize = 11.sp, maxLines = 1)
                                Text("Path: ${project.folder.name}/", color = Color(0xFFE5C07B), fontSize = 10.sp, fontFamily = FontFamily.Monospace)

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(color = Color(0xFF282C34), shape = RoundedCornerShape(4.dp)) {
                                        Text("📦 ${project.modelCount} Model", color = Color(0xFF98C379), fontSize = 9.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                    Surface(color = Color(0xFF282C34), shape = RoundedCornerShape(4.dp)) {
                                        Text("📜 ${project.scriptCount} Script", color = Color(0xFF61AFEF), fontSize = 9.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                    Spacer(modifier = Modifier.weight(1f))

                                    // Tombol Buka Proyek
                                    Button(
                                        onClick = {
                                            ProjectManager.activeProject = project
                                            onLaunchProject(project)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF98C379)),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("▶ Buka Proyek", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    // Tombol Hapus Proyek
                                    IconButton(
                                        onClick = {
                                            ProjectManager.deleteProject(project.folder)
                                            projects = ProjectManager.listProjects(context)
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Text("🗑️", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Action Buttons
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier.weight(1f).height(42.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
                    ) {
                        Text("+ Buat Proyek Baru", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { projects = ProjectManager.listProjects(context) },
                        modifier = Modifier.height(42.dp),
                        border = BorderStroke(1.dp, Color(0xFFABB2BF))
                    ) {
                        Text("🔄 Scan Ulang", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }
    }

    // Dialog Input Pembuatan Proyek Baru
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Buat Proyek Godot Baru", color = Color(0xFF61AFEF), fontSize = 15.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newProjectName,
                        onValueChange = { newProjectName = it },
                        label = { Text("Nama Proyek (misal: MyGame3D)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newProjectDesc,
                        onValueChange = { newProjectDesc = it },
                        label = { Text("Deskripsi Singkat") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newProjectName.isNotBlank()) {
                            ProjectManager.createProject(context, newProjectName, newProjectDesc)
                            projects = ProjectManager.listProjects(context)
                            newProjectName = ""
                            newProjectDesc = ""
                            showCreateDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF98C379))
                ) {
                    Text("Buat Sekarang", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}
