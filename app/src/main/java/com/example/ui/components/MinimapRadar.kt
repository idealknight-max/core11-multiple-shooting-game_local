package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.game.engine.GameMap
import com.example.game.model.LootItem
import com.example.game.model.Player
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MinimapRadar(
    player: Player,
    otherPlayers: List<Player>,
    loots: List<LootItem>,
    modifier: Modifier = Modifier,
    size: Dp = 100.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xCC0A1118))
            .border(2.dp, Color(0x9900E5FF), CircleShape)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
            val radius = size.toPx() / 2f
            val scale = radius / 9f // 9 map units visible in radius

            // 1. Radar Grid Circles
            drawCircle(
                color = Color(0x3300E5FF),
                radius = radius * 0.5f,
                center = center,
                style = Stroke(width = 1f)
            )
            drawCircle(
                color = Color(0x3300E5FF),
                radius = radius * 0.85f,
                center = center,
                style = Stroke(width = 1f)
            )

            // Crosshairs
            drawLine(
                color = Color(0x2200E5FF),
                start = Offset(center.x, 0f),
                end = Offset(center.x, size.toPx()),
                strokeWidth = 1f
            )
            drawLine(
                color = Color(0x2200E5FF),
                start = Offset(0f, center.y),
                end = Offset(size.toPx(), center.y),
                strokeWidth = 1f
            )

            // 2. Render Nearby Walls
            val minX = (player.x - 9).toInt().coerceAtLeast(0)
            val maxX = (player.x + 9).toInt().coerceAtMost(GameMap.MAP_WIDTH - 1)
            val minY = (player.y - 9).toInt().coerceAtLeast(0)
            val maxY = (player.y + 9).toInt().coerceAtMost(GameMap.MAP_HEIGHT - 1)

            for (gy in minY..maxY) {
                for (gx in minX..maxX) {
                    if (GameMap.GRID[gy][gx] > 0) {
                        val dx = (gx + 0.5f - player.x) * scale
                        val dy = (gy + 0.5f - player.y) * scale
                        val cellScreen = center + Offset(dx, dy)

                        if ((cellScreen - center).getDistance() < radius) {
                            val cellSize = scale * 0.9f
                            drawRect(
                                color = Color(0x99455A64),
                                topLeft = Offset(cellScreen.x - cellSize / 2f, cellScreen.y - cellSize / 2f),
                                size = Size(cellSize, cellSize)
                            )
                        }
                    }
                }
            }

            // 3. Render Loots
            for (loot in loots) {
                if (!loot.isAvailable) continue
                val dx = (loot.x - player.x) * scale
                val dy = (loot.y - player.y) * scale
                val lootPos = center + Offset(dx, dy)

                if ((lootPos - center).getDistance() < radius - 4f) {
                    drawCircle(
                        color = Color(loot.type.colorHex),
                        radius = 3.5f,
                        center = lootPos
                    )
                }
            }

            // 4. Render Other Players
            for (other in otherPlayers) {
                if (!other.isAlive) continue
                val dx = (other.x - player.x) * scale
                val dy = (other.y - player.y) * scale
                val otherPos = center + Offset(dx, dy)

                if ((otherPos - center).getDistance() < radius - 4f) {
                    drawCircle(
                        color = Color(other.colorHex),
                        radius = 4.5f,
                        center = otherPos
                    )
                    // Ring ping
                    drawCircle(
                        color = Color.White,
                        radius = 2f,
                        center = otherPos
                    )
                }
            }

            // 5. Player in Center + View Frustum Cone
            val coneLength = scale * 4.5f
            val fovHalf = 0.55f // approx 33 deg
            val leftAngle = player.angle - fovHalf
            val rightAngle = player.angle + fovHalf

            val pLeft = center + Offset(cos(leftAngle) * coneLength, sin(leftAngle) * coneLength)
            val pRight = center + Offset(cos(rightAngle) * coneLength, sin(rightAngle) * coneLength)

            val visionCone = Path().apply {
                moveTo(center.x, center.y)
                lineTo(pLeft.x, pLeft.y)
                lineTo(pRight.x, pRight.y)
                close()
            }
            drawPath(
                path = visionCone,
                color = Color(0x3300E5FF)
            )

            // Local Player Dot
            drawCircle(
                color = Color(0xFF00E5FF),
                radius = 4f,
                center = center
            )
            // Local Player Direction Indicator line
            val dirLen = 8f
            drawLine(
                color = Color.White,
                start = center,
                end = center + Offset(cos(player.angle) * dirLen, sin(player.angle) * dirLen),
                strokeWidth = 2f
            )
        }
    }
}
