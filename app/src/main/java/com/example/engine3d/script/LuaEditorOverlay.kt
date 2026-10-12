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

        LaunchedEffect(ProjectManager.activeProject) {
            scriptText = ProjectManager.readActiveScript()
        }

        // 4 TEMPLATE SAMPLE SCRIPT GODOT BERDASARKAN ARSITEKTUR RINGAN
        val templates = listOf(
            "1. Player Selective" to """-- SAMPLE 1: PLAYER CONTROLLER (Selective Queue)
-- Hanya player yang aktif di frame loop, objek lain tidur!

function _ready()
    print("[Player] Node aktif! Mendaftarkan ke Active Queue.")
    player:set_process(true)
    
    -- Tangkap sinyal lompat
    player:connect("on_jump", function()
        print("[Sinyal] Karakter melompat ke udara!")
    end)
end

function _process(delta)
    -- Translasi / rotasi halus
    player:rotate_y(20.0 * delta)
end""",

            "2. Peti Interaktif (0% CPU)" to """-- SAMPLE 2: OBJEK EVENT-DRIVEN (0% CPU saat diam!)
-- Cocok untuk 100+ NPC, Peti, dan Pintu tanpa bikin lag.

function _ready()
    player:set_process(false) -- TIDAK memakan CPU per frame!
    print("[Peti Harta] Siap di map, status: TIDUR (0% CPU).")
    
    -- Bangun HANYA saat ada kejadian (Signal)
    player:connect("on_interact", function(item)
        print("[Sinyal Event] Peti dibuka!")
        UI:show_message("🎁 Anda mendapatkan 100 Koin Emas!")
    end)
end

-- Tombol uji coba untuk memicu sinyal
UI:clear()
UI:add_button("🎁 Buka Peti (Kirim Sinyal)", function()
    player:emit_signal("on_interact", "Peti Emas")
end)""",

            "3. Siang Malam (Throttled)" to """-- SAMPLE 3: SIKLUS WAKTU HEMAT DAYA (Tick Throttled)
-- Update tiap 0.5 detik saja, bukan 60 FPS, hemat baterai!

local jam = 12.0
local interval = 0.5
local elapsed = 0.0

function _ready()
    player:set_process(true)
    print("[World] Siklus waktu aktif dengan Tick Throttling (0.5s).")
end

function _process(delta)
    elapsed = elapsed + delta
    if elapsed >= interval then
        elapsed = 0.0
        jam = (jam + 0.2) % 24.0
        world:set_time(jam)
        if jam >= 6.0 and jam < 18.0 then
            world:set_sun_intensity(1.0)
        else
            world:set_sun_intensity(0.2)
        end
    end
end""",

            "4. Dynamic Controls" to """-- SAMPLE 4: KONTROL DINAMIS REAKTIF (HUD + Signals)
UI:clear()

UI:add_button("🚀 LOMPAT FISIK", function()
    player:jump()
end)

UI:add_button("⚔️ ENERGY SLASH", function()
    player:slash()
end)

UI:add_button("💨 CYBER DASH", function()
    player:dash()
end)

UI:add_slider("Kecepatan", 0.5, 4.0, 1.0, function(val)
    player:set_speed(val)
end)"""
        )

        Box(modifier = modifier.fillMaxSize().zIndex(9999f)) {
            // A. TOMBOL DINAMIS DI LAYAR GAME
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
                                android.util.Log.e("Apex3D-Lua", "Error click: ${e.message}")
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

            // B. TOMBOL PEMBUKA STUDIO (POJOK KANAN ATAS)
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
                                Text("📁 Proyek: ${ProjectManager.activeProject?.name ?: "Harvest 3D"} | ⚡ Active Queue: ${SceneTreeManager.tree.getActiveCount()} Node", color = Color(0xFF98C379), fontSize = 10.sp, fontWeight = FontWeight.Bold)
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

                            // 6. Script Editor & Presets (Daftar 4 Contoh Script Ringan)
                            AccordionGroup("📜 Studio Script & Sample Presets (Godot Style)", expEditor, { expEditor = !expEditor }) {
                                Text("Pilih salah satu dari 4 contoh arsitektur ringan:", color = Color(0xFFE5C07B), fontSize = 9.sp)
                                Spacer(modifier = Modifier.height(4.dp))

                                // Tombol 4 Template Sample
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    templates.forEach { (nama, code) ->
                                        OutlinedButton(
                                            onClick = { scriptText = code },
                                            modifier = Modifier.weight(1f).height(30.dp),
                                            contentPadding = PaddingValues(1.dp)
                                        ) {
                                            Text(nama, fontSize = 7.5.sp, color = Color(0xFF98C379), maxLines = 1)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

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
                                    Text("▶ Simpan & Eksekusi Script", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // 7. Console Log & Efisiensi CPU
                            AccordionGroup("💻 Live Console & CPU Monitor", expConsole, { expConsole = !expConsole }) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(90.dp).background(Color(0xFF16181D)).padding(6.dp).verticalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        text = logs.ifEmpty { "[System] Arsitektur Selective Queue aktif. CPU 0% untuk objek diam." },
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
