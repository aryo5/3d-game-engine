package com.example.engine3d.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay

@Composable
fun ShaderWarmupDialog(
    onWarmupComplete: () -> Unit
) {
    var stepIndex by remember { mutableIntStateOf(0) }
    var rawProgress by remember { mutableFloatStateOf(0.1f) }

    val steps = listOf(
        "Kompilasi Vertex Shader PBR (GLSL v100 ES)...",
        "Kompilasi Fragment Shader Real-time Blinn-Phong & Dynamic Lights...",
        "Menautkan Program Shader Pipeline & Uniform Cache...",
        "Membangun Partisi Spasial 2D Kontur Lereng GLB...",
        "Menghubungkan Rig Animasi Karakter & File Aksi...",
        "Pipeline Siap! Memasuki Dunia 3D..."
    )

    LaunchedEffect(Unit) {
        delay(250)
        stepIndex = 1
        rawProgress = 0.35f
        delay(300)
        stepIndex = 2
        rawProgress = 0.60f
        delay(250)
        stepIndex = 3
        rawProgress = 0.80f
        delay(250)
        stepIndex = 4
        rawProgress = 0.95f
        delay(200)
        stepIndex = 5
        rawProgress = 1.0f
        delay(300)
        onWarmupComplete()
    }

    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = tween(durationMillis = 200),
        label = "warmup_progress"
    )

    Dialog(onDismissRequest = {}) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xF50A0E17))
                .border(1.5.dp, Color(0xFF00E5FF), RoundedCornerShape(18.dp))
                .padding(22.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "Kompilasi Shader & Pemanasan PSO",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }

                Text(
                    text = "Mempersiapkan pipeline grafis OpenGL ES dan partisi kontur spasial untuk mencegah frame drop saat bermain.",
                    fontSize = 11.sp,
                    color = Color(0xFF90A4AE),
                    lineHeight = 16.sp
                )

                Spacer(Modifier.height(4.dp))

                // Progress Indicator
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = steps[stepIndex],
                            fontSize = 11.sp,
                            color = Color(0xFF80D8FF),
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${(animatedProgress * 100).toInt()}%",
                            fontSize = 12.sp,
                            color = Color(0xFF00E5FF),
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFF00E5FF),
                        trackColor = Color(0xFF1E2842)
                    )
                }

                // Steps indicators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (i in 0 until 5) {
                        val isDone = stepIndex > i
                        val isCurrent = stepIndex == i
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isDone -> Color(0xFF00E676)
                                        isCurrent -> Color(0xFF00E5FF)
                                        else -> Color(0xFF263238)
                                    }
                                )
                        )
                    }
                }
            }
        }
    }
}
