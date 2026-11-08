package com.example.game.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.game.model.LootItem
import com.example.game.model.LootType
import com.example.game.model.Player
import com.example.game.model.Vector2
import com.example.game.model.WeaponType
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt

class Raycaster3D {

    private val fovRad = (66.0 * PI / 180.0).toFloat()
    private val numRays = 110 // Balances performance and visual crispness on mobile
    private val zBuffer = FloatArray(numRays)

    fun renderScene(
        drawScope: DrawScope,
        player: Player,
        otherPlayers: List<Player>,
        loots: List<LootItem>,
        pitchOffset: Float = 0f, // vertical camera tilt
        textMeasurer: TextMeasurer,
        gameTimeSec: Float
    ) {
        val width = drawScope.size.width
        val height = drawScope.size.height
        val halfHeight = height / 2f + pitchOffset

        // 1. Draw Ceiling and Floor (Atmospheric gradient)
        drawCeilingAndFloor(drawScope, width, height, halfHeight)

        // 2. Cast rays for walls
        val halfFov = fovRad / 2f
        val startAngle = player.angle - halfFov
        val angleStep = fovRad / numRays
        val columnWidth = width / numRays

        for (i in 0 until numRays) {
            val rayAngle = startAngle + i * angleStep
            val rayDirX = cos(rayAngle)
            val rayDirY = sin(rayAngle)

            // DDA Raycasting
            var mapX = floor(player.x).toInt()
            var mapY = floor(player.y).toInt()

            val deltaDistX = if (abs(rayDirX) < 1e-6f) 1e30f else abs(1f / rayDirX)
            val deltaDistY = if (abs(rayDirY) < 1e-6f) 1e30f else abs(1f / rayDirY)

            var stepX: Int
            var sideDistX: Float
            if (rayDirX < 0) {
                stepX = -1
                sideDistX = (player.x - mapX) * deltaDistX
            } else {
                stepX = 1
                sideDistX = (mapX + 1.0f - player.x) * deltaDistX
            }

            var stepY: Int
            var sideDistY: Float
            if (rayDirY < 0) {
                stepY = -1
                sideDistY = (player.y - mapY) * deltaDistY
            } else {
                stepY = 1
                sideDistY = (mapY + 1.0f - player.y) * deltaDistY
            }

            var hit = false
            var side = 0 // 0 for X hit, 1 for Y hit
            var hitWallType = 1
            var distanceTraveled = 0f

            while (!hit && distanceTraveled < 28f) {
                if (sideDistX < sideDistY) {
                    sideDistX += deltaDistX
                    mapX += stepX
                    side = 0
                } else {
                    sideDistY += deltaDistY
                    mapY += stepY
                    side = 1
                }

                hitWallType = GameMap.getWallType(mapX, mapY)
                if (hitWallType > 0) {
                    hit = true
                }
                distanceTraveled = if (side == 0) sideDistX else sideDistY
            }

            val perpWallDist = if (side == 0) {
                (mapX - player.x + (1 - stepX) / 2f) / rayDirX
            } else {
                (mapY - player.y + (1 - stepY) / 2f) / rayDirY
            }

            // Fix fisheye distortion
            val correctedDist = perpWallDist * cos(rayAngle - player.angle)
            zBuffer[i] = correctedDist.coerceAtLeast(0.1f)

            // Wall slice height
            val wallHeight = (height / correctedDist).coerceIn(4f, height * 3f)
            val wallTop = halfHeight - wallHeight / 2f
            val wallBottom = halfHeight + wallHeight / 2f

            // Wall color with depth fog
            val baseColor = getWallBaseColor(hitWallType, side == 1)
            val depthFactor = (1f - (correctedDist / 26f)).coerceIn(0.15f, 1f)
            val shadedColor = Color(
                red = baseColor.red * depthFactor,
                green = baseColor.green * depthFactor,
                blue = baseColor.blue * depthFactor,
                alpha = 1f
            )

            // Draw wall column
            drawScope.drawRect(
                color = shadedColor,
                topLeft = Offset(i * columnWidth, wallTop),
                size = Size(columnWidth + 1f, wallBottom - wallTop)
            )

            // Subtle vertical pillar accent lines for high tech look
            if (i % 6 == 0) {
                drawScope.drawLine(
                    color = Color.Black.copy(alpha = 0.2f * depthFactor),
                    start = Offset(i * columnWidth, wallTop),
                    end = Offset(i * columnWidth, wallBottom),
                    strokeWidth = 1f
                )
            }
        }

        // 3. Render 3D Sprites (Loots + Other Players), depth sorted
        renderSprites(
            drawScope = drawScope,
            player = player,
            otherPlayers = otherPlayers,
            loots = loots,
            width = width,
            height = height,
            halfHeight = halfHeight,
            textMeasurer = textMeasurer,
            gameTimeSec = gameTimeSec
        )
    }

