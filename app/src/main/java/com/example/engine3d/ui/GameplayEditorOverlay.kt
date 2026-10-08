package com.example.engine3d.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine3d.actions.InteractableType
import com.example.engine3d.actions.InteractionSystem
import com.example.engine3d.actions.WorldInteractable
import com.example.engine3d.controller.HudControlId
import com.example.engine3d.controller.HudElementConfig
import com.example.engine3d.controller.HudPreferences
import com.example.engine3d.controller.HudPresets
import com.example.engine3d.importer.SampleExportManager
import com.example.engine3d.math.Vec3
import com.example.engine3d.npc.NpcBehavior
import com.example.engine3d.npc.NpcEntity
import com.example.engine3d.npc.NpcManager
import com.example.engine3d.physics.BarrierManager
import com.example.engine3d.physics.BarrierType
import com.example.engine3d.physics.PhysicsEngine
import com.example.engine3d.physics.WorldBarrier
import com.example.engine3d.renderer.Apex3DRenderer
import com.example.engine3d.renderer.EngineSettings
import kotlin.math.roundToInt

enum class GameplayEditorTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    WORLD_OBJECTS("Objek & Rintangan", Icons.Default.Terrain),
    NPCS_INTERACT("NPC & Interaksi", Icons.AutoMirrored.Filled.Chat),
    PHYSICS_PLAYER("Fisika & Karakter", Icons.Default.Speed),
    LIGHTING_ENV("Cuaca & Cahaya", Icons.Default.Brightness6),
    HUD_LAYOUT("Tata Letak HUD", Icons.Default.VideogameAsset),
    SAVE_LEVEL("Simpan & Ekspor", Icons.Default.Save)
}

