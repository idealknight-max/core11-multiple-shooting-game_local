package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.game.model.WeaponType
import kotlin.random.Random

@Composable
fun WeaponViewModel3D(
    weapon: WeaponType,
    isFiring: Boolean,
    recoilKick: Float,
    isReloading: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Calculate gun position (anchored to bottom right)
            val reloadOffset = if (isReloading) h * 0.18f else 0f
            val kickX = recoilKick * (w * 0.02f)
            val kickY = recoilKick * (h * 0.05f)

            val gunBaseX = w * 0.68f + kickX
            val gunBaseY = h * 0.60f + kickY + reloadOffset
            val weaponColor = Color(weapon.colorHex)

            // 1. Gun Stock & Body
            val bodyPath = Path().apply {
                moveTo(gunBaseX, h)
                lineTo(gunBaseX - w * 0.05f, gunBaseY + h * 0.18f)
                lineTo(gunBaseX + w * 0.04f, gunBaseY + h * 0.04f)
                lineTo(gunBaseX + w * 0.18f, gunBaseY + h * 0.02f)
                lineTo(gunBaseX + w * 0.26f, h)
                close()
            }
            drawPath(
                path = bodyPath,
                color = Color(0xFF1E272E),
                style = Fill
            )
            drawPath(
                path = bodyPath,
                color = Color(0xFF37474F),
                style = Stroke(width = 2.5f)
            )

            // 2. Gun Barrel
            val barrelLeft = gunBaseX + w * 0.06f
            val barrelTop = gunBaseY - h * 0.05f
            val barrelWidth = when (weapon) {
                WeaponType.RIFLE -> w * 0.045f
                WeaponType.SHOTGUN -> w * 0.075f
                WeaponType.PLASMA -> w * 0.06f
            }
            val barrelHeight = h * 0.12f

            drawRoundRect(
                color = Color(0xFF12181F),
                topLeft = Offset(barrelLeft, barrelTop),
                size = Size(barrelWidth, barrelHeight),
                cornerRadius = CornerRadius(4f, 4f)
            )

            // 3. Neon Sci-Fi Weapon Accents
            drawRoundRect(
                color = weaponColor.copy(alpha = 0.9f),
                topLeft = Offset(barrelLeft + barrelWidth * 0.2f, barrelTop + barrelHeight * 0.2f),
                size = Size(barrelWidth * 0.6f, barrelHeight * 0.5f),
                cornerRadius = CornerRadius(2f, 2f)
            )

            // Sight Rail / Holo Reticle Rail
            drawRect(
                color = Color(0xFF263238),
                topLeft = Offset(barrelLeft + barrelWidth * 0.1f, barrelTop - h * 0.02f),
                size = Size(barrelWidth * 0.8f, h * 0.02f)
            )
            drawCircle(
                color = weaponColor,
                radius = 3f,
                center = Offset(barrelLeft + barrelWidth / 2f, barrelTop - h * 0.015f)
            )

            // 4. Muzzle Flash Animation
            if (isFiring) {
                val muzzleX = barrelLeft + barrelWidth / 2f
                val muzzleY = barrelTop - h * 0.02f

                // Outer flash spikes
                val flashRadius = (w * 0.07f) * (0.8f + Random.nextFloat() * 0.4f)
                val flashPath = Path().apply {
                    val points = 8
                    for (i in 0 until points * 2) {
                        val angle = (i * Math.PI / points).toFloat()
                        val r = if (i % 2 == 0) flashRadius else flashRadius * 0.35f
                        val px = muzzleX + kotlin.math.cos(angle) * r
                        val py = muzzleY + kotlin.math.sin(angle) * r
                        if (i == 0) moveTo(px, py) else lineTo(px, py)
                    }
                    close()
                }

                drawPath(
                    path = flashPath,
                    color = when (weapon) {
                        WeaponType.PLASMA -> Color(0xFFE040FB)
                        WeaponType.SHOTGUN -> Color(0xFFFFB300)
                        WeaponType.RIFLE -> Color(0xFFFFE082)
                    }
                )

                // Flash Core Bright Light
                drawCircle(
                    color = Color.White,
                    radius = flashRadius * 0.35f,
                    center = Offset(muzzleX, muzzleY)
                )
            }
        }
    }
}
