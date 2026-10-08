package com.example.engine3d.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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

    val teleportPoints = interactionSystem.interactables.filter {
        it.type == InteractableType.QUANTUM_PORTAL ||
        it.type == InteractableType.HOUSE_DOOR_ENTER ||
        it.type == InteractableType.HOUSE_DOOR_EXIT ||
        it.targetTeleportPos != null
    }

    var selectedInteractable by remember { mutableStateOf<WorldInteractable?>(teleportPoints.firstOrNull()) }
    val worldMin = -130f
    val worldMax = 130f
    val worldSize = worldMax - worldMin

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xF8060A12)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "🗺️ Peta Dunia & Titik Teleportasi",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF00E5FF)
                        )
                        Text(
                            text = "Posisi: [${String.format("%.1f", playerPos.x)}, ${String.format("%.1f", playerPos.z)}] • ${teleportPoints.size} Portal",
                            fontSize = 10.sp,
                            color = Color(0xFFB0BEC5)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
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

                        IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Radar Canvas dengan proyeksi 1:1 isometric
                    Box(
                        modifier = Modifier
                            .weight(1.3f)
                            .fillMaxHeight()
                            .background(Color(0xFF080D18), RoundedCornerShape(10.dp))
                            .border(1.2.dp, Color(0xFF00E5FF), RoundedCornerShape(10.dp))
                            .clip(RoundedCornerShape(10.dp))
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(teleportPoints) {
                                    detectTapGestures { offset ->
                                        val radarDim = min(size.width, size.height)
                                        val offX = (size.width - radarDim) / 2f
                                        val offY = (size.height - radarDim) / 2f
                                        val wx = ((offset.x - offX) / radarDim) * worldSize + worldMin
                                        val wz = ((offset.y - offY) / radarDim) * worldSize + worldMin

                                        val clicked = teleportPoints.minByOrNull {
                                            val dx = it.position.x - wx
                                            val dz = it.position.z - wz
                                            sqrt(dx * dx + dz * dz)
                                        }
                                        if (clicked != null) {
                                            selectedInteractable = clicked
                                            Toast.makeText(context, "🌀 Titik dipilih: ${clicked.name}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                        ) {
                            val radarDim = min(size.width, size.height)
                            val offX = (size.width - radarDim) / 2f
                            val offY = (size.height - radarDim) / 2f
                            val cx = size.width / 2f
                            val cy = size.height / 2f

                            fun worldToCanvas(wx: Float, wz: Float): Offset {
                                val nx = (wx - worldMin) / worldSize
                                val nz = (wz - worldMin) / worldSize
                                return Offset(offX + nx * radarDim, offY + nz * radarDim)
                            }

                            // Range rings bulat 1:1
                            listOf(25f, 50f, 75f, 100f, 120f).forEach { dist ->
                                val rRadius = (dist / (worldSize / 2f)) * (radarDim / 2f)
                                drawCircle(Color(0x1800E5FF), radius = rRadius, center = Offset(cx, cy), style = Stroke(width = 1f))
                            }

                            // Crosshairs
                            drawLine(Color(0x2200E5FF), Offset(cx - radarDim / 2f, cy), Offset(cx + radarDim / 2f, cy), strokeWidth = 1f)
                            drawLine(Color(0x2200E5FF), Offset(cx, cy - radarDim / 2f), Offset(cx, cy + radarDim / 2f), strokeWidth = 1f)

                            // World Border Limits
                            val boundMin = worldToCanvas(-120f, -120f)
                            val boundMax = worldToCanvas(120f, 120f)
                            drawRect(Color(0x55FF5252), topLeft = boundMin, size = Size(boundMax.x - boundMin.x, boundMax.y - boundMin.y), style = Stroke(width = 1.5f))

                            // Barriers
                            physicsEngine.barrierManager.barriers.forEach { b ->
                                if (b.type == BarrierType.WALL_BARRIER) {
                                    val halfX = b.size.x * 0.5f
                                    val halfZ = b.size.z * 0.5f
                                    val minP = worldToCanvas(b.position.x - halfX, b.position.z - halfZ)
                                    val maxP = worldToCanvas(b.position.x + halfX, b.position.z + halfZ)
                                    drawRect(Color(0x66FFD600), topLeft = minP, size = Size(maxP.x - minP.x, maxP.y - minP.y))
                                }
                            }

                            // NPCs
                            npcManager.npcs.forEach { npc ->
                                drawCircle(Color(0xFFFFD600), radius = 4.5f, center = worldToCanvas(npc.position.x, npc.position.z))
                            }

                            // Portals
                            teleportPoints.forEach { portal ->
                                val pos = worldToCanvas(portal.position.x, portal.position.z)
                                val isSel = portal.id == selectedInteractable?.id
                                drawCircle(if (isSel) Color(0xFF00E5FF) else Color(0xFFE040FB), radius = if (isSel) 8f else 6f, center = pos)
                                drawCircle(Color.White, radius = 2.5f, center = pos)
                            }

                            // Player Position & Heading
                            val pPos = worldToCanvas(playerPos.x, playerPos.z)
                            drawCircle(Color(0x6676FF03), radius = 10f, center = pPos)
                            drawCircle(Color(0xFF76FF03), radius = 5f, center = pPos)

                            val rad = Math.toRadians((playerYaw - 90.0)).toFloat()
                            val tipX = pPos.x + cos(rad) * 14f
                            val tipY = pPos.y + sin(rad) * 14f
                            val path = Path().apply {
                                moveTo(tipX, tipY)
                                lineTo(pPos.x + cos(rad + 2.5f) * 6f, pPos.y + sin(rad + 2.5f) * 6f)
                                lineTo(pPos.x + cos(rad - 2.5f) * 6f, pPos.y + sin(rad - 2.5f) * 6f)
                                close()
                            }
                            drawPath(path, Color(0xFF76FF03))
                        }
                    }

                    // Right Drawer: Portal Fast Travel List
                    Card(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1726)),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF1E2B47))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("🌀 Titik Teleportasi (${teleportPoints.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF))

                            Column(
                                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                teleportPoints.forEach { portal ->
                                    val isSel = portal.id == selectedInteractable?.id
                                    Surface(
                                        onClick = { selectedInteractable = portal },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSel) Color(0xFF16233B) else Color(0xFF131D31),
                                        border = BorderStroke(1.dp, if (isSel) Color(0xFF00E5FF) else Color(0xFF263238)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(portal.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                Text("[${portal.position.x.toInt()}, ${portal.position.z.toInt()}]", fontSize = 9.sp, color = Color(0xFF80D8FF))
                                            }
                                            Button(
                                                onClick = {
                                                    playerPos.set(portal.position.x, portal.position.y + 1f, portal.position.z)
                                                    Toast.makeText(context, "Teleportasi ke ${portal.name}!", Toast.LENGTH_SHORT).show()
                                                    onDismiss()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.height(26.dp)
                                            ) {
                                                Text("Teleport", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    val newPortal = WorldInteractable(
                                        id = "portal_${System.currentTimeMillis()}",
                                        name = "Portal #${teleportPoints.size + 1}",
                                        type = InteractableType.QUANTUM_PORTAL,
                                        position = Vec3(playerPos.x, playerPos.y, playerPos.z),
                                        targetTeleportPos = Vec3(0f, 1.5f, 0f),
                                        promptText = "Teleportasi"
                                    )
                                    interactionSystem.interactables.add(newPortal)
                                    selectedInteractable = newPortal
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2B47)),
                                modifier = Modifier.fillMaxWidth().height(32.dp)
                            ) {
                                Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(13.dp))
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
