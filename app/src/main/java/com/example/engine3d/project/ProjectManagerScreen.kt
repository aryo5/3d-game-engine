package com.example.engine3d.project

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
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
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProjectManagerView(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var projects by remember { mutableStateOf(ProjectManager.listProjects(context)) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newProjectName by remember { mutableStateOf("") }
    var newProjectDesc by remember { mutableStateOf("") }

    val dateFormat = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()) }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Status Folder Induk
        Text("📁 Lokasi: Documents/Apex3D_Projects/", color = Color(0xFF98C379), fontSize = 10.sp, fontFamily = FontFamily.Monospace)

        // Daftar Kartu Proyek
        projects.forEach { project ->
            val isActive = ProjectManager.activeProject?.folder?.path == project.folder.path
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = if (isActive) Color(0xFF1E2E3D) else Color(0xFF1E2330)),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, if (isActive) Color(0xFF38BDF8) else Color(0xFF2E384D))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(project.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(dateFormat.format(Date(project.lastModified)), color = Color(0xFF828997), fontSize = 9.sp)
                    }

                    Text("Folder: ${project.folder.name}/", color = Color(0xFFE5C07B), fontSize = 9.sp, fontFamily = FontFamily.Monospace)

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(color = Color(0xFF282C34), shape = RoundedCornerShape(4.dp)) {
                            Text("📦 ${project.modelCount} Model", color = Color(0xFF98C379), fontSize = 8.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                        Surface(color = Color(0xFF282C34), shape = RoundedCornerShape(4.dp)) {
                            Text("📜 ${project.scriptCount} Script", color = Color(0xFF61AFEF), fontSize = 8.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                        Spacer(modifier = Modifier.weight(1f))

                        Button(
                            onClick = {
                                ProjectManager.activeProject = project
                                Toast.makeText(context, "Proyek [${project.name}] Aktif!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isActive) Color(0xFF478CBF) else Color(0xFF98C379)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(if (isActive) "✔ Aktif" else "▶ Pilih", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        IconButton(
                            onClick = {
                                ProjectManager.deleteProject(project.folder)
                                projects = ProjectManager.listProjects(context)
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Text("🗑️", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Tombol Aksi Bawah
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(
                onClick = { showCreateDialog = true },
                modifier = Modifier.weight(1f).height(34.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
            ) {
                Text("+ Buat Proyek Baru", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = { projects = ProjectManager.listProjects(context) },
                modifier = Modifier.height(34.dp),
                border = BorderStroke(1.dp, Color(0xFFABB2BF))
            ) {
                Text("🔄 Scan", color = Color.White, fontSize = 11.sp)
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Buat Proyek Godot Baru", color = Color(0xFF61AFEF), fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = newProjectName,
                        onValueChange = { newProjectName = it },
                        label = { Text("Nama Proyek") },
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
                    Text("Buat", color = Color.Black, fontWeight = FontWeight.Bold)
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
