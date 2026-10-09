package com.example.engine3d.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
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
    var isExpanded by remember { mutableStateOf(false) }

    val fpsColor = when {
        stats.fps >= 55 -> Color(0xFF00E676)
        stats.fps >= 30 -> Color(0xFFFFD600)
        else -> Color(0xFFFF3D00)
    }

    Row(
        modifier = modifier
            .testTag("performance_hud_pill")
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xD00A101D))
            .border(1.dp, Color(0x6600E5FF), RoundedCornerShape(12.dp))
            .clickable { isExpanded = !isExpanded }
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .animateContentSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Status indicator dot
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(fpsColor, CircleShape)
        )

        // FPS
        Text(
            text = "${stats.fps} FPS",
            color = fpsColor,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )

        // Frame time ms
        Text(
            text = "${String.format("%.1f", stats.frameTimeMs)}ms",
            color = Color(0xFF94A3B8),
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
        )

        if (isExpanded) {
            // Preset tag
            Text(
                text = "[$presetName]",
                color = Color(0xFFFFD600),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
            // Coordinates
            Text(
                text = "XYZ: [${stats.posX.toInt()}, ${stats.posY.toInt()}, ${stats.posZ.toInt()}]",
                color = Color(0xFF00E5FF),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
            // Direction
            Text(
                text = "${stats.headingDeg.toInt()}°",
                color = Color(0xFF76FF03),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
            // Collapse arrow
            Text(
                text = "▴",
                color = Color(0xFF94A3B8),
                fontSize = 9.sp
            )
        } else {
            // Expand hint indicator
            Text(
                text = "▾",
                color = Color(0xFF64748B),
                fontSize = 9.sp
            )
        }
    }
}
