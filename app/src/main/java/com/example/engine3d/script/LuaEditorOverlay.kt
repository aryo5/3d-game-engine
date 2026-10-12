package com.example.engine3d.script

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.engine3d.project.ProjectManager
import com.example.engine3d.project.ProjectManagerView

@Composable
fun LuaEditorOverlay(
    luaEngine: LuaEngine = LuaScriptManager.engine,
    modifier: Modifier = Modifier
) {
    MaterialTheme {
        var isOpen by remember { mutableStateOf(false) }
        var scriptText by remember { mutableStateOf(luaEngine.currentScript) }
        val logs = luaEngine.logs

        var expProjectManager by remember { mutableStateOf(false) }
        var expPlayer by remember { mutableStateOf(false) }
        var expAnim by remember { mutableStateOf(false) }
        var expCamera by remember { mutableStateOf(false) }
        var expWorld by remember { mutableStateOf(false) }
        var expUI by remember { mutableStateOf(false) }
        var expEditor by remember { mutableStateOf(true) }
        var expConsole by remember { mutableStateOf(false) }

        // Sinkronisasi teks script saat berpindah proyek aktif
        LaunchedEffect(ProjectManager.activeProject) {
            scriptText = ProjectManager.readActiveScript()
        }

        Box(modifier = modifier.fillMaxSize().zIndex(9999f)) {
            // A. TOMBOL KUSTOM DI HUD
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 14.dp)
                    .zIndex(9995f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Apex3DRealHook.customButtons.forEach { btn ->
                    Button(
                        onClick = {
                            try { btn.callback.call() } catch (e: Throwable) {
                                android.util.Log.e("Apex3D-Lua", "Error: ${e.message}")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xEEFF9800),
                            contentColor = Color.White
                        ),
                        border = BorderStroke(1.5.dp, Color(0xFFFFD54F)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(btn.label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Apex3DRealHook.customSliders.forEach { slider ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xDD1E293B)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.width(160.dp).padding(vertical = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(6.dp)) {
                            Text("${slider.label}: ${String.format("%.1f", slider.value)}", color = Color.White, fontSize = 9.sp)
                            Slider(
                                value = slider.value,
                                onValueChange = { newVal ->
                                    slider.value = newVal
                                    try { slider.callback.call(org.luaj.vm2.LuaValue.valueOf(newVal.toDouble())) } catch (_: Throwable) {}
                                },
                                valueRange = slider.min..slider.max,
                                modifier = Modifier.height(20.dp)
                            )
                        }
                    }
                }
            }

            // B. TOMBOL PEMBUKA STUDIO
            if (!isOpen) {
                Button(
                    onClick = { isOpen = true },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 54.dp, end = 14.dp)
                        .zIndex(9999f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xEE1E293B),
                        contentColor = Color(0xFF38BDF8)
                    ),
                    border = BorderStroke(1.5.dp, Color(0xFF38BDF8)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("⚡ LUA STUDIO", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                // C. MODAL STUDIO ACCORDION
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .fillMaxHeight(0.88f)
                        .align(Alignment.Center)
                        .zIndex(10000f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xF414171E)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("🎮 Godot Engine Inspector & Studio", color = Color(0xFF61AFEF), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("📁 Proyek Aktif: ${ProjectManager.activeProject?.name ?: "Harvest 3D"}", color = Color(0xFF98C379), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            IconButton(onClick = { isOpen = false }, modifier = Modifier.size(28.dp)) {
                                Text("✕", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // 0. GODOT PROJECT MANAGER (DOCUMENTS)
                            AccordionGroup("📁 Godot Project Manager (Folder Documents)", expProjectManager, { expProjectManager = !expProjectManager }) {
                                ProjectManagerView()
                            }

                            // 1. Karakter & Fisika
                            AccordionGroup("🎮 Karakter & Fisika (Player & Physics)", expPlayer, { expPlayer = !expPlayer }) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(onClick = { LuaScriptManager.player.jump() }, modifier = Modifier.weight(1f).height(34.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF478CBF))) {
                                        Text("Lompat Fisik", fontSize = 10.sp)
                                    }
                                    Button(onClick = { LuaScriptManager.player.reset() }, modifier = Modifier.weight(1f).height(34.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF282C34))) {
                                        Text("Reset Posisi", fontSize = 10.sp)
                                    }
                                }
                            }

                            // 2. Jurus & Aksi
                            AccordionGroup("⚔️ Jurus & Aksi (ActionManager)", expAnim, { expAnim = !expAnim }) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    OutlinedButton(onClick = { LuaScriptManager.player.slash() }, modifier = Modifier.weight(1f).height(32.dp), contentPadding = PaddingValues(2.dp)) {
                                        Text("Slash", fontSize = 9.sp, color = Color(0xFF00E5FF))
                                    }
                                    OutlinedButton(onClick = { LuaScriptManager.player.slam() }, modifier = Modifier.weight(1f).height(32.dp), contentPadding = PaddingValues(2.dp)) {
                                        Text("Slam", fontSize = 9.sp, color = Color(0xFFFF3D00))
                                    }
                                    OutlinedButton(onClick = { LuaScriptManager.player.dash() }, modifier = Modifier.weight(1f).height(32.dp), contentPadding = PaddingValues(2.dp)) {
                                        Text("Dash", fontSize = 9.sp, color = Color(0xFF76FF03))
                                    }
                                }
                            }

                            // 3. Kamera 3D
                            AccordionGroup("🎥 Kamera 3D (Camera3D Viewport)", expCamera, { expCamera = !expCamera }) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(onClick = { LuaScriptManager.camera.set_fov(75.0) }, modifier = Modifier.weight(1f).height(32.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF282C34))) {
                                        Text("FOV Lebar (75°)", fontSize = 10.sp)
                                    }
                                    Button(onClick = { LuaScriptManager.camera.set_fov(50.0) }, modifier = Modifier.weight(1f).height(32.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF282C34))) {
                                        Text("FOV Dekat (50°)", fontSize = 10.sp)
                                    }
                                }
                            }

                            // 4. Dunia & Waktu
                            AccordionGroup("🌍 Dunia & Waktu (World Environment)", expWorld, { expWorld = !expWorld }) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(onClick = { LuaScriptManager.world.set_time(12.0); LuaScriptManager.world.set_sun_intensity(1.0) }, modifier = Modifier.weight(1f).height(32.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF282C34))) {
                                        Text("Siang (12:00)", fontSize = 10.sp)
                                    }
                                    Button(onClick = { LuaScriptManager.world.set_time(0.0); LuaScriptManager.world.set_sun_intensity(0.2) }, modifier = Modifier.weight(1f).height(32.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF282C34))) {
                                        Text("Malam (00:00)", fontSize = 10.sp)
                                    }
                                }
                            }

                            // 5. Kontrol UI Dinamis
                            AccordionGroup("🕹️ Kontrol UI Kustom (Dynamic Controls)", expUI, { expUI = !expUI }) {
                                Text("Tombol aktif di layar: ${Apex3DRealHook.customButtons.size} | Slider: ${Apex3DRealHook.customSliders.size}", color = Color(0xFFABB2BF), fontSize = 10.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(onClick = { Apex3DRealHook.clearCustomUI() }, modifier = Modifier.fillMaxWidth().height(32.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD9534F))) {
                                    Text("Bersihkan Semua Tombol Kustom di Layar", fontSize = 10.sp)
                                }
                            }

                            // 6. Script Editor & Presets (Tersinkronisasi ke File Documents)
                            AccordionGroup("📜 Studio Script & Presets (Live Sync Documents)", expEditor, { expEditor = !expEditor }) {
                                Text(
                                    "File aktif: Documents/Apex3D_Projects/${ProjectManager.activeProject?.folder?.name ?: "Proyek_Harvest_3D"}/scripts/player_controller.lua",
                                    color = Color(0xFFE5C07B),
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = scriptText,
                                    onValueChange = { scriptText = it },
                                    modifier = Modifier.fillMaxWidth().height(160.dp),
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = Color(0xFFECEFF4)
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = {
                                        luaEngine.runScript(scriptText)
                                        expConsole = true
                                    },
                                    modifier = Modifier.fillMaxWidth().height(38.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF98C379))
                                ) {
                                    Text("▶ Simpan ke Documents & Hot Reload", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // 7. Console Log
                            AccordionGroup("💻 Live Console & Debugger", expConsole, { expConsole = !expConsole }) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(90.dp).background(Color(0xFF16181D)).padding(6.dp).verticalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        text = logs.ifEmpty { "[System] Siap menerima log dari print()." },
                                        color = Color(0xFF98C379),
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(onClick = { luaEngine.clearLogs() }, modifier = Modifier.fillMaxWidth().height(30.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4B5263))) {
                                    Text("Bersihkan Log Console", fontSize = 10.sp)
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
fun AccordionGroup(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E222B)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, color = Color(0xFF61AFEF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(if (isExpanded) "▲" else "▼", color = Color(0xFFABB2BF), fontSize = 11.sp)
            }
            if (isExpanded) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)) {
                    content()
                }
            }
        }
    }
}
