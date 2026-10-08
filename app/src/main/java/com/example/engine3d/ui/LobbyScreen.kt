package com.example.engine3d.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine3d.actions.ActionManager
import com.example.engine3d.controller.ExpandedContainerConfig
import com.example.engine3d.controller.HudElementConfig
import com.example.engine3d.controller.HudPreferences
import com.example.engine3d.importer.BatchImportManager
import com.example.engine3d.importer.CustomModelManager
import com.example.engine3d.npc.NpcManager
import com.example.engine3d.physics.BarrierManager
import com.example.engine3d.physics.PhysicsEngine
import com.example.engine3d.actions.InteractionSystem
import com.example.engine3d.renderer.EngineSettings
import com.example.engine3d.terrain.TerrainMesh

@Composable
fun LobbyScreen(
    settings: EngineSettings,
    terrainMesh: TerrainMesh,
    customModelManager: CustomModelManager,
    batchImportManager: BatchImportManager,
    actionManager: ActionManager,
    npcManager: NpcManager,
    barrierManager: BarrierManager,
    hudPreferences: HudPreferences,
    expandedContainerConfig: ExpandedContainerConfig,
    onExpandedConfigChanged: (ExpandedContainerConfig) -> Unit,
    onEnterGame: () -> Unit,
    interactionSystem: InteractionSystem,
    physicsEngine: PhysicsEngine,
    modifier: Modifier = Modifier
) {
    var showWarmupDialog by remember { mutableStateOf(false) }
    var showGraphicsSheet by remember { mutableStateOf(false) }
    var showAssetManagerSheet by remember { mutableStateOf(false) }
    var showHudEditorModal by remember { mutableStateOf(false) }
    var showPlanImporterSheet by remember { mutableStateOf(false) }

    var activePresetName by remember { mutableStateOf(hudPreferences.getActivePresetName()) }
    var hudConfigs by remember { mutableStateOf(hudPreferences.loadLayout(activePresetName)) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF070B12),
                        Color(0xFF0F172A),
                        Color(0xFF070B12)
                    )
                )
            )
    ) {
        val screenWidth = maxWidth
        val isNarrow = screenWidth < 360.dp
        val horizontalPadding = if (isNarrow) 12.dp else if (screenWidth > 600.dp) 28.dp else 18.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .align(Alignment.TopCenter)
                .padding(horizontal = horizontalPadding, vertical = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(if (isNarrow) 14.dp else 18.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "APEX3D ENGINE",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp,
                        color = Color(0xFF00E5FF),
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Mobile 3D Runtime & Custom Mesh Studio",
                        fontSize = 11.sp,
                        color = Color(0xFF90A4AE)
                    )
                }

                Box(
                    modifier = Modifier
                        .background(Color(0x3300E5FF), RoundedCornerShape(20.dp))
                        .border(1.dp, Color(0xFF00E5FF), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "READY • ${settings.activePreset.name.take(7)}",
                        color = Color(0xFF00E5FF),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Hero Start Simulation Button (Requires Shader Warmup)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131E33)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, Color(0xFF00E5FF), RoundedCornerShape(16.dp))
                    .clickable { showWarmupDialog = true }
                    .testTag("start_game_hero_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00E5FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(24.dp))
                            }
                            Text(
                                text = "Mulai Masuk Dunia 3D",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = Color.White
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Melakukan kompilasi shader GLSL terlebih dahulu untuk performa 60 FPS bebas stutter.",
                            fontSize = 11.sp,
                            color = Color(0xFFB0BEC5),
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // Quick Status Tiles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatusTile(
                    title = "Terrain Aktif",
                    value = customModelManager.activeCustomTerrainMesh?.name ?: terrainMesh.currentPreset.name,
                    color = Color(0xFF00E676),
                    modifier = Modifier.weight(1f)
                )
                StatusTile(
                    title = "Model Player",
                    value = customModelManager.activeCustomCharacterMesh?.name ?: "Cyber Knight (Default)",
                    color = Color(0xFF00E5FF),
                    modifier = Modifier.weight(1f)
                )
                StatusTile(
                    title = "NPC & Batas",
                    value = "${npcManager.npcs.size} NPC • ${barrierManager.barriers.size} Batas",
                    color = Color(0xFFFFD600),
                    modifier = Modifier.weight(1f)
                )
            }

            // Hub Menu Cards
            Text(
                text = "Pusat Kontrol & Studio Aset",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color.White
            )

            // 1. Studio Kustomisasi HUD & Expanded Container
            HubMenuCard(
                icon = Icons.Default.VideogameAsset,
                iconColor = Color(0xFF00E5FF),
                title = "Studio Kustomisasi HUD & Kontrol",
                subtitle = "Edit posisi bebas, ukuran, opasitas, reset kontrol, serta atur Expanded Container (Radial Wheel / Baris Jurus).",
                onClick = { showHudEditorModal = true },
                tag = "open_hud_studio_menu"
            )

            // 2. Pusat Impor & Rigging Model GLB
            HubMenuCard(
                icon = Icons.Default.FolderOpen,
                iconColor = Color(0xFF76FF03),
                title = "Pusat Impor & Kustomisasi Aset (Lobby Hub)",
                subtitle = "Impor file .GLB, .OBJ, .OBB, dan .ZIP dari lobby. Sambungkan aksi lari, jurus, dan ketinggian ke model karakter.",
                onClick = { showAssetManagerSheet = true },
                tag = "open_asset_hub_menu"
            )

            // 3. Hub Konfigurasi & Editor Grafis Gerak (Central Studio Hub)
            HubMenuCard(
                icon = Icons.Default.Build,
                iconColor = Color(0xFF00E5FF),
                title = "Hub Konfigurasi & Editor Grafis Gerak (Central Studio Hub)",
                subtitle = "Pusat konfigurasi grafis gerak karakter, fisika, trigger zone & pintu GLB/OBJ, rintangan peta, NPC dialog, dan cuaca.",
                onClick = { showPlanImporterSheet = true },
                tag = "open_plan_importer_menu"
            )

            // 4. Pengaturan Grafis & Profil Performa
            HubMenuCard(
                icon = Icons.Default.Tune,
                iconColor = Color(0xFFFFD600),
                title = "Pengaturan Grafis Engine (HP Kentang s/d Ultra)",
                subtitle = "Pilih preset Paling Ringan (Potato), Balanced 60 FPS, atau Ultra HD 120 FPS. Atur jarak pandang, pencahayaan, dan kabut.",
                onClick = { showGraphicsSheet = true },
                tag = "open_graphics_menu"
            )

            Spacer(Modifier.height(16.dp))
        }
    }

    // Shader Warmup Compilation Sequence Dialog
    if (showWarmupDialog) {
        ShaderWarmupDialog(
            onWarmupComplete = {
                showWarmupDialog = false
                onEnterGame()
            }
        )
    }

    // HUD Editor Overlay from Lobby
    if (showHudEditorModal) {
        HudEditorOverlay(
            configs = hudConfigs,
            expandedConfig = expandedContainerConfig,
            onConfigsUpdated = { updated ->
                hudConfigs = updated
            },
            onExpandedConfigUpdated = { newExp ->
                onExpandedConfigChanged(newExp)
            },
            onSaveAndExit = {
                hudPreferences.saveLayout(activePresetName, hudConfigs)
                showHudEditorModal = false
            },
            onCancel = {
                hudConfigs = hudPreferences.loadLayout(activePresetName)
                showHudEditorModal = false
            }
        )
    }

    // Graphics Settings Sheet
    if (showGraphicsSheet) {
        GraphicsSettingsSheet(
            settings = settings,
            onSettingsChanged = {},
            onSpawnPhysicsCrate = {},
            onDismiss = { showGraphicsSheet = false }
        )
    }

    // Asset Manager & Rigging Sheet
    if (showAssetManagerSheet) {
        AssetManagerSheet(
            customModelManager = customModelManager,
            batchImportManager = batchImportManager,
            terrainMesh = terrainMesh,
            actionManager = actionManager,
            npcManager = npcManager,
            barrierManager = barrierManager,
            onModelImported = {},
            onTerrainChanged = {},
            onDismiss = { showAssetManagerSheet = false }
        )
    }

    // Hub Konfigurasi & Editor Grafis Gerak
    if (showPlanImporterSheet) {
        GlbConfigPatcherSheet(
            customModelManager = customModelManager,
            terrainMesh = terrainMesh,
            barrierManager = barrierManager,
            npcManager = npcManager,
            interactionSystem = interactionSystem,
            playerPos = physicsEngine.characterPos,
            settings = settings,
            physicsEngine = physicsEngine,
            actionManager = actionManager,
            onDismiss = { showPlanImporterSheet = false }
        )
    }
}

@Composable
fun StatusTile(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color(0xFF131C2E), RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFF263238), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(title, fontSize = 10.sp, color = Color(0xFF90A4AE), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun HubMenuCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    tag: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131E33)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF223147), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1C2B47)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                Text(subtitle, fontSize = 11.sp, color = Color(0xFF90A4AE), lineHeight = 15.sp)
            }
        }
    }
}
