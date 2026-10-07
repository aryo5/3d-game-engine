package com.example.engine3d.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SwitchVideo
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.engine3d.actions.ActionManager
import com.example.engine3d.actions.InteractionSystem
import com.example.engine3d.controller.ExpandedContainerConfig
import com.example.engine3d.controller.ExpandedContainerType
import com.example.engine3d.controller.ExpandedContentType
import com.example.engine3d.controller.HudControlId
import com.example.engine3d.controller.HudElementConfig
import com.example.engine3d.controller.HudPreferences
import com.example.engine3d.controller.HudPresets
import com.example.engine3d.core.CameraPresetMode
import com.example.engine3d.importer.BatchImportManager
import com.example.engine3d.importer.CustomModelManager
import com.example.engine3d.npc.NpcManager
import com.example.engine3d.physics.BarrierManager
import com.example.engine3d.physics.PhysicsEngine
import com.example.engine3d.renderer.Apex3DRenderer
import com.example.engine3d.renderer.EngineSettings
import com.example.engine3d.renderer.GraphicPreset
import com.example.engine3d.terrain.TerrainMesh

enum class StudioMenuTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    CONTROLS("Kontrol HUD", Icons.Default.VideogameAsset),
    GRAPHICS("Grafis & FPS", Icons.Default.Tune),
    ASSETS("Impor GLB", Icons.Default.FolderOpen),
    UTILITIES("Utilitas Dunia", Icons.Default.Apps)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StudioMenuSheet(
    settings: EngineSettings,
    renderer: Apex3DRenderer,
    physicsEngine: PhysicsEngine,
    actionManager: ActionManager,
    interactionSystem: InteractionSystem,
    customModelManager: CustomModelManager,
    batchImportManager: BatchImportManager,
    terrainMesh: TerrainMesh,
    npcManager: NpcManager,
    barrierManager: BarrierManager,
    hudPreferences: HudPreferences,
    activePresetName: String,
    onPresetChanged: (String, Map<HudControlId, HudElementConfig>) -> Unit,
    onOpenHudEditor: () -> Unit,
    onOpenAssetSheet: () -> Unit,
    onSettingsChanged: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableStateOf(StudioMenuTab.CONTROLS) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xF80B0F19),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0x3300E5FF), CircleShape)
                            .border(1.dp, Color(0xFF00E5FF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
                    }
                    Column {
                        Text("Pengaturan & Studio Apex3D", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                        Text("Konfigurasi HUD, Grafis, Impor Aset & Utilitas Game", fontSize = 11.sp, color = Color(0xFF90A4AE))
                    }
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                }
            }

            Spacer(Modifier.height(10.dp))

            // Tab Navigation
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = Color(0xFF101726),
                contentColor = Color(0xFF00E5FF),
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                        color = Color(0xFF00E5FF)
                    )
                }
            ) {
                StudioMenuTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(tab.icon, contentDescription = null, modifier = Modifier.size(15.dp))
                                Text(tab.title, fontSize = 12.sp, fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Tab Contents
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (selectedTab) {
                    StudioMenuTab.CONTROLS -> {
                        // 1. Action to Open Drag & Drop HUD Editor
                        Button(
                            onClick = {
                                onDismiss()
                                onOpenHudEditor()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("menu_open_hud_editor")
                        ) {
                            Icon(Icons.Default.VideogameAsset, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Buka Editor Tata Letak HUD (Geser & Atur Bebas)", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        // 2. Preset Switcher
                        Text("Pilih Preset Tata Letak HUD:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color(0xFF80D8FF))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            val presets = listOf(
                                "DEFAULT_2_FINGER" to "Standar 2-Jari (ARPG)",
                                "CLAW_3_FINGER" to "3-Jari Claw",
                                "PUBG_4_FINGER_CLAW" to "4-Jari Pro Claw",
                                "LEFT_HANDED" to "Kidal (Left-Handed)"
                            )
                            presets.forEach { (key, label) ->
                                val isSelected = activePresetName == key
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        val newConfigs = HudPresets.getPreset(key).mapValues { it.value.copy() }
                                        hudPreferences.saveLayout(key, newConfigs)
                                        hudPreferences.setActivePresetName(key)
                                        onPresetChanged(key, newConfigs)
                                    },
                                    label = { Text(label, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF00E5FF),
                                        selectedLabelColor = Color.Black
                                    )
                                )
                            }
                        }

                        // 3. Reset Button
                        Button(
                            onClick = {
                                hudPreferences.resetPreset(activePresetName)
                                val resetConfigs = HudPresets.getPreset(activePresetName).mapValues { it.value.copy() }
                                onPresetChanged(activePresetName, resetConfigs)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263238)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null, tint = Color(0xFFFFD600), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Reset Preset Ini ke Posisi Standar Pabrik", color = Color(0xFFFFD600), fontSize = 12.sp)
                        }
                    }

                    StudioMenuTab.GRAPHICS -> {
                        // Presets
                        Text("Preset Grafis & FPS:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color(0xFF80D8FF))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            GraphicPreset.values().forEach { p ->
                                val isSel = settings.activePreset == p
                                Button(
                                    onClick = {
                                        settings.applyPreset(p)
                                        onSettingsChanged()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSel) Color(0xFF00E5FF) else Color(0xFF1E293B)
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = when(p) {
                                            GraphicPreset.POTATO_ULTRA_LIGHT -> "Potato 30fps"
                                            GraphicPreset.BALANCED -> "Balance 60fps"
                                            GraphicPreset.ULTRA_HD -> "Ultra 120fps"
                                        },
                                        fontSize = 10.sp,
                                        color = if (isSel) Color.Black else Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Toggle dynamic lights & wireframe
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Kabut Atmosfer (Fog)", fontSize = 12.sp)
                            Switch(
                                checked = settings.enableFog,
                                onCheckedChange = {
                                    settings.enableFog = it
                                    onSettingsChanged()
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF))
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Mode Wireframe Poligon", fontSize = 12.sp)
                            Switch(
                                checked = settings.enableWireframe,
                                onCheckedChange = {
                                    settings.enableWireframe = it
                                    onSettingsChanged()
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF))
                            )
                        }

                        Button(
                            onClick = {
                                physicsEngine.spawnPhysicsBox(physicsEngine.characterPos)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF37474F)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Jatuhkan Balok Fisika di Lokasi Karakter (+Box)", fontSize = 12.sp)
                        }
                    }

                    StudioMenuTab.ASSETS -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF131D31)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Kelola Aset & Folder Eksternal", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF00E5FF))
                                Text(
                                    "Buka dialog manajemen aset untuk memindai folder GLB, mengekstrak OBB/ZIP, dan mengekspor kit sample manifest.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF90A4AE)
                                )
                                Button(
                                    onClick = {
                                        onDismiss()
                                        onOpenAssetSheet()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color.Black)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Buka Manajer Impor Folder & GLB", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    StudioMenuTab.UTILITIES -> {
                        // Camera Mode Switcher
                        Text("Mode Kamera Assassin's Creed Shadows:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color(0xFF80D8FF))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            CameraPresetMode.values().forEach { mode ->
                                val isSel = renderer.camera.mode == mode
                                FilterChip(
                                    selected = isSel,
                                    onClick = {
                                        renderer.camera.setCameraMode(mode)
                                    },
                                    label = { Text(mode.displayName, fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF00E5FF),
                                        selectedLabelColor = Color.Black
                                    )
                                )
                            }
                        }

                        // World Flashlight Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Highlight, contentDescription = null, tint = Color(0xFFFFD600), modifier = Modifier.size(18.dp))
                                Text("Senter Dunia (Point Light)", fontSize = 12.sp)
                            }
                            val isLightOn = renderer.lighting.pointLights.firstOrNull()?.let { it.intensity > 0.1f } ?: false
                            Switch(
                                checked = isLightOn,
                                onCheckedChange = {
                                    if (renderer.lighting.pointLights.isNotEmpty()) {
                                        renderer.lighting.pointLights[0].intensity = if (it) 2.5f else 0f
                                    }
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFFD600))
                            )
                        }

                        // Reset Position Button
                        Button(
                            onClick = {
                                physicsEngine.resetCharacterPosition()
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(6.dp))
                            Text("Reset Posisi Karakter ke Titik Awal (0, 0, 0)", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
