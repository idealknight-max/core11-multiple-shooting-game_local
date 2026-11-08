package com.example.game.engine

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
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

class Raycaster3D {

    private val normalFovRad = (66.0 * PI / 180.0).toFloat()
    private val adsFovRad = (38.0 * PI / 180.0).toFloat()
    private val numRays = 110 // Balances high visual fidelity and 60fps performance on mobile
    private val zBuffer = FloatArray(numRays)

    // Pre-allocated sprite list to avoid GC allocations during render loop
    private val spriteList = ArrayList<RenderableSprite>(32)

    fun renderScene(
        drawScope: DrawScope,
        player: Player,
        otherPlayers: List<Player>,
        loots: List<LootItem>,
        pitchOffset: Float = 0f,
        gameTimeSec: Float
    ) {
        val width = drawScope.size.width
        val height = drawScope.size.height
        val halfHeight = height / 2f + pitchOffset + player.jumpOffset

        // ADS dynamic FOV
        val currentFov = if (player.isAimingDownSights) adsFovRad else normalFovRad

        // 1. Draw Ceiling and Floor (Atmospheric gradient)
        drawCeilingAndFloor(drawScope, width, height, halfHeight)

        // 2. Cast rays for walls
        val halfFov = currentFov / 2f
        val startAngle = player.angle - halfFov
        val angleStep = currentFov / numRays
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
            var side = 0
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

            // Wall color with atmospheric depth shading
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
                size = Size(columnWidth + 1.2f, wallBottom - wallTop)
            )

