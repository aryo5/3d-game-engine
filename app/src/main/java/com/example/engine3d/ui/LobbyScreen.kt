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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine3d.project.ProjectManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LobbyScreen(
    onEnterWorld: () -> Unit,
    onOpenHudEditor: () -> Unit,
    onOpenAssetManager: () -> Unit,
    onOpenCentralStudio: () -> Unit,
    onOpenGraphicsSettings: () -> Unit,
    onOpenLuaStudio: () -> Unit
) {
    val scrollState = rememberScrollState()
    val activeProjectName = ProjectManager.activeProject?.name ?: "Proyek_Harvest_3D"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117)) // Tema Gelap Godot/GitHub Engine
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            // ==========================================
            // 1. HEADER ATAS (RAPI & ANTI-TUMPANG TINDIH)
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4CAF50))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "APEX3D ENGINE",
                            color = Color(0xFF58A6FF),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                    }
                    Text(
                        text = "Mobile 3D Runtime & Lua Studio Hub",
                        color = Color(0xFF8B949E),
                        fontSize = 12.sp
                    )
                }

                // Tombol LUA STUDIO di pojok kanan atas yang mandiri
                FilledTonalButton(
                    onClick = onOpenLuaStudio,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFF21262D),
                        contentColor = Color(0xFFFFD54F)
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Lua Studio",
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFFFFD54F)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LUA STUDIO",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // ==========================================
            // 2. KARTU PROYEK AKTIF (GAYA GODOT PROJECT)
            // ==========================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1F6FEB)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Proyek Aktif",
                            color = Color(0xFF8B949E),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = activeProjectName,
                            color = Color(0xFFF0F6FC),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF238636).copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF238636))
                    ) {
                        Text(
                            text = "LUA 60 FPS",
                            color = Color(0xFF3FB950),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // ==========================================
            // 3. HERO ACTION: PLAY / MASUK DUNIA 3D
            // ==========================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF1F6FEB), Color(0xFF238636))
                        )
                    )
                    .clickable { onEnterWorld() }
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Mulai Masuk Dunia 3D",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Eksekusi scene graph, shader GLSL, & sistem kontrol fisik",
                            color = Color(0xFFE6EDF3),
                            fontSize = 12.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Mainkan",
                            tint = Color(0xFF1F6FEB),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            // ==========================================
            // 4. RINGKASAN STATUS ASET & ENTITY
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatusBadge(
                    title = "TERRAIN",
                    value = "HIGHLAND",
                    color = Color(0xFF58A6FF),
                    modifier = Modifier.weight(1f)
                )
                StatusBadge(
                    title = "MODEL GLB",
                    value = "FARMER 3D",
                    color = Color(0xFF3FB950),
                    modifier = Modifier.weight(1f)
                )
                StatusBadge(
                    title = "ENTITAS",
                    value = "2 NPC • BATAS",
                    color = Color(0xFFD29922),
                    modifier = Modifier.weight(1f)
                )
            }

            // ==========================================
            // 5. PUSAT KONTROL & STUDIO ALAT ENGINE
            // ==========================================
            Text(
                text = "Pusat Kontrol & Studio Engine",
                color = Color(0xFFF0F6FC),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Grid Kartu Alat Studio
            StudioMenuCard(
                icon = Icons.Default.Terminal,
                title = "Studio Script & Plugin Lua",
                subtitle = "Tulis kode perilaku game, bind event sinyal, dan muat plugin modular.",
                iconBg = Color(0xFF8957E5),
                onClick = onOpenLuaStudio
            )
            Spacer(modifier = Modifier.height(10.dp))

            StudioMenuCard(
                icon = Icons.Default.Inventory2,
                title = "Manajer Model GLB & Aset",
                subtitle = "Impor file .GLB/.OBJ dari penyimpanan HP ke folder proyek aktif.",
                iconBg = Color(0xFF1F6FEB),
                onClick = onOpenAssetManager
            )
            Spacer(modifier = Modifier.height(10.dp))

            StudioMenuCard(
                icon = Icons.Default.Gamepad,
                title = "Studio Kustomisasi Kontrol Layar",
                subtitle = "Konfigurasi ukuran, posisi, dan tata letak virtual joystick & tombol.",
                iconBg = Color(0xFF238636),
                onClick = onOpenHudEditor
            )
            Spacer(modifier = Modifier.height(10.dp))

            StudioMenuCard(
                icon = Icons.Default.Tune,
                title = "Hub Konfigurasi & Grafis Gerak",
                subtitle = "Konfigurasi trigger zone, fisika rintangan peta, dialog, dan cuaca.",
                iconBg = Color(0xFFD29922),
                onClick = onOpenCentralStudio
            )
            Spacer(modifier = Modifier.height(10.dp))

            StudioMenuCard(
                icon = Icons.Default.Settings,
                title = "Pengaturan Grafis & Engine",
                subtitle = "Sesuaikan resolusi render, shadow, filter tekstur, dan target FPS.",
                iconBg = Color(0xFF6E7681),
                onClick = onOpenGraphicsSettings
            )
        }
    }
}

@Composable
private fun StatusBadge(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                color = Color(0xFF8B949E),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = color,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StudioMenuCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconBg: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconBg,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color(0xFFF0F6FC),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = Color(0xFF8B949E),
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF484F58),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
