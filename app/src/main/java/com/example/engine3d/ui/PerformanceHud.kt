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

    val slopeColor = if (stats.slopeAngle > 45f) Color(0xFFFF1744) else Color(0xFF00E5FF)

    Row(
        modifier = modifier
            .background(Color(0xCC0D131F), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0x3300E5FF), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Text(
            text = "${stats.fps} FPS",
            color = fpsColor,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "${String.format("%.1f", stats.frameTimeMs)}ms",
            color = Color(0xFFB0BEC5),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "Pos:[${String.format("%.1f", stats.posX)}, ${String.format("%.1f", stats.posY)}, ${String.format("%.1f", stats.posZ)}]",
            color = Color(0xFF00E5FF),
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "Arah:${String.format("%.0f", (stats.headingDeg % 360f + 360f) % 360f)}°",
            color = Color(0xFFFFD600),
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "Lereng:${String.format("%.0f", stats.slopeAngle)}°",
            color = slopeColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