    private fun drawCeilingAndFloor(drawScope: DrawScope, width: Float, height: Float, halfHeight: Float) {
        // Ceiling (Deep space navy/slate gradient)
        drawScope.drawRect(
            color = Color(0xFF0A0E17),
            topLeft = Offset(0f, 0f),
            size = Size(width, halfHeight)
        )
        // Floor (Tactical grid dark grey)
        drawScope.drawRect(
            color = Color(0xFF131924),
            topLeft = Offset(0f, halfHeight),
            size = Size(width, height - halfHeight)
        )

        // Subtle horizon glow line
        drawScope.drawLine(
            color = Color(0x3300E5FF),
            start = Offset(0f, halfHeight),
            end = Offset(width, halfHeight),
            strokeWidth = 1.5f
        )
    }

    private fun getWallBaseColor(wallType: Int, isSide: Boolean): Color {
        val base = when (wallType) {
            1 -> Color(0xFF37474F) // Border Wall - Tactical Charcoal
            2 -> Color(0xFF455A64) // Steel Barricade - Slate Blue
            3 -> Color(0xFF0097A7) // Energy Pillar - Glowing Cyan
            4 -> Color(0xFFD84315) // Supply Crate - Amber Orange
            else -> Color(0xFF37474F)
        }
        // Darken side faces for 3D lighting depth
        return if (isSide) {
            Color(base.red * 0.72f, base.green * 0.72f, base.blue * 0.72f, 1f)
        } else {
            base
        }
    }