/**
 * Overlay Live Editor Gameplay untuk mengedit seluruh elemen di dalam game saat berjalan:
 * - Menambah, menggeser, dan menghapus rintangan / tembok / speed pad
 * - Menambah dan mengonfigurasi NPC dan dialognya
 * - Mengubah fisika gravitasi, lompat, kecepatan lari secara live
 * - Mengatur cuaca, posisi matahari, cahaya dan kabut
 * - Mengatur dan menyesuaikan seluruh HUD
 * - Menyimpan level ke memori dan mengekspor ke folder publik
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GameplayEditorOverlay(
    physicsEngine: PhysicsEngine,
    barrierManager: BarrierManager,
    npcManager: NpcManager,
    interactionSystem: InteractionSystem,
    renderer: Apex3DRenderer,
    settings: EngineSettings,
    hudConfigs: Map<HudControlId, HudElementConfig>,
    hudPreferences: HudPreferences,
    activePresetName: String,
    onHudConfigsUpdated: (Map<HudControlId, HudElementConfig>) -> Unit,
    onOpenFullHudEditor: () -> Unit,
    onExitEditorMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sceneManager = remember { GameplaySceneManager(context) }
    val exportManager = remember { SampleExportManager(context, barrierManager = barrierManager, npcManager = npcManager) }

    var selectedTab by remember { mutableStateOf(GameplayEditorTab.WORLD_OBJECTS) }
    var isDrawerExpanded by remember { mutableStateOf(true) }

    // Object Editing State
    var selectedBarrierId by remember { mutableStateOf<String?>(null) }
    var selectedNpcId by remember { mutableStateOf<String?>(null) }
    var selectedInteractableId by remember { mutableStateOf<String?>(null) }

    // Repaint trigger for canvas/scene
    var updateCounter by remember { mutableIntStateOf(0) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0x18000000)) // Non-obstructive live 3D view
    ) {
        // 1. Top Sleek Editor Status Bar
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xF50D1524),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color(0xFF00E5FF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = Color.Black, modifier = Modifier.size(15.dp))
                    }
                    Column {
                        Text(
                            text = "🛠️ MODE EDITOR GAMEPLAY AKTIF",
                            color = Color(0xFF00E5FF),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        val p = physicsEngine.characterPos
                        Text(
                            text = "Posisi Karakter: X: %.1f, Y: %.1f, Z: %.1f • FPS: %d".format(p.x, p.y, p.z, renderer.fps),
                            color = Color(0xFF90A4AE),
                            fontSize = 9.sp
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = { isDrawerExpanded = !isDrawerExpanded },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isDrawerExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Lipat Panel",
                            tint = Color.White
                        )
                    }

                    Button(
                        onClick = onExitEditorMode,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp).testTag("exit_gameplay_editor_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Mode Main", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }

        // 2. Main Floating Inspector Drawer (Expandable at Bottom)
        AnimatedVisibility(
            visible = isDrawerExpanded,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(290.dp)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xF20B111D),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF1E2D4A))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    // Tab Row
                    TabRow(
                        selectedTabIndex = selectedTab.ordinal,
                        containerColor = Color(0xFF111A2E),
                        contentColor = Color(0xFF00E5FF),
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                                color = Color(0xFF00E5FF)
                            )
                        },
                        modifier = Modifier.height(38.dp)
                    ) {
                        GameplayEditorTab.values().forEach { tab ->
                            Tab(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(tab.icon, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Text(
                                            tab.title,
                                            fontSize = 10.sp,
                                            fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Tab Contents
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        when (selectedTab) {
                            GameplayEditorTab.WORLD_OBJECTS -> {
                                WorldObjectsEditorTab(
                                    barrierManager = barrierManager,
                                    physicsEngine = physicsEngine,
                                    selectedBarrierId = selectedBarrierId,
                                    onSelectBarrier = { selectedBarrierId = it },
                                    onUpdate = { updateCounter++ }
                                )
                            }
                            GameplayEditorTab.NPCS_INTERACT -> {
                                NpcAndInteractEditorTab(
                                    npcManager = npcManager,
                                    interactionSystem = interactionSystem,
                                    physicsEngine = physicsEngine,
                                    selectedNpcId = selectedNpcId,
                                    selectedInteractableId = selectedInteractableId,
                                    onSelectNpc = { selectedNpcId = it },
                                    onSelectInteractable = { selectedInteractableId = it },
                                    onUpdate = { updateCounter++ }
                                )
                            }
                            GameplayEditorTab.PHYSICS_PLAYER -> {
                                PhysicsPlayerEditorTab(
                                    physicsEngine = physicsEngine,
                                    onUpdate = { updateCounter++ }
                                )
                            }
                            GameplayEditorTab.LIGHTING_ENV -> {
                                LightingEnvEditorTab(
                                    renderer = renderer,
                                    settings = settings,
                                    onUpdate = { updateCounter++ }
                                )
                            }
                            GameplayEditorTab.HUD_LAYOUT -> {
                                HudLayoutEditorTab(
                                    hudConfigs = hudConfigs,
                                    hudPreferences = hudPreferences,
                                    activePresetName = activePresetName,
                                    onHudConfigsUpdated = onHudConfigsUpdated,
                                    onOpenFullHudEditor = onOpenFullHudEditor
                                )
                            }
                            GameplayEditorTab.SAVE_LEVEL -> {
                                SaveLevelEditorTab(
                                    sceneManager = sceneManager,
                                    exportManager = exportManager,
                                    physicsEngine = physicsEngine,
                                    barrierManager = barrierManager,
                                    npcManager = npcManager,
                                    interactionSystem = interactionSystem,
                                    renderer = renderer,
                                    settings = settings,
                                    onReload = { updateCounter++ }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Tab 1: Objek, Rintangan & Tembok Dunia **/
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WorldObjectsEditorTab(
    barrierManager: BarrierManager,
    physicsEngine: PhysicsEngine,
    selectedBarrierId: String?,
    onSelectBarrier: (String?) -> Unit,
    onUpdate: () -> Unit
) {
    val context = LocalContext.current
    val currentBarrier = barrierManager.barriers.firstOrNull { it.id == selectedBarrierId }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Daftar Rintangan & Batas Dunia (${barrierManager.barriers.size} Objek):",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = Color(0xFF00E5FF)
            )

            Button(
                onClick = {
                    val p = physicsEngine.characterPos
                    val newId = "custom_barrier_${System.currentTimeMillis()}"
                    val newBarrier = WorldBarrier(
                        id = newId,
                        name = "Tembok Baru #${barrierManager.barriers.size + 1}",
                        type = BarrierType.WALL_BARRIER,
                        position = Vec3(p.x, p.y + 1.5f, p.z),
                        size = Vec3(3f, 3f, 3f),
                        isPassable = false,
                        warningMessage = "🚫 Batas Tembok Solid Kustom!"
                    )
                    barrierManager.addBarrier(newBarrier)
                    onSelectBarrier(newId)
                    onUpdate()
                    Toast.makeText(context, "🧱 Tembok baru dibuat di posisi karakter!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text("+ Buat Objek", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Horizontal chip list of barriers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            barrierManager.barriers.forEach { b ->
                val isSel = b.id == selectedBarrierId
                FilterChip(
                    selected = isSel,
                    onClick = { onSelectBarrier(b.id) },
                    label = { Text(b.name.take(18), fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF00E5FF),
                        selectedLabelColor = Color.Black
                    )
                )
            }
        }

        // Inspector for selected barrier
        if (currentBarrier != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131D31)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚙️ ${currentBarrier.name} [${currentBarrier.type.name}]",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color(0xFFFFD600)
                        )
                        IconButton(
                            onClick = {
                                barrierManager.removeBarrier(currentBarrier.id)
                                onSelectBarrier(null)
                                onUpdate()
                                Toast.makeText(context, "Objek dihapus!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp))
                        }
                    }

                    // Position Controls (X, Y, Z)
                    Text("Posisi Objek (Meter):", fontSize = 10.sp, color = Color(0xFF90A4AE))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CoordinateNudge(label = "X", value = currentBarrier.position.x, onValueChange = { currentBarrier.position.x = it; onUpdate() }, modifier = Modifier.weight(1f))
                        CoordinateNudge(label = "Y", value = currentBarrier.position.y, onValueChange = { currentBarrier.position.y = it; onUpdate() }, modifier = Modifier.weight(1f))
                        CoordinateNudge(label = "Z", value = currentBarrier.position.z, onValueChange = { currentBarrier.position.z = it; onUpdate() }, modifier = Modifier.weight(1f))
                    }

                    // Size Controls (Width, Height, Depth)
                    Text("Dimensi Ukuran Objek (Meter):", fontSize = 10.sp, color = Color(0xFF90A4AE))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CoordinateNudge(label = "Lebar (X)", value = currentBarrier.size.x, onValueChange = { currentBarrier.size.x = it.coerceAtLeast(0.2f); onUpdate() }, modifier = Modifier.weight(1f))
                        CoordinateNudge(label = "Tinggi (Y)", value = currentBarrier.size.y, onValueChange = { currentBarrier.size.y = it.coerceAtLeast(0.2f); onUpdate() }, modifier = Modifier.weight(1f))
                        CoordinateNudge(label = "Panjang (Z)", value = currentBarrier.size.z, onValueChange = { currentBarrier.size.z = it.coerceAtLeast(0.2f); onUpdate() }, modifier = Modifier.weight(1f))
                    }

                    // Quick Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = {
                                val p = physicsEngine.characterPos
                                currentBarrier.position.set(p.x, p.y + currentBarrier.size.y / 2f, p.z)
                                onUpdate()
                                Toast.makeText(context, "Objek dipindahkan ke karakter!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2B47)),
                            modifier = Modifier.weight(1f).height(30.dp)
                        ) {
                            Text("📍 Tarik ke Pos Karakter", fontSize = 9.sp, color = Color.White)
                        }

                        Button(
                            onClick = {
                                physicsEngine.characterPos.set(currentBarrier.position.x, currentBarrier.position.y + 2f, currentBarrier.position.z)
                                onUpdate()
                                Toast.makeText(context, "Karakter diteleportasi ke objek!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2B47)),
                            modifier = Modifier.weight(1f).height(30.dp)
                        ) {
                            Text("🌀 Teleport Karakter ke Sini", fontSize = 9.sp, color = Color(0xFF00E5FF))
                        }
                    }
                }
            }
        }
    }
}

