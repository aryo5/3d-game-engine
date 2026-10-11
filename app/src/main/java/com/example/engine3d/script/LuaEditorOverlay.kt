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

@Composable
fun LuaEditorOverlay(
    luaEngine: LuaEngine = LuaScriptManager.engine,
    modifier: Modifier = Modifier
) {
    MaterialTheme {
        var isOpen by remember { mutableStateOf(false) }
        var scriptText by remember { mutableStateOf(luaEngine.currentScript) }
        val logs = luaEngine.logs

        var expPlayer by remember { mutableStateOf(false) }
        var expAnim by remember { mutableStateOf(false) }
        var expCamera by remember { mutableStateOf(false) }
        var expWorld by remember { mutableStateOf(false) }
        var expUI by remember { mutableStateOf(false) }
        var expEditor by remember { mutableStateOf(true) }
        var expConsole by remember { mutableStateOf(false) }

        val templates = listOf(
            "Tombol Kontrol Baru" to """-- Membuat tombol kustom di layar game!
UI:clear()

UI:add_button("🚀 SUPER JUMP", function()
    player:jump()
end)

UI:add_button("⚡ SPEED BOOST", function()
    player:set_speed(3.0)
    print("Kecepatan diset ke 3.0x!")
end)

UI:add_slider("Kecepatan Lari", 0.5, 5.0, 1.0, function(val)
    player:set_speed(val)
end)""",

            "Auto Jump Loop" to """local timer = 0.0

function _ready()
    print("Auto jump diaktifkan!")
    player:jump()
end

function _process(delta)
    timer = timer + delta
    if timer >= 1.5 then
        player:jump()
        timer = 0.0
    end
end""",

            "Slow Motion Matrix" to """function _ready()
    print("Efek Slow Motion Matrix aktif!")
    Engine:set_time_scale(0.3)
end""",

            "Siklus Siang Malam" to """local jam = 6.0

function _process(delta)
    jam = (jam + (0.5 * delta)) % 24.0
    world:set_time(jam)
    if jam >= 6.0 and jam < 18.0 then
        world:set_sun_intensity(1.0)
    else
        world:set_sun_intensity(0.2)
    end
end"""
        )

        Box(modifier = modifier.fillMaxSize().zIndex(9999f)) {
            // A. TOMBOL DAN SLIDER KUSTOM YANG DIBUAT DARI LUA MUNCUL DI SINI (KIRI LAYAR)
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

            // B. TOMBOL PEMBUKA STUDIO (POJOK KANAN ATAS DI BAWAH GRAFIS)
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
                // C. MODAL STUDIO UTAMA ACCORDION EXPANDED
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
                            Text("🎮 Godot Engine Inspector & Studio", color = Color(0xFF61AFEF), fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
                            AccordionGroup("🎮 Karakter & Fisika (Player & Physics)", expPlayer, { expPlayer = !expPlayer }) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(onClick = { LuaScriptManager.player.jump() }, modifier = Modifier.weight(1f).height(34.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF478CBF))) {
                                        Text("Lompat Fisik", fontSize = 10.sp)
                                    }
                                    Button(onClick = { LuaScriptManager.player.set_speed(2.0) }, modifier = Modifier.weight(1f).height(34.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF282C34))) {
                                        Text("Speed 2.0x", fontSize = 10.sp)
                                    }
                                }
                            }

                            AccordionGroup("🎬 Animasi & Pose (AnimationPlayer)", expAnim, { expAnim = !expAnim }) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf("Idle", "Walk", "Run", "Slash").forEach { animName ->
                                        OutlinedButton(onClick = { LuaScriptManager.player.play_animation(animName) }, modifier = Modifier.weight(1f).height(32.dp), contentPadding = PaddingValues(2.dp)) {
                                            Text(animName, fontSize = 9.sp, color = Color(0xFF98C379))
                                        }
                                    }
                                }
                            }

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

                            AccordionGroup("🕹️ Kontrol UI Kustom (Dynamic Controls)", expUI, { expUI = !expUI }) {
                                Text("Total tombol kustom aktif: ${Apex3DRealHook.customButtons.size} | Slider: ${Apex3DRealHook.customSliders.size}", color = Color(0xFFABB2BF), fontSize = 10.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(onClick = { Apex3DRealHook.clearCustomUI() }, modifier = Modifier.fillMaxWidth().height(32.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD9534F))) {
                                    Text("Bersihkan Semua Tombol Kustom di Layar", fontSize = 10.sp)
                                }
                            }

                            AccordionGroup("📜 Studio Script & Presets (Godot Lua)", expEditor, { expEditor = !expEditor }) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    templates.forEach { (nama, code) ->
                                        OutlinedButton(onClick = { scriptText = code }, modifier = Modifier.weight(1f).height(28.dp), contentPadding = PaddingValues(2.dp)) {
                                            Text(nama, fontSize = 8.sp, color = Color(0xFF98C379))
                                        }
                                    }
                                }
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
                                    Text("▶ Hot Reload & Eksekusi Script", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

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
