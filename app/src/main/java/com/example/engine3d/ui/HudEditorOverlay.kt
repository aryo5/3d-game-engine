package com.example.engine3d.ui

import android.app.Activity
import android.content.pm.ActivityInfo
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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.North
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SwitchVideo
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.TouchApp
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current
    val activity = context as? Activity

    // Wajib Landscape selama berada di Editor Kontroler HUD, lalu pulihkan orientasi sebelumnya saat keluar
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
    var isSlidersVisible by remember { mutableStateOf(true) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White) // Background Putih Bersih sesuai permintaan
    ) {
        val screenW = maxWidth.value
        val screenH = maxHeight.value
        val isPortrait = screenW < screenH

        // Grid Blueprint / Pola Tata Letak Lembar Putih untuk memudahkan alignment tombol
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, Color(0xFFE2E8F0))
        )

        // 1. Draggable HUD Elements (Di atas Background Putih)
        localConfigs.forEach { (id, cfg) ->
            val isSelected = selectedControlId == id
            val baseScale = if (isPortrait) 0.88f else 1.0f
            val baseSize = when (id) {
                HudControlId.JOYSTICK -> 118.dp * baseScale
                HudControlId.LOOK_PAD -> if (isPortrait) 130.dp else 160.dp
                HudControlId.STATUS_BAR -> 132.dp * baseScale
                HudControlId.PERF_MONITOR -> 115.dp * baseScale
                HudControlId.MINIMAP_RADAR -> 62.dp * baseScale
                HudControlId.TOP_BAR_ACTIONS -> 96.dp * baseScale
                HudControlId.CROSSHAIR -> 38.dp * baseScale
                HudControlId.ATTACK -> 70.dp * baseScale
                HudControlId.JUMP -> 62.dp * baseScale
                HudControlId.CROUCH, HudControlId.ACTION_SLAM, HudControlId.ACTION_DASH -> 56.dp * baseScale
                HudControlId.INTERACT -> 66.dp * baseScale
                HudControlId.EXPANDED_MENU -> 60.dp * baseScale
                else -> 46.dp * baseScale
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
                    .alpha(if (cfg.isEnabled) cfg.alpha else 0.40f)
                    .clip(elementShape)
                    .background(
                        if (isSelected) Color(0xFF00E5FF) // Cyan jelas saat terpilih
                        else when (id) {
                            HudControlId.JOYSTICK -> Color(0xFF1E293B)
                            HudControlId.LOOK_PAD -> Color(0xFFE2E8F0)
                            HudControlId.STATUS_BAR -> Color(0xFF0F172A)
                            HudControlId.PERF_MONITOR -> Color(0xFF1E293B)
                            HudControlId.MINIMAP_RADAR -> Color(0xFF0F172A)
                            HudControlId.TOP_BAR_ACTIONS -> Color(0xFF1E293B)
                            HudControlId.CROSSHAIR -> Color(0xFFE0F7FA)
                            HudControlId.ATTACK -> Color(0xFFFF3D00)
                            HudControlId.JUMP -> Color(0xFF0288D1)
                            HudControlId.CROUCH -> Color(0xFF455A64)
                            HudControlId.ACTION_SLAM -> Color(0xFFEF6C00)
                            HudControlId.ACTION_DASH -> Color(0xFF2E7D32)
                            HudControlId.INTERACT -> Color(0xFF37474F)
                            HudControlId.EXPANDED_MENU -> Color(0xFFE65100)
                            else -> Color(0xFF37474F)
                        }
                    )
                    .border(
                        width = if (isSelected) 3.5.dp else 1.5.dp,
                        color = if (isSelected) Color(0xFF0091EA) else Color(0x660F172A),
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
                // Real UI button contents for perfect layout simulation
                when (id) {
                    HudControlId.JOYSTICK -> {
                        Box(
                            modifier = Modifier
                                .size(scaledSize * 0.42f)
                                .clip(CircleShape)
                                .background(Color(0xFF00E5FF))
                                .border(1.5.dp, Color.White, CircleShape)
                        )
                    }
                    HudControlId.LOOK_PAD -> {
                        Text(
                            text = "Usap Kamera",
                            color = if (isSelected) Color.Black else Color(0xFF334155),
                            fontSize = (10f * cfg.scale).coerceIn(7f, 12f).sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    HudControlId.STATUS_BAR -> {
                        Column(
                            modifier = Modifier.padding(4.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("HP", fontSize = 8.sp, color = Color(0xFF76FF03), fontWeight = FontWeight.Bold)
                                Box(modifier = Modifier.fillMaxWidth(0.85f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFF76FF03)))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("SP", fontSize = 8.sp, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                                Box(modifier = Modifier.fillMaxWidth(0.85f).height(5.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFF00E5FF)))
                            }
                        }
                    }
                    HudControlId.PERF_MONITOR -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("60 FPS", color = Color(0xFF76FF03), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("Apex3D • 16ms", color = Color(0xFF94A3B8), fontSize = 7.sp)
                        }
                    }
                    HudControlId.MINIMAP_RADAR -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Terrain, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                            Text("RADAR (N)", color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    HudControlId.TOP_BAR_ACTIONS -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(Color(0xFF00E5FF)), contentAlignment = Alignment.Center) {
                                Text("🏠", fontSize = 9.sp)
                            }
                            Text("Menu", color = Color(0xFF00E5FF), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    HudControlId.CROSSHAIR -> {
                        Box(modifier = Modifier.size(10.dp), contentAlignment = Alignment.Center) {
                            Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(Color(0xFF0091EA)))
                        }
                    }
                    else -> {
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
                            HudControlId.CAMERA_SWITCH -> "AC Cam"
                            HudControlId.FLASHLIGHT -> "Lampu"
                            HudControlId.RESET_POS -> "Reset"
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
                                modifier = Modifier.size(scaledSize * 0.40f)
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
            }
        }

        // 2. Sleek, Compact Top Header Bar (Height = 56.dp)
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(56.dp),
            color = Color(0xFF0F172A),
            shadowElevation = 6.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Header Label & Presets Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(20.dp))
                    Text(
                        text = "Editor Kontroler HUD (Landscape)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )

                    Spacer(Modifier.width(6.dp))

                    // Preset Menu
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

                    // Reset button
                    Button(
                        onClick = {
                            localConfigs = HudPresets.getPreset("DEFAULT_2_FINGER").mapValues { it.value.copy() }
                            localExpandedConfig = ExpandedContainerConfig()
                            onConfigsUpdated(localConfigs)
                            onExpandedConfigUpdated(localExpandedConfig)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("reset_hud_button")
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, tint = Color(0xFFFFD600), modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Reset", fontSize = 11.sp, color = Color(0xFFFFD600))
                    }
                }

                // Control Action Buttons (Cancel / Save / Slider Toggle)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    selectedControlId?.let { id ->
                        Text(
                            text = id.displayName,
                            color = Color(0xFF00E5FF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }

                    // Sliders Panel toggle
                    IconButton(
                        onClick = { isSlidersVisible = !isSlidersVisible },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isSlidersVisible) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                            contentDescription = "Toggle Sliders",
                            tint = Color(0xFF94A3B8)
                        )
                    }

                    OutlinedButton(
                        onClick = onCancel,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252)),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Batal", fontSize = 11.sp)
                    }

                    Button(
                        onClick = onSaveAndExit,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Simpan", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }

        // 3. Compact Slider Control Drawer (Di bagian bawah, rapi dan kontras tinggi)
        val selectedCfg = selectedControlId?.let { localConfigs[it] }
        if (selectedCfg != null && isSlidersVisible) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
                    .fillMaxWidth(0.85f)
                    .heightIn(max = 135.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xF20F172A),
                shadowElevation = 10.dp,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF00E5FF))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // horizontal list of buttons for easy quick selection
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
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

                    // Sliders for Scale (Ukuran) and Alpha (Transparansi), plus Enable Toggle
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Switch
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Tampilkan Button:", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Medium)
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
                                    checkedTrackColor = Color(0xFF0891B2)
                                )
                            )
                        }

                        // Size Slider
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.widthIn(min = 160.dp, max = 220.dp)
                        ) {
                            Text("Ukuran:", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Medium)
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
                            Text("${(selectedCfg.scale * 100).roundToInt()}%", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        // Alpha Slider
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.widthIn(min = 160.dp, max = 220.dp)
                        ) {
                            Text("Transparansi:", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Medium)
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
                            Text("${(selectedCfg.alpha * 100).roundToInt()}%", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Expanded customizer logic (if selected)
                    if (selectedCfg.id == HudControlId.EXPANDED_MENU) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF090E17), RoundedCornerShape(8.dp))
                                .padding(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "⚙️ Bentuk Kontainer Expanded (Menu Mengembang):",
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = Color(0xFFFFD600)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
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

                                Row(
                                    modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
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

