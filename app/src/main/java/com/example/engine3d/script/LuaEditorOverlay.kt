package com.example.engine3d.script

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LuaEditorOverlay(
    luaEngine: LuaEngine = LuaScriptManager.engine,
    modifier: Modifier = Modifier
) {
    MaterialTheme {
        var isOpen by remember { mutableStateOf(false) }
        var selectedTab by remember { mutableStateOf(0) }
        var scriptText by remember { mutableStateOf(luaEngine.currentScript) }
        val logs = luaEngine.logs

        val templates = listOf(
            "Petani Berjalan" to """function _ready()
    print("Petani Harvest Moon mulai berjalan!")
    player:setPosition(0.0, 0.0, 0.0)
    player:playAnimation("Walk")
end

function _process(delta)
    player:translate(0.5 * delta, 0.0, 0.0)
end""",
            "Kamera Orbit" to """local angle = 0.0
function _ready()
    print("Kamera Orbit Aktif!")
    camera:setFov(65.0)
end

function _process(delta)
    angle = angle + (0.5 * delta)
    local x = 6.0 * math.sin(angle)
    local z = 6.0 * math.cos(angle)
    camera:setPosition(x, 3.0, z)
    camera:lookAt(player:getX(), player:getY(), player:getZ())
end""",
            "Siang Malam" to """local jam = 6.0
function _ready()
    print("Siklus Siang Malam Aktif!")
end

function _process(delta)
    jam = (jam + (0.5 * delta)) % 24.0
    world:setTime(jam)
    if jam >= 6.0 and jam < 18.0 then
        world:setSunIntensity(1.0)
    else
        world:setSunIntensity(0.2)
    end
end"""
        )

        Box(modifier = modifier.fillMaxSize()) {
            if (!isOpen) {
                FloatingActionButton(
                    onClick = { isOpen = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    containerColor = Color(0xFF478CBF)
                ) {
                    Text("📜 Lua", color = Color.White, fontSize = 14.sp)
                }
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .fillMaxHeight(0.88f)
                        .align(Alignment.Center),
                    colors = CardDefaults.cardColors(containerColor = Color(0xF0181A1F))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🎮 Godot Lua Script Studio", color = Color(0xFF61AFEF), fontSize = 15.sp)
                            IconButton(onClick = { isOpen = false }) {
                                Text("✕", color = Color.White)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("Editor", "Cheat Sheet", "Console").forEachIndexed { index, title ->
                                Button(
                                    onClick = { selectedTab = index },
                                    modifier = Modifier.weight(1f).height(36.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selectedTab == index) Color(0xFF478CBF) else Color(0xFF282C34)
                                    ),
                                    contentPadding = PaddingValues(2.dp)
                                ) {
                                    Text(title, fontSize = 11.sp, color = Color.White)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        if (selectedTab == 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                templates.forEach { (nama, code) ->
                                    OutlinedButton(
                                        onClick = { scriptText = code },
                                        modifier = Modifier.weight(1f).height(32.dp),
                                        contentPadding = PaddingValues(2.dp)
                                    ) {
                                        Text(nama, fontSize = 9.sp, color = Color(0xFF98C379))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            OutlinedTextField(
                                value = scriptText,
                                onValueChange = { scriptText = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = Color(0xFFECEFF4)
                                )
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Button(
                                onClick = {
                                    luaEngine.runScript(scriptText)
                                    selectedTab = 2
                                },
                                modifier = Modifier.fillMaxWidth().height(42.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF98C379))
                            ) {
                                Text("▶ Hot Reload / Eksekusi", color = Color.Black, fontSize = 13.sp)
                            }
                        }

                        if (selectedTab == 1) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .background(Color(0xFF21252B))
                                    .padding(8.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text("📌 DAFTAR FUNGSI (API) GODOT LUA:", color = Color(0xFFE5C07B), fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    """
• player:setPosition(x, y, z)
• player:translate(dx, dy, dz)
• player:rotateY(deg)
• player:setScale(scale)
• player:playAnimation("Idle" / "Walk")
• player:getX(), getY(), getZ()

• camera:setPosition(x, y, z)
• camera:setFov(60.0)
• camera:lookAt(x, y, z)

• world:setTime(hour) (0-24)
• world:setSunIntensity(val)
• world:setGravity(-9.8)

• print("Pesan...") -> Masuk ke tab Console
                                    """.trimIndent(),
                                    color = Color(0xFFABB2BF),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        if (selectedTab == 2) {
                            Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .background(Color(0xFF16181D))
                                        .padding(6.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        text = logs.ifEmpty { "[System] Konsol siap menerima output print()." },
                                        color = Color(0xFF98C379),
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = { luaEngine.clearLogs() },
                                    modifier = Modifier.fillMaxWidth().height(36.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4B5263))
                                ) {
                                    Text("Bersihkan Log", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
