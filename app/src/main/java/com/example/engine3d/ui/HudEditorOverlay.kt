package com.example.engine3d.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine3d.controller.*
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HudEditorOverlay(
    configs: Map<HudControlId, HudElementConfig>,
    expandedConfig: ExpandedContainerConfig = ExpandedContainerConfig(),
    onConfigsUpdated: (Map<HudControlId, HudElementConfig>) -> Unit,
    onExpandedConfigUpdated: (ExpandedContainerConfig) -> Unit = {},
    onSaveAndExit: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity

    DisposableEffect(Unit) {
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = originalOrientation
        }
    }

    var localConfigs by remember { mutableStateOf(configs.mapValues { it.value.copy() }) }
    var localExpandedConfig by remember { mutableStateOf(expandedConfig.copy()) }
    var selectedControlId by remember { mutableStateOf<HudControlId?>(HudControlId.JOYSTICK) }
    var showPresetMenu by remember { mutableStateOf(false) }
    var isDrawerMinimized by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070B14))
    ) {
        val screenW = maxWidth.value
        val screenH = maxHeight.value

        // Dark Sci-Fi Blueprint Grid
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 32.dp.toPx()
            var x = 0f
            while (x < size.width) {
                drawLine(Color(0x1000E5FF), Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                x += step
            }
            var y = 0f
            while (y < size.height) {
                drawLine(Color(0x1000E5FF), Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                y += step
            }
        }

        // Draggable HUD Elements
        localConfigs.forEach { (id, cfg) ->
            val isSelected = selectedControlId == id
            val baseSize = when (id) {
                HudControlId.JOYSTICK -> 118.dp
                HudControlId.LOOK_PAD -> 150.dp
                HudControlId.STATUS_BAR -> 132.dp
                HudControlId.PERF_MONITOR -> 115.dp
                HudControlId.MINIMAP_RADAR -> 62.dp
                HudControlId.TOP_BAR_ACTIONS -> 96.dp
                HudControlId.CROSSHAIR -> 38.dp
                HudControlId.ATTACK -> 70.dp
                HudControlId.JUMP -> 62.dp
                HudControlId.CROUCH, HudControlId.ACTION_SLAM, HudControlId.ACTION_DASH -> 56.dp
                HudControlId.INTERACT -> 66.dp
                HudControlId.EXPANDED_MENU -> 60.dp
                else -> 46.dp
            }
            val scaledSize = baseSize * cfg.scale
            val posX = (cfg.xPercent * screenW - scaledSize.value / 2f).coerceIn(0f, screenW - scaledSize.value)
            val posY = (cfg.yPercent * screenH - scaledSize.value / 2f).coerceIn(0f, screenH - scaledSize.value)

            val elementShape = when (id) {
                HudControlId.LOOK_PAD -> RoundedCornerShape(16.dp)
                HudControlId.STATUS_BAR, HudControlId.PERF_MONITOR, HudControlId.TOP_BAR_ACTIONS -> RoundedCornerShape(10.dp)
                else -> CircleShape
            }

            Box(
                modifier = Modifier
                    .offset { IntOffset(posX.dp.roundToPx(), posY.dp.roundToPx()) }
                    .size(scaledSize)
                    .alpha(if (cfg.isEnabled) cfg.alpha else 0.35f)
                    .clip(elementShape)
                    .background(
                        if (isSelected) Color(0xFF00E5FF)
                        else when (id) {
                            HudControlId.JOYSTICK -> Color(0xFF1E293B)
                            HudControlId.LOOK_PAD -> Color(0x33334155)
                            HudControlId.STATUS_BAR, HudControlId.PERF_MONITOR, HudControlId.TOP_BAR_ACTIONS -> Color(0xFF0F172A)
                            HudControlId.ATTACK -> Color(0xFFD84315)
                            HudControlId.JUMP -> Color(0xFF0277BD)
                            HudControlId.CROUCH -> Color(0xFF37474F)
                            HudControlId.ACTION_SLAM -> Color(0xFFE65100)
                            HudControlId.ACTION_DASH -> Color(0xFF2E7D32)
                            HudControlId.INTERACT -> Color(0xFF455A64)
                            HudControlId.EXPANDED_MENU -> Color(0xFFEF6C00)
                            else -> Color(0xFF1E293B)
                        }
                    )
                    .border(
                        width = if (isSelected) 3.dp else 1.2.dp,
                        color = if (isSelected) Color.White else Color(0x6600E5FF),
                        shape = elementShape
                    )
                    .pointerInput(id, screenW, screenH) {
                        detectDragGestures(
                            onDragStart = { selectedControlId = id },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val curCfg = localConfigs[id] ?: return@detectDragGestures
                                val dx = (dragAmount.x / density) / screenW
                                val dy = (dragAmount.y / density) / screenH
                                val newX = (curCfg.xPercent + dx).coerceIn(0.02f, 0.98f)
                                val newY = (curCfg.yPercent + dy).coerceIn(0.02f, 0.98f)
                                val updated = localConfigs.toMutableMap()
                                updated[id] = curCfg.copy(xPercent = newX, yPercent = newY)
                                localConfigs = updated
                                onConfigsUpdated(updated)
                            }
                        )
                    }
                    .clickable { selectedControlId = id },
                contentAlignment = Alignment.Center
            ) {
                val icon = when (id) {
                    HudControlId.SPRINT_LOCK -> Icons.AutoMirrored.Filled.DirectionsRun
                    HudControlId.JUMP -> Icons.Default.North
                    HudControlId.CROUCH -> Icons.Default.ArrowDownward
                    HudControlId.ATTACK -> Icons.Default.FlashOn
                    HudControlId.ACTION_SLAM -> Icons.Default.Terrain
                    HudControlId.ACTION_DASH -> Icons.Default.Speed
                    HudControlId.INTERACT -> Icons.AutoMirrored.Filled.Chat
                    HudControlId.EXPANDED_MENU -> Icons.Default.Apps
                    HudControlId.CAMERA_SWITCH -> Icons.Default.SwitchVideo
                    HudControlId.FLASHLIGHT -> Icons.Default.Highlight
                    HudControlId.RESET_POS -> Icons.Default.RestartAlt
                    else -> Icons.Default.Tune
                }
                val label = when (id) {
                    HudControlId.SPRINT_LOCK -> "Kunci Lari"
                    HudControlId.JUMP -> "Lompat"
                    HudControlId.CROUCH -> "Jongkok"
                    HudControlId.ATTACK -> "Slash"
                    HudControlId.ACTION_SLAM -> "Slam"
                    HudControlId.ACTION_DASH -> "Dash"
                    HudControlId.INTERACT -> "Bicara"
                    HudControlId.EXPANDED_MENU -> "Menu"
                    else -> id.displayName
                }
                val colorTint = if (isSelected) Color.Black else Color.White

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = colorTint,
                        modifier = Modifier.size(scaledSize * 0.38f)
                    )
                    Text(
                        text = label,
                        color = colorTint,
                        fontSize = (8f * (scaledSize.value / 55f)).coerceIn(6f, 11f).sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }

        // Top Header Bar
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(52.dp),
            color = Color(0xF50F172A),
            shadowElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
                    Text("Editor HUD", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    Box {
                        OutlinedButton(
                            onClick = { showPresetMenu = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E5FF)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("Preset ▾", fontSize = 11.sp)
                        }

                        DropdownMenu(
                            expanded = showPresetMenu,
                            onDismissRequest = { showPresetMenu = false }
                        ) {
                            DropdownMenuItem(text = { Text("Default 2-Finger") }, onClick = {
                                localConfigs = HudPresets.getPreset("DEFAULT_2_FINGER").mapValues { it.value.copy() }
                                onConfigsUpdated(localConfigs)
                                showPresetMenu = false
                            })
                            DropdownMenuItem(text = { Text("3-Finger Claw") }, onClick = {
                                localConfigs = HudPresets.getPreset("CLAW_3_FINGER").mapValues { it.value.copy() }
                                onConfigsUpdated(localConfigs)
                                showPresetMenu = false
                            })
                            DropdownMenuItem(text = { Text("4-Finger Pro Claw") }, onClick = {
                                localConfigs = HudPresets.getPreset("PUBG_4_FINGER_CLAW").mapValues { it.value.copy() }
                                onConfigsUpdated(localConfigs)
                                showPresetMenu = false
                            })
                        }
                    }

                    Button(
                        onClick = {
                            localConfigs = HudPresets.getPreset("DEFAULT_2_FINGER").mapValues { it.value.copy() }
                            onConfigsUpdated(localConfigs)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Reset", fontSize = 10.sp, color = Color(0xFFFFD600))
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    selectedControlId?.let { id ->
                        Text(id.displayName, color = Color(0xFF00E5FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onCancel,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Batal", fontSize = 11.sp)
                    }

                    Button(
                        onClick = onSaveAndExit,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Simpan", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }

        // Minimizeable Floating Bottom Drawer
        val selectedCfg = selectedControlId?.let { localConfigs[it] }
        if (selectedCfg != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 6.dp)
                    .fillMaxWidth(if (isDrawerMinimized) 0.35f else 0.85f),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xF20F172A),
                shadowElevation = 10.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚙️ ${selectedCfg.id.displayName}",
                            color = Color(0xFF00E5FF),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        IconButton(
                            onClick = { isDrawerMinimized = !isDrawerMinimized },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (isDrawerMinimized) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Toggle",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    if (!isDrawerMinimized) {
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            localConfigs.keys.forEach { id ->
                                val isSel = selectedControlId == id
                                FilterChip(
                                    selected = isSel,
                                    onClick = { selectedControlId = id },
                                    label = { Text(id.displayName, fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF00E5FF),
                                        selectedLabelColor = Color.Black,
                                        containerColor = Color(0xFF1E293B),
                                        labelColor = Color.White
                                    )
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Text("Ukuran:", color = Color(0xFF94A3B8), fontSize = 10.sp)
                                Slider(
                                    value = selectedCfg.scale,
                                    onValueChange = {
                                        val updated = localConfigs.toMutableMap()
                                        updated[selectedCfg.id] = selectedCfg.copy(scale = it)
                                        localConfigs = updated
                                        onConfigsUpdated(updated)
                                    },
                                    valueRange = 0.5f..1.8f,
                                    colors = SliderDefaults.colors(thumbColor = Color(0xFF00E5FF), activeTrackColor = Color(0xFF00E5FF)),
                                    modifier = Modifier.weight(1f)
                                )
                                Text("${(selectedCfg.scale * 100).roundToInt()}%", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Text("Transparan:", color = Color(0xFF94A3B8), fontSize = 10.sp)
                                Slider(
                                    value = selectedCfg.alpha,
                                    onValueChange = {
                                        val updated = localConfigs.toMutableMap()
                                        updated[selectedCfg.id] = selectedCfg.copy(alpha = it)
                                        localConfigs = updated
                                        onConfigsUpdated(updated)
                                    },
                                    valueRange = 0.2f..1.0f,
                                    colors = SliderDefaults.colors(thumbColor = Color(0xFF76FF03), activeTrackColor = Color(0xFF76FF03)),
                                    modifier = Modifier.weight(1f)
                                )
                                Text("${(selectedCfg.alpha * 100).roundToInt()}%", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