    private fun renderSprites(
        drawScope: DrawScope,
        player: Player,
        otherPlayers: List<Player>,
        loots: List<LootItem>,
        width: Float,
        height: Float,
        halfHeight: Float,
        textMeasurer: TextMeasurer,
        gameTimeSec: Float
    ) {
        // Collect all renderable sprites
        val sprites = mutableListOf<RenderableSprite>()

        for (other in otherPlayers) {
            if (!other.isAlive) continue
            val dist = player.toVector().distanceTo(other.toVector())
            if (dist > 0.2f && dist < 26f) {
                sprites.add(RenderableSprite.PlayerSprite(other, dist))
            }
        }

        for (loot in loots) {
            if (!loot.isAvailable) continue
            val dist = player.toVector().distanceTo(Vector2(loot.x, loot.y))
            if (dist > 0.2f && dist < 26f) {
                sprites.add(RenderableSprite.LootSprite(loot, dist))
            }
        }

        // Sort back to front (Painter's Algorithm)
        sprites.sortByDescending { it.distance }

        for (sprite in sprites) {
            val spriteX = sprite.x
            val spriteY = sprite.y

            // Transform sprite with the inverse camera matrix
            val dx = spriteX - player.x
            val dy = spriteY - player.y

            var angleToSprite = atan2(dy, dx) - player.angle
            while (angleToSprite > PI) angleToSprite -= (2 * PI).toFloat()
            while (angleToSprite < -PI) angleToSprite += (2 * PI).toFloat()

            // Is sprite in front of camera?
            if (abs(angleToSprite) < fovRad * 0.85f) {
                val screenX = (width / 2f) + (angleToSprite / (fovRad / 2f)) * (width / 2f)
                val dist = sprite.distance * cos(angleToSprite)

                if (dist > 0.2f) {
                    val spriteHeight = (height / dist).coerceIn(10f, height * 2.2f)
                    val spriteWidth = spriteHeight * 0.65f

                    // Find ray index on screen to test zBuffer occlusion
                    val rayIdx = ((screenX / width) * numRays).toInt().coerceIn(0, numRays - 1)
                    if (dist < zBuffer[rayIdx] + 0.35f) {
                        when (sprite) {
                            is RenderableSprite.PlayerSprite -> {
                                drawPlayerModel(
                                    drawScope = drawScope,
                                    targetPlayer = sprite.player,
                                    screenX = screenX,
                                    centerY = halfHeight,
                                    spriteW = spriteWidth,
                                    spriteH = spriteHeight,
                                    distance = dist,
                                    textMeasurer = textMeasurer
                                )
                            }
                            is RenderableSprite.LootSprite -> {
                                drawLootModel(
                                    drawScope = drawScope,
                                    loot = sprite.loot,
                                    screenX = screenX,
                                    centerY = halfHeight,
                                    spriteW = spriteWidth * 0.7f,
                                    spriteH = spriteHeight * 0.7f,
                                    distance = dist,
                                    gameTimeSec = gameTimeSec,
                                    textMeasurer = textMeasurer
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun drawPlayerModel(
        drawScope: DrawScope,
        targetPlayer: Player,
        screenX: Float,
        centerY: Float,
        spriteW: Float,
        spriteH: Float,
        distance: Float,
        textMeasurer: TextMeasurer
    ) {
        val top = centerY - spriteH / 2f
        val left = screenX - spriteW / 2f

        val depthAlpha = (1f - (distance / 25f)).coerceIn(0.4f, 1f)
        val teamColor = Color(targetPlayer.colorHex)

        // 1. Shadow / Base ring
        drawScope.drawOval(
            color = Color.Black.copy(alpha = 0.45f * depthAlpha),
            topLeft = Offset(left + spriteW * 0.1f, top + spriteH * 0.88f),
            size = Size(spriteW * 0.8f, spriteH * 0.14f)
        )

        // 2. Legs / Tactical Boots
        val legW = spriteW * 0.22f
        val legH = spriteH * 0.38f
        drawScope.drawRect(
            color = Color(0xFF263238).copy(alpha = depthAlpha),
            topLeft = Offset(left + spriteW * 0.2f, top + spriteH * 0.58f),
            size = Size(legW, legH)
        )
        drawScope.drawRect(
            color = Color(0xFF263238).copy(alpha = depthAlpha),
            topLeft = Offset(left + spriteW * 0.58f, top + spriteH * 0.58f),
            size = Size(legW, legH)
        )

        // 3. Tactical Torso / Armor Vest
        val torsoW = spriteW * 0.7f
        val torsoH = spriteH * 0.35f
        val torsoLeft = left + spriteW * 0.15f
        val torsoTop = top + spriteH * 0.25f

        drawScope.drawRoundRect(
            color = Color(0xFF37474F).copy(alpha = depthAlpha),
            topLeft = Offset(torsoLeft, torsoTop),
            size = Size(torsoW, torsoH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
        )

        // Armor Plate Accent
        drawScope.drawRoundRect(
            color = teamColor.copy(alpha = 0.85f * depthAlpha),
            topLeft = Offset(torsoLeft + torsoW * 0.15f, torsoTop + torsoH * 0.15f),
            size = Size(torsoW * 0.7f, torsoH * 0.6f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
        )

        // 4. Helmet and Visor
        val headSize = spriteW * 0.42f
        val headLeft = left + (spriteW - headSize) / 2f
        val headTop = top + spriteH * 0.05f

        drawScope.drawOval(
            color = Color(0xFF212121).copy(alpha = depthAlpha),
            topLeft = Offset(headLeft, headTop),
            size = Size(headSize, headSize)
        )

        // Visor (Glowing visor horizontal bar)
        val visorW = headSize * 0.65f
        val visorH = headSize * 0.22f
        drawScope.drawRoundRect(
            color = if (targetPlayer.isFiring) Color(0xFFFF1744) else teamColor,
            topLeft = Offset(headLeft + (headSize - visorW) / 2f, headTop + headSize * 0.35f),
            size = Size(visorW, visorH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
        )

        // 5. Weapon held forward
        val gunW = spriteW * 0.5f
        val gunH = spriteH * 0.12f
        drawScope.drawRect(
            color = Color(0xFF1B1B1B).copy(alpha = depthAlpha),
            topLeft = Offset(left + spriteW * 0.45f, torsoTop + torsoH * 0.4f),
            size = Size(gunW, gunH)
        )

        // Muzzle Flash if firing
        if (targetPlayer.isFiring) {
            drawScope.drawCircle(
                color = Color(0xFFFFD54F),
                radius = spriteW * 0.22f,
                center = Offset(left + spriteW * 0.45f + gunW, torsoTop + torsoH * 0.4f + gunH / 2f)
            )
        }

        // 6. Overhead 3D HUD (Player Name + Health Bar + Armor Bar)
        val barW = (spriteW * 1.1f).coerceAtLeast(40f)
        val barH = (spriteH * 0.07f).coerceIn(4f, 10f)
        val barLeft = screenX - barW / 2f
        val barTop = top - barH * 3.2f

        // Name text
        val textResult = textMeasurer.measure(
            text = "${targetPlayer.name} [${distance.toInt()}m]",
            style = TextStyle(
                color = Color.White.copy(alpha = depthAlpha),
                fontSize = (barH * 1.6f).coerceIn(9f, 14f).sp,
                fontWeight = FontWeight.Bold
            )
        )
        drawScope.drawText(
            textLayoutResult = textResult,
            topLeft = Offset(screenX - textResult.size.width / 2f, barTop - textResult.size.height - 2f)
        )

        // Health Bar (Background + Fill)
        drawScope.drawRect(
            color = Color(0x99000000),
            topLeft = Offset(barLeft, barTop),
            size = Size(barW, barH)
        )
        val healthFraction = (targetPlayer.health.toFloat() / targetPlayer.maxHealth).coerceIn(0f, 1f)
        drawScope.drawRect(
            color = Color(0xFF00E676),
            topLeft = Offset(barLeft, barTop),
            size = Size(barW * healthFraction, barH)
        )

        // Armor Bar (Below health bar)
        if (targetPlayer.armor > 0) {
            val armorTop = barTop + barH + 2f
            drawScope.drawRect(
                color = Color(0x99000000),
                topLeft = Offset(barLeft, armorTop),
                size = Size(barW, barH * 0.75f)
            )
            val armorFraction = (targetPlayer.armor.toFloat() / targetPlayer.maxArmor).coerceIn(0f, 1f)
            drawScope.drawRect(
                color = Color(0xFF00E5FF),
                topLeft = Offset(barLeft, armorTop),
                size = Size(barW * armorFraction, barH * 0.75f)
            )
        }
    }

    private fun drawLootModel(
        drawScope: DrawScope,
        loot: LootItem,
        screenX: Float,
        centerY: Float,
        spriteW: Float,
        spriteH: Float,
        distance: Float,
        gameTimeSec: Float,
        textMeasurer: TextMeasurer
    ) {
        val bobOffset = sin(gameTimeSec * 3f + loot.x + loot.y) * (spriteH * 0.15f)
        val top = centerY - spriteH / 2f + bobOffset
        val left = screenX - spriteW / 2f
        val depthAlpha = (1f - (distance / 25f)).coerceIn(0.4f, 1f)
        val lootColor = Color(loot.type.colorHex)

        // 1. Ground halo ring
        drawScope.drawOval(
            color = lootColor.copy(alpha = 0.35f * depthAlpha),
            topLeft = Offset(left - spriteW * 0.1f, centerY + spriteH * 0.4f),
            size = Size(spriteW * 1.2f, spriteH * 0.22f)
        )

        // 2. Glowing Loot Container / Diamond Box
        val boxSize = spriteW * 0.65f
        val boxCenter = Offset(screenX, top + spriteH * 0.45f)

        // Outer glow diamond
        val diamondPath = Path().apply {
            moveTo(boxCenter.x, boxCenter.y - boxSize / 2f)
            lineTo(boxCenter.x + boxSize / 2f, boxCenter.y)
            lineTo(boxCenter.x, boxCenter.y + boxSize / 2f)
            lineTo(boxCenter.x - boxSize / 2f, boxCenter.y)
            close()
        }

        drawScope.drawPath(
            path = diamondPath,
            color = lootColor.copy(alpha = 0.85f * depthAlpha),
            style = Fill
        )
        drawScope.drawPath(
            path = diamondPath,
            color = Color.White.copy(alpha = depthAlpha),
            style = Stroke(width = 2f)
        )

        // 3. Loot Title Label
        val labelResult = textMeasurer.measure(
            text = "${loot.type.iconEmoji} ${loot.type.title}",
            style = TextStyle(
                color = Color.White.copy(alpha = depthAlpha),
                fontSize = (spriteH * 0.22f).coerceIn(9f, 13f).sp,
                fontWeight = FontWeight.Bold
            )
        )
        drawScope.drawText(
            textLayoutResult = labelResult,
            topLeft = Offset(screenX - labelResult.size.width / 2f, top - labelResult.size.height - 4f)
        )
    }

    private fun Player.toVector(): Vector2 = Vector2(this.x, this.y)
}

private sealed class RenderableSprite(val x: Float, val y: Float, val distance: Float) {
    class PlayerSprite(val player: Player, distance: Float) :
        RenderableSprite(player.x, player.y, distance)

    class LootSprite(val loot: LootItem, distance: Float) :
        RenderableSprite(loot.x, loot.y, distance)
}
