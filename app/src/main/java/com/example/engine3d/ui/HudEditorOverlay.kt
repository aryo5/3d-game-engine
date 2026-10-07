package com.example.engine3d.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine3d.controller.ExpandedContainerConfig
import com.example.engine3d.controller.ExpandedContainerType
import com.example.engine3d.controller.ExpandedContentType
import com.example.engine3d.controller.HudControlId
import com.example.engine3d.controller.HudElementConfig
import com.example.engine3d.controller.HudPresets
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
    var localConfigs by remember { mutableStateOf(configs.mapValues { it.value.copy() }) }
    var localExpandedConfig by remember { mutableStateOf(expandedConfig.copy()) }
    var selectedControlId by remember { mutableStateOf<HudControlId?>(HudControlId.EXPANDED_MENU) }
    var showPresetMenu by remember { mutableStateOf(false) }
    var isPanelCollapsed by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0x990A0E17))
    ) {
        val screenW = maxWidth.value
        val screenH = maxHeight.value
        val isPortrait = screenW < screenH

        // 1. Grid lines overlay for precision alignment (Subtle HUD Blueprint)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, Color(0x2200E5FF))
        )

        // 2. Draggable HUD Element Handles
        localConfigs.forEach { (id, cfg) ->
            val isSelected = selectedControlId == id
            val baseSize = when (id) {
                HudControlId.JOYSTICK -> 110.dp
                HudControlId.LOOK_PAD -> 135.dp
                HudControlId.ATTACK -> 70.dp
                HudControlId.JUMP -> 62.dp
                HudControlId.CROUCH, HudControlId.ACTION_SLAM, HudControlId.ACTION_DASH -> 56.dp
                HudControlId.INTERACT -> 65.dp
                HudControlId.EXPANDED_MENU -> 60.dp
                else -> 48.dp
            }
            val scaledSize = baseSize * cfg.scale

            val posX = (cfg.xPercent * screenW - scaledSize.value / 2f).coerceIn(0f, screenW - scaledSize.value)
            val posY = (cfg.yPercent * screenH - scaledSize.value / 2f).coerceIn(0f, screenH - scaledSize.value)

            Box(
                modifier = Modifier
                    .offset { IntOffset(posX.dp.roundToPx(), posY.dp.roundToPx()) }
                    .size(scaledSize)
                    .alpha(if (cfg.isEnabled) cfg.alpha else 0.25f)
                    .clip(if (id == HudControlId.LOOK_PAD) RoundedCornerShape(12.dp) else CircleShape)
                    .background(
                        if (isSelected) Color(0xAA00E5FF)
                        else if (id == HudControlId.ATTACK) Color(0x66FF3D00)
                        else if (id == HudControlId.EXPANDED_MENU) Color(0x77FFD600)
                        else Color(0x5537474F)
                    )
                    .border(
                        width = if (isSelected) 2.5.dp else 1.dp,
                        color = if (isSelected) Color(0xFF00E5FF) else Color(0x88FFFFFF),
                        shape = if (id == HudControlId.LOOK_PAD) RoundedCornerShape(12.dp) else CircleShape
                    )
                    .pointerInput(id, screenW, screenH) {
                        detectDragGestures(
                            onDragStart = {
                                selectedControlId = id
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val curCfg = localConfigs[id] ?: return@detectDragGestures
                                val dx = (dragAmount.x / density) / screenW
                                val dy = (dragAmount.y / density) / screenH
                                val newX = (curCfg.xPercent + dx).coerceIn(0.04f, 0.96f)
                                val newY = (curCfg.yPercent + dy).coerceIn(0.04f, 0.96f)

                                val updated = localConfigs.toMutableMap()
                                updated[id] = curCfg.copy(xPercent = newX, yPercent = newY)
                                localConfigs = updated
                                onConfigsUpdated(updated)
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = id.displayName,
                    color = if (isSelected) Color.Black else Color.White,
                    fontSize = (9f * cfg.scale).coerceIn(8f, 13f).sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }

        // 3. Top Floating Inspector Panel (Responsive, scrollable & collapsible)
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(if (isPortrait) 0.98f else 0.88f)
                .padding(top = 10.dp)
                .heightIn(max = if (isPortrait) 310.dp else 220.dp),
            shape = RoundedCornerShape(14.dp),
            color = Color(0xF20F172A),
            shadowElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF263238))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header Row (Title, Presets, Collapse, Cancel, Save)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
                        Text(
                            text = if (isPortrait) "Editor HUD" else "Studio HUD & Kontrol",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )

                        // Presets Button
                        Box {
                            OutlinedButton(
                                onClick = { showPresetMenu = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF80D8FF)),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(32.dp)
                                    .testTag("preset_dropdown_button")
                            ) {
                                Text("Preset ▾", fontSize = 11.sp)
                            }

                            DropdownMenu(
                                expanded = showPresetMenu,
                                onDismissRequest = { showPresetMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Default 2-Finger") },
                                    onClick = {
                                        localConfigs = HudPresets.getPreset("DEFAULT_2_FINGER").mapValues { it.value.copy() }
                                        onConfigsUpdated(localConfigs)
                                        showPresetMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("3-Finger Claw") },
                                    onClick = {
                                        localConfigs = HudPresets.getPreset("CLAW_3_FINGER").mapValues { it.value.copy() }
                                        onConfigsUpdated(localConfigs)
                                        showPresetMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("4-Finger Pro Claw") },
                                    onClick = {
                                        localConfigs = HudPresets.getPreset("PUBG_4_FINGER_CLAW").mapValues { it.value.copy() }
                                        onConfigsUpdated(localConfigs)
                                        showPresetMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Left-Handed (Kidal)") },
                                    onClick = {
                                        localConfigs = HudPresets.getPreset("LEFT_HANDED").mapValues { it.value.copy() }
                                        onConfigsUpdated(localConfigs)
                                        showPresetMenu = false
                                    }
                                )
                            }
                        }

                        // Reset HUD Button
                        Button(
                            onClick = {
                                localConfigs = HudPresets.getPreset("DEFAULT_2_FINGER").mapValues { it.value.copy() }
                                localExpandedConfig = ExpandedContainerConfig()
                                onConfigsUpdated(localConfigs)
                                onExpandedConfigUpdated(localExpandedConfig)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263238)),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("reset_hud_button")
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null, tint = Color(0xFFFFD600), modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("Reset", fontSize = 11.sp, color = Color(0xFFFFD600))
                        }
                    }

                    // Action buttons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = { isPanelCollapsed = !isPanelCollapsed },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                if (isPanelCollapsed) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                contentDescription = "Minimize",
                                tint = Color(0xFFB0BEC5)
                            )
                        }

                        OutlinedButton(
                            onClick = onCancel,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252)),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Batal", fontSize = 11.sp)
                        }

                        Button(
                            onClick = onSaveAndExit,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("Simpan", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }

                // Collapsible body
                AnimatedVisibility(visible = !isPanelCollapsed) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Quick Button Selector Row (Scrollable horizontally)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            localConfigs.keys.forEach { id ->
                                val isSel = selectedControlId == id
                                val isEn = localConfigs[id]?.isEnabled == true
                                FilterChip(
                                    selected = isSel,
                                    onClick = { selectedControlId = id },
                                    label = { Text(id.displayName, fontSize = 10.sp) },
                                    leadingIcon = {
                                        Icon(
                                            if (isEn) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp),
                                            tint = if (isSel) Color.Black else if (isEn) Color(0xFF00E5FF) else Color.Gray
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF00E5FF),
                                        selectedLabelColor = Color.Black
                                    )
                                )
                            }
                        }

                        // Inspector controls for currently selected button
                        val selectedCfg = selectedControlId?.let { localConfigs[it] }
                        if (selectedCfg != null) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Enable / Disable Switch
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("Aktif:", color = Color(0xFFB0BEC5), fontSize = 11.sp)
                                    Switch(
                                        checked = selectedCfg.isEnabled,
                                        onCheckedChange = { en ->
                                            val updated = localConfigs.toMutableMap()
                                            updated[selectedCfg.id] = selectedCfg.copy(isEnabled = en)
                                            localConfigs = updated
                                            onConfigsUpdated(updated)
                                        },
                                        modifier = Modifier.height(24.dp),
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color(0xFF00E5FF),
                                            checkedTrackColor = Color(0xFF005B66)
                                        )
                                    )
                                }

                                // Scale Slider
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.widthIn(min = 130.dp, max = 220.dp)
                                ) {
                                    Text("Ukuran:", color = Color(0xFFB0BEC5), fontSize = 10.sp)
                                    Slider(
                                        value = selectedCfg.scale,
                                        onValueChange = { newScale ->
                                            val updated = localConfigs.toMutableMap()
                                            updated[selectedCfg.id] = selectedCfg.copy(scale = newScale)
                                            localConfigs = updated
                                            onConfigsUpdated(updated)
                                        },
                                        valueRange = 0.5f..1.8f,
                                        colors = SliderDefaults.colors(thumbColor = Color(0xFF00E5FF), activeTrackColor = Color(0xFF00E5FF)),
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                // Opacity Slider
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.widthIn(min = 130.dp, max = 220.dp)
                                ) {
                                    Text("Alpha:", color = Color(0xFFB0BEC5), fontSize = 10.sp)
                                    Slider(
                                        value = selectedCfg.alpha,
                                        onValueChange = { newAlpha ->
                                            val updated = localConfigs.toMutableMap()
                                            updated[selectedCfg.id] = selectedCfg.copy(alpha = newAlpha)
                                            localConfigs = updated
                                            onConfigsUpdated(updated)
                                        },
                                        valueRange = 0.20f..1.0f,
                                        colors = SliderDefaults.colors(thumbColor = Color(0xFF76FF03), activeTrackColor = Color(0xFF76FF03)),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            // If selected button is EXPANDED_MENU, show container layout customizer!
                            if (selectedCfg.id == HudControlId.EXPANDED_MENU) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF090E17), RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        "⚙️ Bentuk Kontainer Expanded (Menu Mengembang):",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Color(0xFFFFD600)
                                    )

                                    // Container Type Chips
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        ExpandedContainerType.values().forEach { t ->
                                            FilterChip(
                                                selected = localExpandedConfig.containerType == t,
                                                onClick = {
                                                    localExpandedConfig = localExpandedConfig.copy(containerType = t)
                                                    onExpandedConfigUpdated(localExpandedConfig)
                                                },
                                                label = { Text(t.displayName, fontSize = 9.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = Color(0xFFFFD600),
                                                    selectedLabelColor = Color.Black
                                                )
                                            )
                                        }
                                    }

                                    // Content Type Chips
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        ExpandedContentType.values().forEach { ct ->
                                            FilterChip(
                                                selected = localExpandedConfig.contentType == ct,
                                                onClick = {
                                                    localExpandedConfig = localExpandedConfig.copy(contentType = ct)
                                                    onExpandedConfigUpdated(localExpandedConfig)
                                                },
                                                label = { Text(ct.displayName, fontSize = 9.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = Color(0xFF00E5FF),
                                                    selectedLabelColor = Color.Black
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
