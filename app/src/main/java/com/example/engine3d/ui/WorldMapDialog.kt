package com.example.engine3d.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.engine3d.actions.InteractableType
import com.example.engine3d.actions.InteractionSystem
import com.example.engine3d.actions.WorldInteractable
import com.example.engine3d.math.Vec3
import com.example.engine3d.npc.NpcManager
import com.example.engine3d.physics.BarrierType
import com.example.engine3d.physics.PhysicsEngine
import kotlin.math.*

/**
 * Dialog Peta Dunia Penuh (Full World Map & Teleportation Hub).
 * Menampilkan seluruh peta dunia secara visual dari atas (orbit),
 * menandai posisi pemain, arah hadap, semua titik teleportasi & portal,
 * serta menyediakan tombol teleportasi instan (fast-travel).
 */
@Composable
fun WorldMapDialog(
    physicsEngine: PhysicsEngine,
    interactionSystem: InteractionSystem,
    npcManager: NpcManager,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val playerPos = physicsEngine.characterPos
    val playerYaw = physicsEngine.characterYawDeg

    // Filter all teleport points / portals
    val teleportPoints = interactionSystem.interactables.filter {
        it.type == InteractableType.QUANTUM_PORTAL ||
        it.type == InteractableType.HOUSE_DOOR_ENTER ||
        it.type == InteractableType.HOUSE_DOOR_EXIT ||
        it.targetTeleportPos != null
    }

    var selectedInteractable by remember { mutableStateOf<WorldInteractable?>(teleportPoints.firstOrNull()) }
    var mapZoom by remember { mutableStateOf(1f) }

    // World boundaries: -130 to 130 meters (Total 260m)
    val worldMin = -130f
    val worldMax = 130f
    val worldSize = worldMax - worldMin

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xF5060A12)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF00E5FF).copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, Color(0xFF00E5FF)),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🗺️", fontSize = 16.sp)
                            }
                        }
                        Column {
                            Text(
                                text = "Peta Dunia & Titik Teleportasi",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF00E5FF)
                            )
                            Text(
                                text = "Posisi: (X: ${String.format("%.1f", playerPos.x)}, Z: ${String.format("%.1f", playerPos.z)}) • ${teleportPoints.size} Titik Teleportasi Terdeteksi",
                                fontSize = 10.sp,
                                color = Color(0xFFB0BEC5)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Quick Fast-Travel Buttons
                        Button(
                            onClick = {
                                playerPos.set(0f, 1.5f, 0f)
                                Toast.makeText(context, "📍 Diteleportasi ke Titik Pusat (0, 0)!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2B47)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("📍 Pusat (0,0)", fontSize = 10.sp, color = Color.White)
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0xFF162235), CircleShape)
                                .border(1.dp, Color(0xFF37474F), CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup Peta", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Main Layout: Map Canvas (Left/Top) + Portal Quick Drawer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Interactive 2D World Map Radar Canvas
                    Box(
                        modifier = Modifier
                            .weight(1.3f)
                            .fillMaxHeight()
                            .background(Color(0xFF080D18), RoundedCornerShape(10.dp))
                            .border(1.5.dp, Color(0xFF00E5FF), RoundedCornerShape(10.dp))
                            .clip(RoundedCornerShape(10.dp))
                            .pointerInput(teleportPoints) {
                                detectTapGestures { offset ->
                                    val canvasW = size.width.toFloat()
                                    val canvasH = size.height.toFloat()

                                    // Convert screen pixel to world coordinates
                                    val wx = (offset.x / canvasW) * worldSize + worldMin
                                    val wz = (offset.y / canvasH) * worldSize + worldMin

                                    // Check if tapped near any teleport point
                                    val clickedPoint = teleportPoints.minByOrNull { item ->
                                        val dx = item.position.x - wx
                                        val dz = item.position.z - wz
                                        sqrt(dx * dx + dz * dz)
                                    }

                                    if (clickedPoint != null) {
                                        val dx = clickedPoint.position.x - wx
                                        val dz = clickedPoint.position.z - wz
                                        val dist = sqrt(dx * dx + dz * dz)
                                        if (dist < 18f) {
                                            selectedInteractable = clickedPoint
                                            Toast.makeText(context, "🌀 Titik dipilih: ${clickedPoint.name}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val canvasW = size.width
                            val canvasH = size.height
                            val cx = canvasW / 2f
                            val cy = canvasH / 2f

                            // Helper coordinate conversion
                            fun worldToCanvas(wx: Float, wz: Float): Offset {
                                val x = ((wx - worldMin) / worldSize) * canvasW
                                val y = ((wz - worldMin) / worldSize) * canvasH
                                return Offset(x, y)
                            }

                            // 1. Concentric radar range rings (every 25 meters)
                            val ringDistances = listOf(25f, 50f, 75f, 100f, 125f)
                            ringDistances.forEach { dist ->
                                val rRadius = (dist / (worldSize / 2f)) * (canvasW / 2f)
                                drawCircle(
                                    color = Color(0x1800E5FF),
                                    radius = rRadius,
                                    center = Offset(cx, cy),
                                    style = Stroke(width = 1f)
                                )
                            }

                            // 2. Coordinate axes crosshairs
                            drawLine(Color(0x2200E5FF), Offset(0f, cy), Offset(canvasW, cy), strokeWidth = 1f)
                            drawLine(Color(0x2200E5FF), Offset(cx, 0f), Offset(cx, canvasH), strokeWidth = 1f)

                            // 3. World border limits (260x260m)
                            val boundMin = worldToCanvas(-120f, -120f)
                            val boundMax = worldToCanvas(120f, 120f)
                            drawRect(
                                color = Color(0x66FF5252),
                                topLeft = boundMin,
                                size = Size(boundMax.x - boundMin.x, boundMax.y - boundMin.y),
                                style = Stroke(width = 2f)
                            )

                            // 4. Draw World Barriers / Walls
                            physicsEngine.barrierManager.barriers.forEach { b ->
                                if (b.type == BarrierType.WALL_BARRIER) {
                                    val halfX = b.size.x * 0.5f
                                    val halfZ = b.size.z * 0.5f
                                    val minP = worldToCanvas(b.position.x - halfX, b.position.z - halfZ)
                                    val maxP = worldToCanvas(b.position.x + halfX, b.position.z + halfZ)
                                    drawRect(
                                        color = if (b.id.startsWith("world_limit")) Color(0x33FF5252) else Color(0x88FFD600),
                                        topLeft = minP,
                                        size = Size(maxP.x - minP.x, maxP.y - minP.y)
                                    )
                                }
                            }

                            // 5. Draw NPCs (yellow dots)
                            npcManager.npcs.forEach { npc ->
                                val pos = worldToCanvas(npc.position.x, npc.position.z)
                                drawCircle(Color(0xFFFFD600), radius = 5f, center = pos)
                            }

                            // 6. DRAW ALL TELEPORTATION POINTS / PORTALS (Vivid Cyan/Magenta Glow!)
                            teleportPoints.forEach { portal ->
                                val pos = worldToCanvas(portal.position.x, portal.position.z)
                                val isSelected = portal.id == selectedInteractable?.id

                                // Outer glowing aura ring
                                drawCircle(
                                    color = if (isSelected) Color(0x8800E5FF) else Color(0x44E040FB),
                                    radius = if (isSelected) 16f else 12f,
                                    center = pos
                                )
                                // Mid ring
                                drawCircle(
                                    color = if (isSelected) Color(0xFF00E5FF) else Color(0xFFE040FB),
                                    radius = if (isSelected) 10f else 7f,
                                    center = pos,
                                    style = Stroke(width = 2.5f)
                                )
                                // Core bright dot
                                drawCircle(
                                    color = Color.White,
                                    radius = if (isSelected) 4f else 2.5f,
                                    center = pos
                                )
                            }

                            // 7. DRAW PLAYER POSITION & REAL-TIME HEADING ARROW
                            val pOffset = worldToCanvas(playerPos.x, playerPos.z)
                            // Player pulsing green aura
                            drawCircle(Color(0x6676FF03), radius = 12f, center = pOffset)
                            drawCircle(Color(0xFF76FF03), radius = 6f, center = pOffset)

                            // Player direction cone / pointer
                            val rad = Math.toRadians((playerYaw - 90.0)).toFloat()
                            val pointerLen = 16f
                            val tipX = pOffset.x + cos(rad) * pointerLen
                            val tipY = pOffset.y + sin(rad) * pointerLen

                            val arrowPath = Path().apply {
                                moveTo(tipX, tipY)
                                val leftRad = rad + 2.5f
                                val rightRad = rad - 2.5f
                                lineTo(pOffset.x + cos(leftRad) * 8f, pOffset.y + sin(leftRad) * 8f)
                                lineTo(pOffset.x + cos(rightRad) * 8f, pOffset.y + sin(rightRad) * 8f)
                                close()
                            }
                            drawPath(arrowPath, Color(0xFF76FF03))
                        }

                        // Map Legend & Compass overlay
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                                .background(Color(0xCC060A12), RoundedCornerShape(6.dp))
                                .border(1.dp, Color(0xFF1E2B47), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("🧭 UTARA (N: Atas)", color = Color(0xFFFF5252), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(modifier = Modifier.size(7.dp).background(Color(0xFF76FF03), CircleShape))
                                    Text("Pemain", color = Color.White, fontSize = 8.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(modifier = Modifier.size(7.dp).background(Color(0xFF00E5FF), CircleShape))
                                    Text("Portal Teleportasi", color = Color(0xFF00E5FF), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // 2. Right Portal Quick-Selection & Fast Travel Drawer
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1726)),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF1E2B47))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "🌀 Titik Teleportasi (${teleportPoints.size}):",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00E5FF)
                            )
                            Text(
                                text = "Pilih portal untuk melihat detail & langsung teleportasi:",
                                fontSize = 9.sp,
                                color = Color(0xFF90A4AE)
                            )

                            // Scrollable list of teleport portals
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (teleportPoints.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 20.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Belum ada portal teleportasi.", color = Color.Gray, fontSize = 11.sp)
                                    }
                                } else {
                                    teleportPoints.forEach { portal ->
                                        val isSel = portal.id == selectedInteractable?.id
                                        val dist = sqrt(
                                            (portal.position.x - playerPos.x).pow(2) +
                                            (portal.position.z - playerPos.z).pow(2)
                                        )

                                        Surface(
                                            onClick = { selectedInteractable = portal },
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSel) Color(0xFF132B45) else Color(0xFF162032),
                                            border = BorderStroke(
                                                1.dp,
                                                if (isSel) Color(0xFF00E5FF) else Color(0xFF263238)
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Text("🌀", fontSize = 12.sp)
                                                        Text(
                                                            text = portal.name,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isSel) Color(0xFF00E5FF) else Color.White
                                                        )
                                                    }
                                                    Text(
                                                        text = "Posisi: (${String.format("%.0f", portal.position.x)}, ${String.format("%.0f", portal.position.z)}) • Jarak: ${String.format("%.0f", dist)}m",
                                                        fontSize = 9.sp,
                                                        color = Color(0xFF80D8FF)
                                                    )
                                                    if (portal.targetTeleportPos != null) {
                                                        Text(
                                                            text = "Tujuan: (${String.format("%.0f", portal.targetTeleportPos.x)}, ${String.format("%.0f", portal.targetTeleportPos.z)})",
                                                            fontSize = 8.sp,
                                                            color = Color(0xFFB0BEC5)
                                                        )
                                                    }
                                                }

                                                // Quick Teleport Button
                                                Button(
                                                    onClick = {
                                                        playerPos.set(portal.position.x, portal.position.y + 1f, portal.position.z)
                                                        Toast.makeText(context, "🌀 Berhasil diteleportasi ke ${portal.name}!", Toast.LENGTH_SHORT).show()
                                                        onDismiss()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(28.dp).testTag("teleport_button_${portal.id}")
                                                ) {
                                                    Text("Teleport", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Add New Portal at Player's current location button!
                            Button(
                                onClick = {
                                    val newPortalId = "portal_${System.currentTimeMillis()}"
                                    val newPortal = WorldInteractable(
                                        id = newPortalId,
                                        name = "Portal Pemain #${teleportPoints.size + 1}",
                                        type = InteractableType.QUANTUM_PORTAL,
                                        position = Vec3(playerPos.x, playerPos.y, playerPos.z),
                                        targetTeleportPos = Vec3(0f, 1.5f, 0f),
                                        promptText = "Teleportasi ke Titik Pusat"
                                    )
                                    interactionSystem.interactables.add(newPortal)
                                    selectedInteractable = newPortal
                                    Toast.makeText(context, "🌀 Titik Teleportasi Baru dibuat di lokasi Anda!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2B47)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(34.dp)
                            ) {
                                Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("+ Tandai Teleport di Sini", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
