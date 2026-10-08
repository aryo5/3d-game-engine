package com.example.engine3d.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
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
    var sunElevation by remember { mutableFloatStateOf(settings.sunElevation) }
    var sunAzimuth by remember { mutableFloatStateOf(settings.sunAzimuth) }

    // Camera & Character Movement Control states
    var invertCameraX by remember { mutableStateOf(settings.invertCameraX) }
    var invertCameraY by remember { mutableStateOf(settings.invertCameraY) }
    var camSensitivity by remember { mutableFloatStateOf(settings.cameraSensitivity) }
    var invertMovementX by remember { mutableStateOf(settings.invertCharacterMovementX) }
    var invertFacing by remember { mutableStateOf(settings.invertCharacterFacing) }
    var twoSidedGlb by remember { mutableStateOf(settings.twoSidedGlbRendering) }

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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = Color(0xFF00E5FF))
                Text(
                    text = "Pengaturan Grafis & Performa Engine",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )
            }

            Text(
                text = "Preset Cepat Grafis (Paling Ringan untuk HP kentang hingga Ultra HD)",
                fontSize = 12.sp,
                color = Color(0xFF90A4AE)
            )

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
                            .background(
                                if (isSelected) Color(0x3300E5FF) else Color(0xFF1A233A),
                                RoundedCornerShape(10.dp)
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) Color(0xFF00E5FF) else Color(0xFF263238),
                                shape = RoundedCornerShape(10.dp)
                            )
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
                        verticalArrangement = Arrangement.spacedBy(4.dp)
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
                                GraphicPreset.POTATO_ULTRA_LIGHT -> "30 FPS • Unlit • Ringan"
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
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Speed, contentDescription = null, tint = Color(0xFFFFD600))
                    Text("Target FPS:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(30, 60, 90, 120).forEach { fps ->
                        FilterChip(
                            selected = targetFps == fps,
                            onClick = {
                                targetFps = fps
                                settings.targetFps = fps
                                updateSettings()
                            },
                            label = { Text("$fps FPS", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF00E5FF),
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }
            }

            // Resolution Scale Slider
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Skala Resolusi Render:", fontSize = 13.sp)
                    Text("${(resScale * 100).toInt()}%", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, fontSize = 13.sp)
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

            // Draw Distance Slider
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Jarak Pandang Render (Draw Distance):", fontSize = 13.sp)
                    Text("${renderDistance.toInt()}m", color = Color(0xFF76FF03), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Slider(
                    value = renderDistance,
                    onValueChange = {
                        renderDistance = it
                        settings.renderDistance = it
                        updateSettings()
                    },
                    valueRange = 80f..800f,
                    colors = SliderDefaults.colors(thumbColor = Color(0xFF76FF03), activeTrackColor = Color(0xFF76FF03))
                )
            }

            // Camera Distance Slider (Zoom Out / In)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("📷 Jarak Kamera (Zoom Out / In):", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(String.format("%.1fm", camDist), color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Slider(
                    value = camDist,
                    onValueChange = {
                        camDist = it
                        settings.cameraDistance = it
                        updateSettings()
                    },
                    valueRange = 2.5f..12.0f,
                    colors = SliderDefaults.colors(thumbColor = Color(0xFF00E5FF), activeTrackColor = Color(0xFF00E5FF))
                )
            }

            // Shading Model Selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.LightMode, contentDescription = null, tint = Color(0xFFFF9100))
                    Text("Model Pencahayaan Real-Time:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        0 to "Unlit (Termurah)",
                        1 to "Gouraud (Normal)",
                        2 to "Blinn-Phong (Mewah)"
                    ).forEach { (idx, label) ->
                        FilterChip(
                            selected = lightingQuality == idx,
                            onClick = {
                                lightingQuality = idx
                                settings.lightingQuality = idx
                                updateSettings()
                            },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF00E5FF),
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }
            }

            // Real-Time Sun Direction (Azimuth & Elevation)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Elevasi Matahari (Tinggi Sinar):", fontSize = 13.sp)
                    Text("${sunElevation.toInt()}°", color = Color(0xFFFFD600), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Slider(
                    value = sunElevation,
                    onValueChange = {
                        sunElevation = it
                        settings.sunElevation = it
                        updateSettings()
                    },
                    valueRange = 10f..85f,
                    colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD600), activeTrackColor = Color(0xFFFFD600))
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Arah Rotasi Sinar Matahari (Azimuth):", fontSize = 13.sp)
                    Text("${sunAzimuth.toInt()}°", color = Color(0xFFFFD600), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Slider(
                    value = sunAzimuth,
                    onValueChange = {
                        sunAzimuth = it
                        settings.sunAzimuth = it
                        updateSettings()
                    },
                    valueRange = 0f..360f,
                    colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD600), activeTrackColor = Color(0xFFFFD600))
                )
            }

            // Section: Kontrol Kamera & Arah Karakter (Gaya PUBG & Anti-Terbalik)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF162032), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF00E5FF), RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = Color(0xFF00E5FF))
                    Text(
                        text = "Kontrol Kamera & Arah Gerak (Gaya PUBG)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF00E5FF)
                    )
                }

                Text(
                    text = "Kamera dapat diusap bebas di area kosong mana saja tanpa kotak kontainer, serta cubit (pinch) 2 jari untuk zoom out/in seperti di galeri foto.",
                    fontSize = 11.sp,
                    color = Color(0xFFB0BEC5),
                    lineHeight = 15.sp
                )

                // Slider Sensitivitas Kamera
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Sensitivitas Usap Kamera & Zoom:", fontSize = 12.sp)
                        Text(
                            "${(camSensitivity * 100f).toInt()}%",
                            color = Color(0xFF00E5FF),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Slider(
                        value = camSensitivity,
                        onValueChange = {
                            camSensitivity = it
                            settings.cameraSensitivity = it
                            updateSettings()
                        },
                        valueRange = 0.10f..0.80f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF00E5FF), activeTrackColor = Color(0xFF00E5FF))
                    )
                }

                // Invert Camera Horizontal
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Invert Usap Kamera Horisontal (X)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Balikkan usap kiri/kanan (Default: OFF / Geser kanan belok kanan)",
                            fontSize = 11.sp,
                            color = Color(0xFF90A4AE)
                        )
                    }
                    Switch(
                        checked = invertCameraX,
                        onCheckedChange = {
                            invertCameraX = it
                            settings.invertCameraX = it
                            updateSettings()
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF), checkedTrackColor = Color(0x6600E5FF))
                    )
                }

                // Invert Camera Vertical
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Invert Usap Kamera Vertikal (Y)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Balikkan usap atas/bawah (Default: OFF / Geser atas lihat langit)",
                            fontSize = 11.sp,
                            color = Color(0xFF90A4AE)
                        )
                    }
                    Switch(
                        checked = invertCameraY,
                        onCheckedChange = {
                            invertCameraY = it
                            settings.invertCameraY = it
                            updateSettings()
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF), checkedTrackColor = Color(0x6600E5FF))
                    )
                }

                // Invert Character Movement X (Strafe / Turning Left-Right without inverting forward/backward)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Invert Gerak Karakter (Kiri / Kanan)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFFFD600))
                        Text(
                            "Membalikkan arah belok/strafe samping bila kontrol terbalik. Maju-mundur tetap normal!",
                            fontSize = 11.sp,
                            color = Color(0xFFB0BEC5)
                        )
                    }
                    Switch(
                        checked = invertMovementX,
                        onCheckedChange = {
                            invertMovementX = it
                            settings.invertCharacterMovementX = it
                            updateSettings()
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFFD600), checkedTrackColor = Color(0x66FFD600))
                    )
                }

                // Invert GLB Character Model Facing (180° Flip)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Invert Hadap Model GLB (Putar 180°)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF76FF03))
                        Text(
                            "Putar orientasi 3D model bila mesh GLB menghadap ke belakang",
                            fontSize = 11.sp,
                            color = Color(0xFFB0BEC5)
                        )
                    }
                    Switch(
                        checked = invertFacing,
                        onCheckedChange = {
                            invertFacing = it
                            settings.invertCharacterFacing = it
                            updateSettings()
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF76FF03), checkedTrackColor = Color(0x6676FF03))
                    )
                }

                // Two-Sided Rendering (Fix celah tembus pandang / backface culling pada mesh GLB)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Render Dua Sisi Model GLB (Double-Sided)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF00E5FF))
                        Text(
                            "Hilangkan celah tembus pandang pada baju/karakter dengan merender poligon depan & belakang (Anti-Culling)",
                            fontSize = 11.sp,
                            color = Color(0xFFB0BEC5)
                        )
                    }
                    Switch(
                        checked = twoSidedGlb,
                        onCheckedChange = {
                            twoSidedGlb = it
                            settings.twoSidedGlbRendering = it
                            updateSettings()
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF), checkedTrackColor = Color(0x6600E5FF))
                    )
                }
            }

            // Toggles: Fog & Wireframe
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Kabut Atmosfer (Distance Fog)", fontSize = 13.sp)
                    Text("Memberikan efek kedalaman pemandangan", fontSize = 11.sp, color = Color(0xFF90A4AE))
                }
                Switch(
                    checked = enableFog,
                    onCheckedChange = {
                        enableFog = it
                        settings.enableFog = it
                        updateSettings()
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF), checkedTrackColor = Color(0x6600E5FF))
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Mode Wireframe", fontSize = 13.sp)
                    Text("Visualisasikan jaring poligon 3D", fontSize = 11.sp, color = Color(0xFF90A4AE))
                }
                Switch(
                    checked = enableWireframe,
                    onCheckedChange = {
                        enableWireframe = it
                        settings.enableWireframe = it
                        updateSettings()
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF76FF03), checkedTrackColor = Color(0x6676FF03))
                )
            }

            // Spawn Dynamic Physics Crate
            Button(
                onClick = onSpawnPhysicsCrate,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263238)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("spawn_physics_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF00E5FF))
                Spacer(Modifier.width(8.dp))
                Text("Jatuhkan Crate Fisika Dinamis (Uji Tabrakan Kontur)", color = Color.White, fontSize = 12.sp)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
