package com.example.engine3d.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine3d.renderer.EngineSettings
import com.example.engine3d.renderer.GraphicPreset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphicsSettingsSheet(
    settings: EngineSettings,
    onSettingsChanged: () -> Unit,
    onSpawnPhysicsCrate: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val updateSettings = {
        settings.saveToPrefs(context)
        onSettingsChanged()
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var currentPreset by remember { mutableStateOf(settings.activePreset) }
    var targetFps by remember { mutableIntStateOf(settings.targetFps) }
    var resScale by remember { mutableFloatStateOf(settings.resolutionScale) }
    var lightingQuality by remember { mutableIntStateOf(settings.lightingQuality) }
    var enableFog by remember { mutableStateOf(settings.enableFog) }
    var enableWireframe by remember { mutableStateOf(settings.enableWireframe) }
    var renderDistance by remember { mutableFloatStateOf(settings.renderDistance) }
    var camDist by remember { mutableFloatStateOf(settings.cameraDistance) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF101726),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = Color(0xFF00E5FF))
                Text("Pengaturan Grafis & Performa Engine", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
            }

            // Preset Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GraphicPreset.values().forEach { preset ->
                    val isSelected = currentPreset == preset
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(if (isSelected) Color(0x3300E5FF) else Color(0xFF1A233A), RoundedCornerShape(10.dp))
                            .border(if (isSelected) 1.5.dp else 1.dp, if (isSelected) Color(0xFF00E5FF) else Color(0xFF263238), RoundedCornerShape(10.dp))
                            .clickable {
                                currentPreset = preset
                                settings.applyPreset(preset)
                                targetFps = settings.targetFps
                                resScale = settings.resolutionScale
                                lightingQuality = settings.lightingQuality
                                enableFog = settings.enableFog
                                renderDistance = settings.renderDistance
                                updateSettings()
                            }
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = when (preset) {
                                GraphicPreset.POTATO_ULTRA_LIGHT -> "🥔 Potato"
                                GraphicPreset.BALANCED -> "⚖️ Balanced"
                                GraphicPreset.ULTRA_HD -> "🌟 Ultra HD"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (isSelected) Color(0xFF00E5FF) else Color.White
                        )
                        Text(
                            text = when (preset) {
                                GraphicPreset.POTATO_ULTRA_LIGHT -> "30 FPS • Unlit"
                                GraphicPreset.BALANCED -> "60 FPS • Gouraud"
                                GraphicPreset.ULTRA_HD -> "120 FPS • Specular"
                            },
                            fontSize = 10.sp,
                            color = Color(0xFFB0BEC5)
                        )
                    }
                }
            }

            // Target FPS Chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Target FPS:", fontSize = 12.sp, color = Color(0xFF90A4AE))
                listOf(30, 60, 90, 120).forEach { fps ->
                    FilterChip(
                        selected = targetFps == fps,
                        onClick = {
                            targetFps = fps
                            settings.targetFps = fps
                            updateSettings()
                        },
                        label = { Text("$fps", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF00E5FF),
                            selectedLabelColor = Color.Black
                        )
                    )
                }
            }

            // Sliders dengan warna seragam Cyber Cyan
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Skala Resolusi Render:", fontSize = 12.sp)
                    Text("${(resScale * 100).toInt()}%", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Slider(
                    value = resScale,
                    onValueChange = {
                        resScale = it
                        settings.resolutionScale = it
                        updateSettings()
                    },
                    valueRange = 0.5f..1.0f,
                    colors = SliderDefaults.colors(thumbColor = Color(0xFF00E5FF), activeTrackColor = Color(0xFF00E5FF))
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Jarak Pandang Render (Draw Distance):", fontSize = 12.sp)
                    Text("${renderDistance.toInt()}m", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Slider(
                    value = renderDistance,
                    onValueChange = {
                        renderDistance = it
                        settings.renderDistance = it
                        updateSettings()
                    },
                    valueRange = 80f..600f,
                    colors = SliderDefaults.colors(thumbColor = Color(0xFF00E5FF), activeTrackColor = Color(0xFF00E5FF))
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Jarak Kamera (Zoom):", fontSize = 12.sp)
                    Text(String.format("%.1fm", camDist), color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Slider(
                    value = camDist,
                    onValueChange = {
                        camDist = it
                        settings.cameraDistance = it
                        updateSettings()
                    },
                    valueRange = 2.0f..12.0f,
                    colors = SliderDefaults.colors(thumbColor = Color(0xFF00E5FF), activeTrackColor = Color(0xFF00E5FF))
                )
            }

            // Switches
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Kabut Atmosfer (Fog)", fontSize = 12.sp)
                Switch(
                    checked = enableFog,
                    onCheckedChange = {
                        enableFog = it
                        settings.enableFog = it
                        updateSettings()
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF))
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Mode Wireframe Poligon", fontSize = 12.sp)
                Switch(
                    checked = enableWireframe,
                    onCheckedChange = {
                        enableWireframe = it
                        settings.enableWireframe = it
                        updateSettings()
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF))
                )
            }

            Button(
                onClick = onSpawnPhysicsCrate,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Jatuhkan Crate Fisika Dinamis", color = Color.White, fontSize = 11.sp)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