            // High-tech pillar seams
            if (i % 6 == 0) {
                drawScope.drawLine(
                    color = Color.Black.copy(alpha = 0.25f * depthFactor),
                    start = Offset(i * columnWidth, wallTop),
                    end = Offset(i * columnWidth, wallBottom),
                    strokeWidth = 1f
                )
            }
        }

        // 3. Render 3D Sprites (Loots + Enemies) depth sorted
        renderSprites(
            drawScope = drawScope,
            player = player,
            otherPlayers = otherPlayers,
            loots = loots,
            width = width,
            height = height,
            halfHeight = halfHeight,
            currentFov = currentFov,
            gameTimeSec = gameTimeSec
        )
    }

    private fun drawCeilingAndFloor(drawScope: DrawScope, width: Float, height: Float, halfHeight: Float) {
        // Ceiling
        drawScope.drawRect(
            color = Color(0xFF0C1017),
            topLeft = Offset(0f, 0f),
            size = Size(width, halfHeight)
        )
        // Floor
        drawScope.drawRect(
            color = Color(0xFF141A24),
            topLeft = Offset(0f, halfHeight),
            size = Size(width, height - halfHeight)
        )

        // Horizon glow line
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
        currentFov: Float,
        gameTimeSec: Float
    ) {
        spriteList.clear()

        for (other in otherPlayers) {
            if (!other.isAlive) continue
            val dist = player.toVector().distanceTo(other.toVector())
            if (dist > 0.2f && dist < 26f) {
                spriteList.add(RenderableSprite.PlayerSprite(other, dist))
            }
        }

        for (loot in loots) {
            if (!loot.isAvailable) continue
            val dist = player.toVector().distanceTo(Vector2(loot.x, loot.y))
            if (dist > 0.2f && dist < 26f) {
                spriteList.add(RenderableSprite.LootSprite(loot, dist))
            }
        }

        // Sort back to front (Painter's Algorithm)
        spriteList.sortByDescending { it.distance }

        for (sprite in spriteList) {
            val spriteX = sprite.x
            val spriteY = sprite.y

            val dx = spriteX - player.x
            val dy = spriteY - player.y

            var angleToSprite = atan2(dy, dx) - player.angle
            while (angleToSprite > PI) angleToSprite -= (2 * PI).toFloat()
            while (angleToSprite < -PI) angleToSprite += (2 * PI).toFloat()

            // Is sprite in front of camera frustum?
            if (abs(angleToSprite) < currentFov * 0.85f) {
                val screenX = (width / 2f) + (angleToSprite / (currentFov / 2f)) * (width / 2f)
                val dist = sprite.distance * cos(angleToSprite)

                if (dist > 0.2f) {
                    val spriteHeight = (height / dist).coerceIn(10f, height * 2.2f)
                    val spriteWidth = spriteHeight * 0.65f

                    val rayIdx = ((screenX / width) * numRays).toInt().coerceIn(0, numRays - 1)
                    if (dist < zBuffer[rayIdx] + 0.35f) {
                        when (sprite) {
                            is RenderableSprite.PlayerSprite -> {
                                draw3DPlayerModel(
                                    drawScope = drawScope,
                                    targetPlayer = sprite.player,
                                    screenX = screenX,
                                    centerY = halfHeight,
                                    spriteW = spriteWidth,
                                    spriteH = spriteHeight,
                                    distance = dist
                                )
                            }
                            is RenderableSprite.LootSprite -> {
                                draw3DRealLootModel(
                                    drawScope = drawScope,
                                    loot = sprite.loot,
                                    screenX = screenX,
                                    centerY = halfHeight,
                                    spriteW = spriteWidth * 0.85f,
                                    spriteH = spriteHeight * 0.85f,
                                    distance = dist,
                                    gameTimeSec = gameTimeSec
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Real 3D Tactical Operative model
     */
    private fun draw3DPlayerModel(
        drawScope: DrawScope,
        targetPlayer: Player,
        screenX: Float,
        centerY: Float,
        spriteW: Float,
        spriteH: Float,
        distance: Float
    ) {
        val top = centerY - spriteH / 2f
        val left = screenX - spriteW / 2f
        val depthAlpha = (1f - (distance / 25f)).coerceIn(0.4f, 1f)
        val teamColor = Color(targetPlayer.colorHex)

        // 1. Ground Drop Shadow
        drawScope.drawOval(
            color = Color.Black.copy(alpha = 0.5f * depthAlpha),
            topLeft = Offset(left + spriteW * 0.1f, top + spriteH * 0.88f),
            size = Size(spriteW * 0.8f, spriteH * 0.14f)
        )

        // 2. Tactical Combat Legs & Boots
        val legW = spriteW * 0.22f
        val legH = spriteH * 0.38f
        drawScope.drawRoundRect(
            color = Color(0xFF1E2830).copy(alpha = depthAlpha),
            topLeft = Offset(left + spriteW * 0.2f, top + spriteH * 0.58f),
            size = Size(legW, legH),
            cornerRadius = CornerRadius(3f, 3f)
        )
        drawScope.drawRoundRect(
            color = Color(0xFF1E2830).copy(alpha = depthAlpha),
            topLeft = Offset(left + spriteW * 0.58f, top + spriteH * 0.58f),
            size = Size(legW, legH),
            cornerRadius = CornerRadius(3f, 3f)
        )

        // Knee pads
        drawScope.drawRoundRect(
            color = Color(0xFF37474F).copy(alpha = depthAlpha),
            topLeft = Offset(left + spriteW * 0.19f, top + spriteH * 0.68f),
            size = Size(legW * 1.1f, legH * 0.28f),
            cornerRadius = CornerRadius(2f, 2f)
        )
        drawScope.drawRoundRect(
            color = Color(0xFF37474F).copy(alpha = depthAlpha),
            topLeft = Offset(left + spriteW * 0.57f, top + spriteH * 0.68f),
            size = Size(legW * 1.1f, legH * 0.28f),
            cornerRadius = CornerRadius(2f, 2f)
        )

        // 3. 3D Body Armor Vest & Shoulder Guards
        val torsoW = spriteW * 0.72f
        val torsoH = spriteH * 0.36f
        val torsoLeft = left + spriteW * 0.14f
        val torsoTop = top + spriteH * 0.24f

        drawScope.drawRoundRect(
            color = Color(0xFF263238).copy(alpha = depthAlpha),
            topLeft = Offset(torsoLeft, torsoTop),
            size = Size(torsoW, torsoH),
            cornerRadius = CornerRadius(6f, 6f)
        )

        // Kevlar Plate with Team Insignia
        drawScope.drawRoundRect(
            color = teamColor.copy(alpha = 0.85f * depthAlpha),
            topLeft = Offset(torsoLeft + torsoW * 0.15f, torsoTop + torsoH * 0.15f),
            size = Size(torsoW * 0.7f, torsoH * 0.62f),
            cornerRadius = CornerRadius(4f, 4f)
        )

        // 4. Tactical Helmet & Glowing Visor
        val headSize = spriteW * 0.44f
        val headLeft = left + (spriteW - headSize) / 2f
        val headTop = top + spriteH * 0.04f

        drawScope.drawOval(
            color = Color(0xFF1B2228).copy(alpha = depthAlpha),
            topLeft = Offset(headLeft, headTop),
            size = Size(headSize, headSize)
        )

        // Glowing Visor
        val visorW = headSize * 0.68f
        val visorH = headSize * 0.22f
        drawScope.drawRoundRect(
            color = if (targetPlayer.isFiring) Color(0xFFFF1744) else teamColor,
            topLeft = Offset(headLeft + (headSize - visorW) / 2f, headTop + headSize * 0.36f),
            size = Size(visorW, visorH),
            cornerRadius = CornerRadius(3f, 3f)
        )

        // 5. 3D Held Firearm model
        val gunW = spriteW * 0.52f
        val gunH = spriteH * 0.13f
        drawScope.drawRoundRect(
            color = Color(0xFF101418).copy(alpha = depthAlpha),
            topLeft = Offset(left + spriteW * 0.44f, torsoTop + torsoH * 0.38f),
            size = Size(gunW, gunH),
            cornerRadius = CornerRadius(2f, 2f)
        )

        // Gun barrel & flash
        if (targetPlayer.isFiring) {
            drawScope.drawCircle(
                color = Color(0xFFFFE082),
                radius = spriteW * 0.24f,
                center = Offset(left + spriteW * 0.44f + gunW, torsoTop + torsoH * 0.38f + gunH / 2f)
            )
            drawScope.drawCircle(
                color = Color.White,
                radius = spriteW * 0.12f,
                center = Offset(left + spriteW * 0.44f + gunW, torsoTop + torsoH * 0.38f + gunH / 2f)
            )
        }

        // 6. Overhead 3D Health and Armor Meters (Optimized without text measurer)
        val barW = (spriteW * 1.15f).coerceIn(36f, 130f)
        val barH = (spriteH * 0.07f).coerceIn(4f, 9f)
        val barLeft = screenX - barW / 2f
        val barTop = top - barH * 2.8f

        // Health background & fill
        drawScope.drawRoundRect(
            color = Color(0xAA000000),
            topLeft = Offset(barLeft, barTop),
            size = Size(barW, barH),
            cornerRadius = CornerRadius(2f, 2f)
        )
        val healthFraction = (targetPlayer.health.toFloat() / targetPlayer.maxHealth).coerceIn(0f, 1f)
        drawScope.drawRoundRect(
            color = Color(0xFF00E676).copy(alpha = depthAlpha),
            topLeft = Offset(barLeft, barTop),
            size = Size(barW * healthFraction, barH),
            cornerRadius = CornerRadius(2f, 2f)
        )

        // Armor gauge
        if (targetPlayer.armor > 0) {
            val armorTop = barTop + barH + 2f
            drawScope.drawRoundRect(
                color = Color(0xAA000000),
                topLeft = Offset(barLeft, armorTop),
                size = Size(barW, barH * 0.75f),
                cornerRadius = CornerRadius(2f, 2f)
            )
            val armorFraction = (targetPlayer.armor.toFloat() / targetPlayer.maxArmor).coerceIn(0f, 1f)
            drawScope.drawRoundRect(
                color = Color(0xFF00E5FF).copy(alpha = depthAlpha),
                topLeft = Offset(barLeft, armorTop),
                size = Size(barW * armorFraction, barH * 0.75f),
                cornerRadius = CornerRadius(2f, 2f)
            )
        }
    }

    /**
     * Real 3D Design Style for Loots (Armor Vest, Ammo Can, MedKit, 3D Guns)
     */
    private fun draw3DRealLootModel(
        drawScope: DrawScope,
        loot: LootItem,
        screenX: Float,
        centerY: Float,
        spriteW: Float,
        spriteH: Float,
        distance: Float,
        gameTimeSec: Float
    ) {
        val bobOffset = sin(gameTimeSec * 3f + loot.x + loot.y) * (spriteH * 0.12f)
        val top = centerY - spriteH / 2f + bobOffset
        val left = screenX - spriteW / 2f
        val depthAlpha = (1f - (distance / 25f)).coerceIn(0.4f, 1f)
        val lootColor = Color(loot.type.colorHex)

        // Ground 3D Projection Shadow & Pulse Ring
        val groundY = centerY + spriteH * 0.44f
        drawScope.drawOval(
            color = lootColor.copy(alpha = 0.35f * depthAlpha),
            topLeft = Offset(left - spriteW * 0.15f, groundY),
            size = Size(spriteW * 1.3f, spriteH * 0.22f)
        )
        drawScope.drawOval(
            color = Color.White.copy(alpha = 0.5f * depthAlpha),
            topLeft = Offset(left + spriteW * 0.1f, groundY + spriteH * 0.04f),
            size = Size(spriteW * 0.8f, spriteH * 0.12f),
            style = Stroke(width = 1.5f)
        )

        when (loot.type) {
            LootType.ARMOR_PLATE -> {
                // 3D REAL TACTICAL KEVLAR VEST
                draw3DArmorVestLoot(drawScope, left, top, spriteW, spriteH, depthAlpha)
            }
            LootType.AMMO_BOX -> {
                // 3D REAL MILITARY AMMO CAN
                draw3DAmmoCanLoot(drawScope, left, top, spriteW, spriteH, depthAlpha)
            }
            LootType.MEDKIT -> {
                // 3D REAL COMBAT TRAUMA PACK
                draw3DMedKitLoot(drawScope, left, top, spriteW, spriteH, depthAlpha)
            }
            LootType.SHOTGUN_DROP -> {
                // 3D REAL SPAS-12 SHOTGUN DROP
                draw3DShotgunLoot(drawScope, left, top, spriteW, spriteH, depthAlpha)
            }
            LootType.SNIPER_DROP -> {
                // 3D REAL AWP SNIPER RIFLE DROP
                draw3DSniperLoot(drawScope, left, top, spriteW, spriteH, depthAlpha)
            }
            LootType.PLASMA_DROP -> {
                // 3D REAL PLASMA CANNON DROP
                draw3DPlasmaLoot(drawScope, left, top, spriteW, spriteH, depthAlpha)
            }
        }
    }

    /**
     * 3D Tactical Kevlar Armor Vest
     */
    private fun draw3DArmorVestLoot(drawScope: DrawScope, left: Float, top: Float, w: Float, h: Float, alpha: Float) {
        val cx = left + w / 2f
        val cy = top + h * 0.45f
        val vestW = w * 0.75f
        val vestH = h * 0.65f

        // Tactical Kevlar Plate (Beveled polygon)
        val vestPath = Path().apply {
            moveTo(cx - vestW * 0.4f, cy - vestH * 0.45f) // left shoulder
            lineTo(cx - vestW * 0.2f, cy - vestH * 0.45f) // neck cut left
            lineTo(cx + vestW * 0.2f, cy - vestH * 0.45f) // neck cut right
            lineTo(cx + vestW * 0.4f, cy - vestH * 0.45f) // right shoulder
            lineTo(cx + vestW * 0.5f, cy - vestH * 0.1f)  // armpit right
            lineTo(cx + vestW * 0.42f, cy + vestH * 0.45f) // bottom right
            lineTo(cx - vestW * 0.42f, cy + vestH * 0.45f) // bottom left
            lineTo(cx - vestW * 0.5f, cy - vestH * 0.1f)  // armpit left
            close()
        }

        drawScope.drawPath(vestPath, color = Color(0xFF1E2830).copy(alpha = alpha), style = Fill)
        drawScope.drawPath(vestPath, color = Color(0xFF00E5FF).copy(alpha = 0.9f * alpha), style = Stroke(width = 2.5f))

        // Center Ballistic Plate (Glowing Cyan Shield Insignia)
        val plateW = vestW * 0.55f
        val plateH = vestH * 0.48f
        drawScope.drawRoundRect(
            color = Color(0xFF00E5FF).copy(alpha = 0.8f * alpha),
            topLeft = Offset(cx - plateW / 2f, cy - plateH * 0.25f),
            size = Size(plateW, plateH),
            cornerRadius = CornerRadius(4f, 4f)
        )
        // Center Shield Emblem
        drawScope.drawCircle(
            color = Color.White.copy(alpha = alpha),
            radius = plateW * 0.22f,
            center = Offset(cx, cy - plateH * 0.25f + plateH / 2f)
        )
    }

    /**
     * 3D Military Ammo Can
     */
    private fun draw3DAmmoCanLoot(drawScope: DrawScope, left: Float, top: Float, w: Float, h: Float, alpha: Float) {
        val cx = left + w / 2f
        val cy = top + h * 0.45f
        val boxW = w * 0.82f
        val boxH = h * 0.55f

        // Heavy Olive/Tactical Metal Ammo Box
        drawScope.drawRoundRect(
            color = Color(0xFF2E3A23).copy(alpha = alpha),
            topLeft = Offset(cx - boxW / 2f, cy - boxH / 2f),
            size = Size(boxW, boxH),
            cornerRadius = CornerRadius(4f, 4f)
        )
        drawScope.drawRoundRect(
            color = Color(0xFFFFB300).copy(alpha = 0.85f * alpha),
            topLeft = Offset(cx - boxW / 2f, cy - boxH / 2f),
            size = Size(boxW, boxH),
            cornerRadius = CornerRadius(4f, 4f),
            style = Stroke(width = 2f)
        )

        // Ammo Box Top Lid with Handle
        drawScope.drawRoundRect(
            color = Color(0xFF1E2616).copy(alpha = alpha),
            topLeft = Offset(cx - boxW * 0.52f, cy - boxH * 0.58f),
            size = Size(boxW * 1.04f, boxH * 0.2f),
            cornerRadius = CornerRadius(2f, 2f)
        )
        // Metal Lock Clasp
        drawScope.drawRect(
            color = Color(0xFFFFB300).copy(alpha = alpha),
            topLeft = Offset(cx - boxW * 0.12f, cy - boxH * 0.38f),
            size = Size(boxW * 0.24f, boxH * 0.25f)
        )
        // Stenciled Caliber Band (Gold ammo band)
        drawScope.drawRect(
            color = Color(0xCCFFB300),
            topLeft = Offset(cx - boxW * 0.42f, cy + boxH * 0.05f),
            size = Size(boxW * 0.84f, boxH * 0.15f)
        )
    }

    /**
     * 3D Field Combat Trauma Kit
     */
    private fun draw3DMedKitLoot(drawScope: DrawScope, left: Float, top: Float, w: Float, h: Float, alpha: Float) {
        val cx = left + w / 2f
        val cy = top + h * 0.45f
        val boxW = w * 0.72f
        val boxH = h * 0.6f

        // Crimson Medical Case
        drawScope.drawRoundRect(
            color = Color(0xFFD32F2F).copy(alpha = alpha),
            topLeft = Offset(cx - boxW / 2f, cy - boxH / 2f),
            size = Size(boxW, boxH),
            cornerRadius = CornerRadius(6f, 6f)
        )
        drawScope.drawRoundRect(
            color = Color.White.copy(alpha = 0.85f * alpha),
            topLeft = Offset(cx - boxW / 2f, cy - boxH / 2f),
            size = Size(boxW, boxH),
            cornerRadius = CornerRadius(6f, 6f),
            style = Stroke(width = 2.5f)
        )

        // White Medical Cross
        val crossThickness = boxW * 0.2f
        val crossLength = boxH * 0.65f
        // Vertical cross bar
        drawScope.drawRoundRect(
            color = Color.White.copy(alpha = alpha),
            topLeft = Offset(cx - crossThickness / 2f, cy - crossLength / 2f),
            size = Size(crossThickness, crossLength),
            cornerRadius = CornerRadius(2f, 2f)
        )
        // Horizontal cross bar
        drawScope.drawRoundRect(
            color = Color.White.copy(alpha = alpha),
            topLeft = Offset(cx - crossLength / 2f, cy - crossThickness / 2f),
            size = Size(crossLength, crossThickness),
            cornerRadius = CornerRadius(2f, 2f)
        )
    }

    /**
     * 3D SPAS-12 Shotgun Drop
     */
    private fun draw3DShotgunLoot(drawScope: DrawScope, left: Float, top: Float, w: Float, h: Float, alpha: Float) {
        val cx = left + w / 2f
        val cy = top + h * 0.45f
        val gunW = w * 1.1f
        val gunH = h * 0.32f

        // Heavy Barrel & Heat Shield
        drawScope.drawRoundRect(
            color = Color(0xFF1B1F24).copy(alpha = alpha),
            topLeft = Offset(cx - gunW / 2f, cy - gunH * 0.35f),
            size = Size(gunW, gunH * 0.4f),
            cornerRadius = CornerRadius(2f, 2f)
        )
        // Shotgun Pump Fore-end (Ribbed Amber Grip)
        drawScope.drawRoundRect(
            color = Color(0xFFFF9100).copy(alpha = alpha),
            topLeft = Offset(cx - gunW * 0.2f, cy - gunH * 0.2f),
            size = Size(gunW * 0.35f, gunH * 0.5f),
            cornerRadius = CornerRadius(3f, 3f)
        )
        // Stock & Trigger
        drawScope.drawRoundRect(
            color = Color(0xFF2A2E35).copy(alpha = alpha),
            topLeft = Offset(cx + gunW * 0.18f, cy - gunH * 0.1f),
            size = Size(gunW * 0.32f, gunH * 0.7f),
            cornerRadius = CornerRadius(3f, 3f)
        )
    }

    /**
     * 3D AWP Sniper Rifle Drop
     */
    private fun draw3DSniperLoot(drawScope: DrawScope, left: Float, top: Float, w: Float, h: Float, alpha: Float) {
        val cx = left + w / 2f
        val cy = top + h * 0.45f
        val gunW = w * 1.25f
        val gunH = h * 0.36f

        // Long Precision Barrel
        drawScope.drawRoundRect(
            color = Color(0xFF14171A).copy(alpha = alpha),
            topLeft = Offset(cx - gunW / 2f, cy),
            size = Size(gunW, gunH * 0.28f),
            cornerRadius = CornerRadius(2f, 2f)
        )
        // High-Power Optic Scope Tube
        val scopeW = gunW * 0.45f
        val scopeH = gunH * 0.32f
        drawScope.drawRoundRect(
            color = Color(0xFFFF3D00).copy(alpha = 0.9f * alpha),
            topLeft = Offset(cx - scopeW * 0.2f, cy - scopeH * 1.15f),
            size = Size(scopeW, scopeH),
            cornerRadius = CornerRadius(2f, 2f)
        )
        // Green Military Stock
        drawScope.drawRoundRect(
            color = Color(0xFF2E4035).copy(alpha = alpha),
            topLeft = Offset(cx + gunW * 0.15f, cy - gunH * 0.1f),
            size = Size(gunW * 0.35f, gunH * 0.75f),
            cornerRadius = CornerRadius(3f, 3f)
        )
    }

    /**
     * 3D Plasma Cannon Drop
     */
    private fun draw3DPlasmaLoot(drawScope: DrawScope, left: Float, top: Float, w: Float, h: Float, alpha: Float) {
        val cx = left + w / 2f
        val cy = top + h * 0.45f
        val gunW = w * 1.0f
        val gunH = h * 0.45f

        // Futuristic Heavy Chassis
        drawScope.drawRoundRect(
            color = Color(0xFF1A1A2E).copy(alpha = alpha),
            topLeft = Offset(cx - gunW / 2f, cy - gunH * 0.4f),
            size = Size(gunW, gunH * 0.8f),
            cornerRadius = CornerRadius(6f, 6f)
        )
        // Glowing Neon Plasma Core (Purple/Magenta)
        drawScope.drawRoundRect(
            color = Color(0xFFD500F9).copy(alpha = 0.9f * alpha),
            topLeft = Offset(cx - gunW * 0.3f, cy - gunH * 0.25f),
            size = Size(gunW * 0.6f, gunH * 0.5f),
            cornerRadius = CornerRadius(4f, 4f)
        )
        // Energy Core Light
        drawScope.drawCircle(
            color = Color.White.copy(alpha = alpha),
            radius = gunH * 0.22f,
            center = Offset(cx, cy)
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
