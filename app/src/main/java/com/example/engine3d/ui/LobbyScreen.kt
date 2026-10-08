package com.example.engine3d.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

    val activePresetName by remember { mutableStateOf(hudPreferences.getActivePresetName()) }
    var hudConfigs by remember { mutableStateOf(hudPreferences.loadLayout(activePresetName)) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF070B12), Color(0xFF0F172A), Color(0xFF070B12))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .align(Alignment.TopCenter)
                .padding(horizontal = 20.dp, vertical = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("APEX3D ENGINE", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = Color(0xFF00E5FF))
                    Text("Mobile 3D Runtime & Custom Mesh Studio", fontSize = 11.sp, color = Color(0xFF90A4AE))
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0x3300E5FF),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF))
                ) {
                    Text(
                        text = "READY • ${settings.activePreset.name.take(7)}",
                        color = Color(0xFF00E5FF),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Hero Start Button
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
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier.size(38.dp).clip(CircleShape).background(Color(0xFF00E5FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(24.dp))
                    }
                    Column {
                        Text("Mulai Masuk Dunia 3D", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                        Text("Kompilasi shader GLSL untuk performa 60 FPS bebas stutter.", fontSize = 11.sp, color = Color(0xFFB0BEC5))
                    }
                }
            }

            // Status Tiles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusTile(
                    title = "Terrain",
                    value = customModelManager.activeCustomTerrainMesh?.name ?: terrainMesh.currentPreset.name,
                    color = Color(0xFF00E676),
                    modifier = Modifier.weight(1f)
                )
                StatusTile(
                    title = "Player Hero",
                    value = customModelManager.activeCustomCharacterMesh?.name ?: "Cyber Knight",
                    color = Color(0xFF00E5FF),
                    modifier = Modifier.weight(1f)
                )
                StatusTile(
                    title = "Objek Dunia",
                    value = "${npcManager.npcs.size} NPC • ${barrierManager.barriers.size} Batas",
                    color = Color(0xFFFFD600),
                    modifier = Modifier.weight(1f)
                )
            }

            Text("Pusat Kontrol & Studio Aset", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)

            // 1. Studio Kustomisasi HUD
            HubMenuCard(
                icon = Icons.Default.VideogameAsset,
                iconColor = Color(0xFF00E5FF),
                title = "Studio Kustomisasi HUD & Kontrol",
                subtitle = "Ubah tata letak tombol virtual, ukuran, opasitas, dan kontainer aksi.",
                onClick = { showHudEditorModal = true },
                tag = "open_hud_studio_menu"
            )

            // 2. Pusat Impor & Kustomisasi Aset (KEMBALI DI SINI)
            HubMenuCard(
                icon = Icons.Default.FolderOpen,
                iconColor = Color(0xFF76FF03),
                title = "Pusat Impor & Kustomisasi Aset (Lobby Hub)",
                subtitle = "Impor file .GLB, .OBJ, .OBB, dan .ZIP dari lobby. Sambungkan aksi lari, jurus, dan ketinggian ke model karakter.",
                onClick = { showAssetManagerSheet = true },
                tag = "open_asset_hub_menu"
            )

            // 3. Hub Konfigurasi & Editor Grafis Gerak (KEMBALI DI SINI)
            HubMenuCard(
                icon = Icons.Default.Build,
                iconColor = Color(0xFF00E5FF),
                title = "Hub Konfigurasi & Editor Grafis Gerak (Central Studio Hub)",
                subtitle = "Pusat konfigurasi grafis gerak karakter, fisika, trigger zone & pintu GLB/OBJ, rintangan peta, NPC dialog, dan cuaca.",
                onClick = { showPlanImporterSheet = true },
                tag = "open_plan_importer_menu"
            )

            // 4. Pengaturan Grafis Engine
            HubMenuCard(
                icon = Icons.Default.Tune,
                iconColor = Color(0xFFFFD600),
                title = "Pengaturan Grafis Engine",
                subtitle = "Pilih preset Potato 30 FPS, Balanced 60 FPS, atau Ultra HD 120 FPS.",
                onClick = { showGraphicsSheet = true },
                tag = "open_graphics_menu"
            )

            Spacer(Modifier.height(48.dp))
        }
    }

    if (showWarmupDialog) {
        ShaderWarmupDialog(onWarmupComplete = {
            showWarmupDialog = false
            onEnterGame()
        })
    }

    if (showHudEditorModal) {
        HudEditorOverlay(
            configs = hudConfigs,
            expandedConfig = expandedContainerConfig,
            onConfigsUpdated = { hudConfigs = it },
            onExpandedConfigUpdated = { onExpandedConfigChanged(it) },
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

    if (showGraphicsSheet) {
        GraphicsSettingsSheet(
            settings = settings,
            onSettingsChanged = {},
            onSpawnPhysicsCrate = {},
            onDismiss = { showGraphicsSheet = false }
        )
    }

    // Sheet Impor Aset GLB / OBJ / OBB / ZIP
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

    // Sheet Patcher & Konfigurasi Karakter / Area
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
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(title, fontSize = 9.sp, color = Color(0xFF90A4AE))
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun HubMenuCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    tag: String = ""
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131E33)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF223147), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(38.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF1C2B47)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                Text(subtitle, fontSize = 10.sp, color = Color(0xFF90A4AE))
            }
        }
    }
}
