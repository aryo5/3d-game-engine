package com.example.engine3d.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine3d.renderer.EnginePerformanceStats

@Composable
fun PerformanceHud(
    stats: EnginePerformanceStats,
    presetName: String,
    modifier: Modifier = Modifier
) {
    val fpsColor = when {
        stats.fps >= 55 -> Color(0xFF00E676)
        stats.fps >= 30 -> Color(0xFFFFD600)
        else -> Color(0xFFFF3D00)
    }

    Row(
        modifier = modifier
            .background(Color(0xDD090D18), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0x3300E5FF), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "${stats.fps} FPS",
            color = fpsColor,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "${String.format("%.1f", stats.frameTimeMs)}ms",
            color = Color(0xFF94A3B8),
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "[${stats.posX.toInt()}, ${stats.posY.toInt()}, ${stats.posZ.toInt()}]",
            color = Color(0xFF00E5FF),
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "${stats.headingDeg.toInt()}°",
            color = Color(0xFFFFD600),
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