/** Tab 2: NPC, Interaksi & Portal **/
@Composable
private fun NpcAndInteractEditorTab(
    npcManager: NpcManager,
    interactionSystem: InteractionSystem,
    physicsEngine: PhysicsEngine,
    selectedNpcId: String?,
    selectedInteractableId: String?,
    onSelectNpc: (String?) -> Unit,
    onSelectInteractable: (String?) -> Unit,
    onUpdate: () -> Unit
) {
    val context = LocalContext.current
    val currentNpc = npcManager.npcs.firstOrNull { it.id == selectedNpcId }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Kelola NPC & Karakter Interaktif (${npcManager.npcs.size} NPC):",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = Color(0xFF76FF03)
            )

            Button(
                onClick = {
                    val p = physicsEngine.characterPos
                    val newId = "npc_${System.currentTimeMillis()}"
                    val newNpc = NpcEntity(
                        id = newId,
                        name = "NPC Baru #${npcManager.npcs.size + 1}",
                        role = "Penduduk",
                        position = Vec3(p.x + 1f, p.y, p.z + 1f),
                        dialogues = mutableListOf("Halo traveler! Aku baru saja ditempatkan di sini.")
                    )
                    npcManager.addNpc(newNpc)
                    onSelectNpc(newId)
                    onUpdate()
                    Toast.makeText(context, "👤 NPC baru ditambahkan di posisi karakter!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF76FF03)),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text("+ Buat NPC", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Chip list of NPCs
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            npcManager.npcs.forEach { npc ->
                val isSel = npc.id == selectedNpcId
                FilterChip(
                    selected = isSel,
                    onClick = { onSelectNpc(npc.id) },
                    label = { Text("${npc.name} (${npc.role})", fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF76FF03),
                        selectedLabelColor = Color.Black
                    )
                )
            }
        }

        // Inspector for current NPC
        if (currentNpc != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131D31)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF76FF03).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("👤 Edit Data NPC: ${currentNpc.name}", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF76FF03))
                        IconButton(
                            onClick = {
                                npcManager.removeNpc(currentNpc)
                                onSelectNpc(null)
                                onUpdate()
                                Toast.makeText(context, "NPC dihapus!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp))
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CoordinateNudge(label = "Pos X", value = currentNpc.position.x, onValueChange = { currentNpc.position.x = it; onUpdate() }, modifier = Modifier.weight(1f))
                        CoordinateNudge(label = "Pos Y", value = currentNpc.position.y, onValueChange = { currentNpc.position.y = it; onUpdate() }, modifier = Modifier.weight(1f))
                        CoordinateNudge(label = "Pos Z", value = currentNpc.position.z, onValueChange = { currentNpc.position.z = it; onUpdate() }, modifier = Modifier.weight(1f))
                    }

                    Button(
                        onClick = {
                            val p = physicsEngine.characterPos
                            currentNpc.position.set(p.x + 1f, p.y, p.z + 1f)
                            onUpdate()
                            Toast.makeText(context, "NPC dipindahkan ke dekat karakter!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2B47)),
                        modifier = Modifier.fillMaxWidth().height(28.dp)
                    ) {
                        Text("📍 Tarik NPC ke Karakter", fontSize = 10.sp, color = Color.White)
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // 2. Section: Portal & Benda Interaksi
        val currentInteractable = interactionSystem.interactables.firstOrNull { it.id == selectedInteractableId }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🌀 Portal Teleportasi & Objek (${interactionSystem.interactables.size} Objek):",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = Color(0xFF00E5FF)
            )

            Button(
                onClick = {
                    val p = physicsEngine.characterPos
                    val newId = "portal_${System.currentTimeMillis()}"
                    val newPortal = WorldInteractable(
                        id = newId,
                        name = "Portal #${interactionSystem.interactables.size + 1}",
                        type = InteractableType.QUANTUM_PORTAL,
                        position = Vec3(p.x + 2f, p.y, p.z),
                        targetTeleportPos = Vec3(0f, 1.5f, 0f),
                        promptText = "Teleportasi ke Titik Pusat"
                    )
                    interactionSystem.interactables.add(newPortal)
                    onSelectInteractable(newId)
                    onUpdate()
                    Toast.makeText(context, "🌀 Portal baru ditambahkan!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Text("+ Buat Portal", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Chip list of Portals / Interactables
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            interactionSystem.interactables.forEach { item ->
                val isSel = item.id == selectedInteractableId
                val isPortal = item.targetTeleportPos != null || item.type == InteractableType.QUANTUM_PORTAL
                FilterChip(
                    selected = isSel,
                    onClick = { onSelectInteractable(item.id) },
                    label = { Text("${if (isPortal) "🌀" else "📦"} ${item.name}", fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF00E5FF),
                        selectedLabelColor = Color.Black
                    )
                )
            }
        }

        // Inspector for current Interactable / Portal
        if (currentInteractable != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF101B2E)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🌀 Edit Data: ${currentInteractable.name}", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF00E5FF))
                        IconButton(
                            onClick = {
                                interactionSystem.interactables.remove(currentInteractable)
                                onSelectInteractable(null)
                                onUpdate()
                                Toast.makeText(context, "Objek dihapus!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp))
                        }
                    }

                    Text("Posisi Portal Saat Ini (Meter):", fontSize = 9.sp, color = Color(0xFF90A4AE))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CoordinateNudge(label = "X", value = currentInteractable.position.x, onValueChange = { currentInteractable.position.x = it; onUpdate() }, modifier = Modifier.weight(1f))
                        CoordinateNudge(label = "Y", value = currentInteractable.position.y, onValueChange = { currentInteractable.position.y = it; onUpdate() }, modifier = Modifier.weight(1f))
                        CoordinateNudge(label = "Z", value = currentInteractable.position.z, onValueChange = { currentInteractable.position.z = it; onUpdate() }, modifier = Modifier.weight(1f))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = {
                                val p = physicsEngine.characterPos
                                currentInteractable.position.set(p.x, p.y, p.z)
                                onUpdate()
                                Toast.makeText(context, "Portal ditarik ke posisi karakter!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2B47)),
                            modifier = Modifier.weight(1f).height(28.dp)
                        ) {
                            Text("📍 Tarik ke Karakter", fontSize = 9.sp, color = Color.White)
                        }

                        Button(
                            onClick = {
                                physicsEngine.characterPos.set(currentInteractable.position.x, currentInteractable.position.y + 1f, currentInteractable.position.z)
                                onUpdate()
                                Toast.makeText(context, "🌀 Karakter diteleportasi ke portal!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                            modifier = Modifier.weight(1f).height(28.dp)
                        ) {
                            Text("🌀 Teleport ke Sini", fontSize = 9.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/** Tab 3: Fisika Karakter & Gameplay Rules **/
@Composable
private fun PhysicsPlayerEditorTab(
    physicsEngine: PhysicsEngine,
    onUpdate: () -> Unit
) {
    val context = LocalContext.current
    var isGodMode by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("⚙️ Pengaturan Mesin Fisika & Parameter Karakter:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFFFFD600))

        // Walk Speed
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Kecepatan Jalan: %.1f m/s".format(physicsEngine.walkSpeed), fontSize = 10.sp, color = Color.White, modifier = Modifier.weight(1f))
            Slider(
                value = physicsEngine.walkSpeed,
                onValueChange = { physicsEngine.walkSpeed = it; onUpdate() },
                valueRange = 1.0f..20.0f,
                colors = SliderDefaults.colors(thumbColor = Color(0xFF00E5FF)),
                modifier = Modifier.weight(1.5f)
            )
        }

        // Sprint Speed
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Kecepatan Lari: %.1f m/s".format(physicsEngine.sprintSpeed), fontSize = 10.sp, color = Color.White, modifier = Modifier.weight(1f))
            Slider(
                value = physicsEngine.sprintSpeed,
                onValueChange = { physicsEngine.sprintSpeed = it; onUpdate() },
                valueRange = 2.0f..40.0f,
                colors = SliderDefaults.colors(thumbColor = Color(0xFF76FF03)),
                modifier = Modifier.weight(1.5f)
            )
        }

        // Jump Impulse
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Daya Lompat: %.1f m/s".format(physicsEngine.jumpImpulse), fontSize = 10.sp, color = Color.White, modifier = Modifier.weight(1f))
            Slider(
                value = physicsEngine.jumpImpulse,
                onValueChange = { physicsEngine.jumpImpulse = it; onUpdate() },
                valueRange = 2.0f..30.0f,
                colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD600)),
                modifier = Modifier.weight(1.5f)
            )
        }

        // Gravity
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Gravitasi Dunia: %.1f".format(physicsEngine.gravity), fontSize = 10.sp, color = Color.White, modifier = Modifier.weight(1f))
            Slider(
                value = physicsEngine.gravity,
                onValueChange = { physicsEngine.gravity = it; onUpdate() },
                valueRange = -50.0f..-2.0f,
                colors = SliderDefaults.colors(thumbColor = Color(0xFFFF5252)),
                modifier = Modifier.weight(1.5f)
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    physicsEngine.spawnPhysicsBox(physicsEngine.characterPos)
                    onUpdate()
                    Toast.makeText(context, "+ Balok Fisika ditambahkan di dekat karakter!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                modifier = Modifier.weight(1f).height(32.dp)
            ) {
                Text("📦 +Balok Fisika", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 10.sp)
            }

            Button(
                onClick = {
                    physicsEngine.rigidBodies.clear()
                    onUpdate()
                    Toast.makeText(context, "Semua balok fisika dibersihkan!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF37474F)),
                modifier = Modifier.weight(1f).height(32.dp)
            ) {
                Text("🧹 Bersihkan Balok", color = Color.White, fontSize = 10.sp)
            }
        }
    }
}

/** Tab 4: Cuaca, Waktu & Pencahayaan **/
@Composable
private fun LightingEnvEditorTab(
    renderer: Apex3DRenderer,
    settings: EngineSettings,
    onUpdate: () -> Unit
) {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("☀️ Pengaturan Cuaca, Waktu & Efek Grafis:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF00E5FF))

        // Preset Time of Day
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                "☀️ Siang" to listOf(45f, 65f, 0.35f, 0.38f, 0.42f, 0.012f),
                "🌅 Senja" to listOf(85f, 20f, 0.50f, 0.28f, 0.18f, 0.022f),
                "🌌 Malam" to listOf(30f, 15f, 0.10f, 0.12f, 0.22f, 0.025f),
                "🌫️ Kabut" to listOf(45f, 40f, 0.30f, 0.35f, 0.35f, 0.055f)
            ).forEach { (label, data) ->
                Button(
                    onClick = {
                        settings.sunAzimuth = data[0]
                        settings.sunElevation = data[1]
                        renderer.lighting.sunAzimuthDeg = data[0]
                        renderer.lighting.sunElevationDeg = data[1]
                        renderer.lighting.ambientColor = floatArrayOf(data[2], data[3], data[4])
                        settings.fogDensity = data[5]
                        onUpdate()
                        Toast.makeText(context, "Preset $label diaktifkan!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2B47)),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.weight(1f).height(30.dp)
                ) {
                    Text(label, fontSize = 9.sp, color = Color.White)
                }
            }
        }

        // Fog Switch & Density
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Kabut Atmosfer Dunia:", fontSize = 10.sp, color = Color.White)
            Switch(
                checked = settings.enableFog,
                onCheckedChange = { settings.enableFog = it; onUpdate() },
                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF))
            )
        }

        // Wireframe Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Tampilkan Wireframe Poligon:", fontSize = 10.sp, color = Color.White)
            Switch(
                checked = settings.enableWireframe,
                onCheckedChange = { settings.enableWireframe = it; onUpdate() },
                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF))
            )
        }
    }
}

/** Tab 5: Tata Letak & Editor Seluruh HUD **/
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HudLayoutEditorTab(
    hudConfigs: Map<HudControlId, HudElementConfig>,
    hudPreferences: HudPreferences,
    activePresetName: String,
    onHudConfigsUpdated: (Map<HudControlId, HudElementConfig>) -> Unit,
    onOpenFullHudEditor: () -> Unit
) {
    val context = LocalContext.current
    var selectedHudId by remember { mutableStateOf<HudControlId?>(HudControlId.JOYSTICK) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Direct Button to full interactive Drag & Drop HUD Editor
        Button(
            onClick = onOpenFullHudEditor,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
            modifier = Modifier.fillMaxWidth().height(36.dp).testTag("open_drag_hud_editor_button")
        ) {
            Icon(Icons.Default.VideogameAsset, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("🎮 Buka Editor Drag & Drop Sentuh Penuh HUD", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }

        // Quick Preset Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Pilihan Preset Tata Letak HUD:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF80D8FF))

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(
                    "DEFAULT_2_FINGER" to "Standar",
                    "CLAW_3_FINGER" to "3-Jari",
                    "PUBG_4_FINGER_CLAW" to "4-Jari",
                    "LEFT_HANDED" to "Kidal"
                ).forEach { (presetKey, presetLabel) ->
                    val isCur = activePresetName == presetKey
                    FilterChip(
                        selected = isCur,
                        onClick = {
                            val presetMap = HudPresets.getPreset(presetKey).mapValues { it.value.copy() }
                            hudPreferences.saveLayout(presetKey, presetMap)
                            hudPreferences.setActivePresetName(presetKey)
                            onHudConfigsUpdated(presetMap)
                            Toast.makeText(context, "Preset HUD diubah: $presetLabel", Toast.LENGTH_SHORT).show()
                        },
                        label = { Text(presetLabel, fontSize = 8.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF00E5FF),
                            selectedLabelColor = Color.Black
                        )
                    )
                }
            }
        }

        // Element Selection List
        Text("Pilih Elemen HUD (${hudConfigs.size} Elemen Tersedia):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            hudConfigs.forEach { (id, cfg) ->
                val isSel = selectedHudId == id
                FilterChip(
                    selected = isSel,
                    onClick = { selectedHudId = id },
                    label = { Text(id.displayName, fontSize = 9.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = if (cfg.isEnabled) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            modifier = Modifier.size(11.dp),
                            tint = if (isSel) Color.Black else if (cfg.isEnabled) Color(0xFF00E5FF) else Color.Gray
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF00E5FF),
                        selectedLabelColor = Color.Black,
                        containerColor = Color(0xFF16233B)
                    )
                )
            }
        }

        // Detailed Inspector for Selected HUD Element
        val activeCfg = selectedHudId?.let { hudConfigs[it] }
        if (activeCfg != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131D30)),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Konfigurasi: ${activeCfg.id.displayName}",
                            color = Color(0xFF00E5FF),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Aktif:", fontSize = 10.sp, color = Color(0xFF90A4AE))
                            Switch(
                                checked = activeCfg.isEnabled,
                                onCheckedChange = { en ->
                                    val updated = hudConfigs.toMutableMap()
                                    updated[activeCfg.id] = activeCfg.copy(isEnabled = en)
                                    onHudConfigsUpdated(updated)
                                    hudPreferences.saveLayout(activePresetName, updated)
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF))
                            )
                        }
                    }

                    // Posisi X & Y Sliders
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Posisi X (Horisontal): ${(activeCfg.xPercent * 100).roundToInt()}%", fontSize = 9.sp, color = Color(0xFF90A4AE))
                            Slider(
                                value = activeCfg.xPercent,
                                onValueChange = { newX ->
                                    val updated = hudConfigs.toMutableMap()
                                    updated[activeCfg.id] = activeCfg.copy(xPercent = newX)
                                    onHudConfigsUpdated(updated)
                                    hudPreferences.saveLayout(activePresetName, updated)
                                },
                                valueRange = 0.02f..0.98f,
                                colors = SliderDefaults.colors(thumbColor = Color(0xFF00E5FF), activeTrackColor = Color(0xFF00E5FF))
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Posisi Y (Vertikal): ${(activeCfg.yPercent * 100).roundToInt()}%", fontSize = 9.sp, color = Color(0xFF90A4AE))
                            Slider(
                                value = activeCfg.yPercent,
                                onValueChange = { newY ->
                                    val updated = hudConfigs.toMutableMap()
                                    updated[activeCfg.id] = activeCfg.copy(yPercent = newY)
                                    onHudConfigsUpdated(updated)
                                    hudPreferences.saveLayout(activePresetName, updated)
                                },
                                valueRange = 0.02f..0.98f,
                                colors = SliderDefaults.colors(thumbColor = Color(0xFF00E5FF), activeTrackColor = Color(0xFF00E5FF))
                            )
                        }
                    }

                    // Ukuran (Scale) & Opasitas (Alpha) Sliders
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Ukuran Skala: ${(activeCfg.scale * 100).roundToInt()}%", fontSize = 9.sp, color = Color(0xFF90A4AE))
                            Slider(
                                value = activeCfg.scale,
                                onValueChange = { newScale ->
                                    val updated = hudConfigs.toMutableMap()
                                    updated[activeCfg.id] = activeCfg.copy(scale = newScale)
                                    onHudConfigsUpdated(updated)
                                    hudPreferences.saveLayout(activePresetName, updated)
                                },
                                valueRange = 0.5f..1.8f,
                                colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD600), activeTrackColor = Color(0xFFFFD600))
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Transparansi / Opasitas: ${(activeCfg.alpha * 100).roundToInt()}%", fontSize = 9.sp, color = Color(0xFF90A4AE))
                            Slider(
                                value = activeCfg.alpha,
                                onValueChange = { newAlpha ->
                                    val updated = hudConfigs.toMutableMap()
                                    updated[activeCfg.id] = activeCfg.copy(alpha = newAlpha)
                                    onHudConfigsUpdated(updated)
                                    hudPreferences.saveLayout(activePresetName, updated)
                                },
                                valueRange = 0.2f..1.0f,
                                colors = SliderDefaults.colors(thumbColor = Color(0xFF76FF03), activeTrackColor = Color(0xFF76FF03))
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Tab 6: Simpan & Ekspor Level **/
@Composable
private fun SaveLevelEditorTab(
    sceneManager: GameplaySceneManager,
    exportManager: SampleExportManager,
    physicsEngine: PhysicsEngine,
    barrierManager: BarrierManager,
    npcManager: NpcManager,
    interactionSystem: InteractionSystem,
    renderer: Apex3DRenderer,
    settings: EngineSettings,
    onReload: () -> Unit
) {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "💾 Manajemen Penyimpanan & Ekspor Dunia:",
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = Color(0xFF00E676)
        )
        Text(
            text = "Simpan seluruh perubahan rintangan, posisi NPC, setting cuaca, dan fisika agar tetap aktif saat game dibuka kembali.",
            fontSize = 10.sp,
            color = Color(0xFF90A4AE)
        )

        Button(
            onClick = {
                val ok = sceneManager.saveScene(physicsEngine, barrierManager, npcManager, interactionSystem, renderer, settings)
                if (ok) {
                    Toast.makeText(context, "✓ Berhasil menyimpan seluruh konfigurasi gameplay & dunia!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Gagal menyimpan konfigurasi gameplay.", Toast.LENGTH_SHORT).show()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
            modifier = Modifier.fillMaxWidth().height(36.dp).testTag("save_gameplay_scene_button")
        ) {
            Icon(Icons.Default.Save, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Simpan Seluruh Konfigurasi Level & Dunia", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }

        Button(
            onClick = {
                val res = sceneManager.exportSceneToPublicFolder(exportManager)
                Toast.makeText(context, res, Toast.LENGTH_LONG).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
            modifier = Modifier.fillMaxWidth().height(36.dp)
        ) {
            Icon(Icons.Default.Share, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Ekspor Level JSON ke Download/Apex3D", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }

        OutlinedButton(
            onClick = {
                sceneManager.resetToDefaults(physicsEngine, barrierManager, npcManager, interactionSystem, renderer, settings)
                onReload()
                Toast.makeText(context, "Dunia berhasil direset ke pengaturan awal!", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth().height(36.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Reset Level & Dunia ke Standar Bawaan", color = Color(0xFFFF5252), fontSize = 11.sp)
        }
    }
}

/** Komponen Nudge Koordinat Ringan **/
@Composable
private fun CoordinateNudge(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF0E1626),
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF263552)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 9.sp, color = Color(0xFF80D8FF), fontWeight = FontWeight.Bold)
            Text("%.1f".format(value), fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onValueChange(value - 1.0f) },
                    modifier = Modifier.size(20.dp)
                ) {
                    Text("-", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                IconButton(
                    onClick = { onValueChange(value + 1.0f) },
                    modifier = Modifier.size(20.dp)
                ) {
                    Text("+", color = Color(0xFF76FF03), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}
