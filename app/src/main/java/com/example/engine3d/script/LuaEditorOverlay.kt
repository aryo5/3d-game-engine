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
    luaEngine: LuaEngine,
    modifier: Modifier = Modifier
) {
    var isOpen by remember { mutableStateOf(false) }
    var scriptText by remember { mutableStateOf(luaEngine.currentScript) }
    val logs = luaEngine.logs

    Box(modifier = modifier.fillMaxSize()) {
        if (!isOpen) {
            FloatingActionButton(
                onClick = { isOpen = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = Color(0xFF478CBF)
            ) {
                Text("📜 Lua", color = Color.White)
            }
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.85f)
                    .align(Alignment.Center),
                colors = CardDefaults.cardColors(containerColor = Color(0xEE1E1E24))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🎮 Godot Lua Script Editor", color = Color(0xFF61AFEF), fontSize = 16.sp)
                        IconButton(onClick = { isOpen = false }) {
                            Text("✕", color = Color.White)
                        }
                    }

                    OutlinedTextField(
                        value = scriptText,
                        onValueChange = { scriptText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = Color(0xFFECEFF4)
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { luaEngine.runScript(scriptText) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF98C379))
                        ) {
                            Text("▶ Hot Reload", color = Color.Black)
                        }
                        Button(
                            onClick = { luaEngine.clearLogs() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4B5263))
                        ) {
                            Text("Clear")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                            .background(Color(0xFF16181D))
                            .padding(6.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = logs.ifEmpty { "[System] Siap menerima log dari print()." },
                            color = Color(0xFFE5C07B),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
