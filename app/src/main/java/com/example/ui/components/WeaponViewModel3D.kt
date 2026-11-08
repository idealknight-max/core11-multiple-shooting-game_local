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
    isAimingDownSights: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Calculate weapon positioning based on hip-fire vs ADS
            val reloadOffsetY = if (isReloading) h * 0.22f else 0f
            val kickY = recoilKick * (h * 0.045f)
            val kickX = if (isAimingDownSights) 0f else recoilKick * (w * 0.015f)

            // When ADS is active, center the gun horizontally for true sight alignment!
            val gunBaseX = if (isAimingDownSights) {
                w * 0.5f - w * 0.08f + kickX
            } else {
                w * 0.68f + kickX
            }

            val gunBaseY = if (isAimingDownSights) {
                h * 0.58f + kickY + reloadOffsetY
            } else {
                h * 0.62f + kickY + reloadOffsetY
            }

            when (weapon) {
                WeaponType.RIFLE -> draw3DRifle(this, w, h, gunBaseX, gunBaseY, isFiring, isAimingDownSights)
                WeaponType.SHOTGUN -> draw3DShotgun(this, w, h, gunBaseX, gunBaseY, isFiring, isAimingDownSights)
                WeaponType.SNIPER -> draw3DSniper(this, w, h, gunBaseX, gunBaseY, isFiring, isAimingDownSights)
                WeaponType.PLASMA -> draw3DPlasma(this, w, h, gunBaseX, gunBaseY, isFiring, isAimingDownSights)
            }
        }
    }
}

/**
 * 3D Real M4-A1 Assault Rifle with Holo Sight & Picatinny Rail
 */
private fun draw3DRifle(
    drawScope: androidx.compose.ui.graphics.drawscope.DrawScope,
    w: Float,
    h: Float,
    bx: Float,
    by: Float,
    isFiring: Boolean,
    isAds: Boolean
) {
    // 1. Lower & Upper Receiver Body (Gunmetal Steel)
    val bodyPath = Path().apply {
        moveTo(bx, h)
        lineTo(bx - w * 0.04f, by + h * 0.16f)
        lineTo(bx + w * 0.02f, by + h * 0.04f)
        lineTo(bx + w * 0.16f, by + h * 0.02f)
        lineTo(bx + w * 0.24f, h)
        close()
    }
    drawScope.drawPath(bodyPath, color = Color(0xFF1E242B), style = Fill)
    drawScope.drawPath(bodyPath, color = Color(0xFF333E4B), style = Stroke(width = 2.5f))

    // 2. Curved STANAG Magazine (Dark polymer)
    val magPath = Path().apply {
        moveTo(bx + w * 0.04f, by + h * 0.16f)
        lineTo(bx + w * 0.01f, by + h * 0.28f)
        lineTo(bx + w * 0.06f, by + h * 0.29f)
        lineTo(bx + w * 0.08f, by + h * 0.16f)
        close()
    }
    drawScope.drawPath(magPath, color = Color(0xFF14171A), style = Fill)
    drawScope.drawPath(magPath, color = Color(0xFF262F38), style = Stroke(width = 1.5f))

    // 3. Handguard with Quad Picatinny Rails
    val barrelLeft = bx + w * 0.05f
    val barrelTop = by - h * 0.05f
    val barrelWidth = w * 0.048f
    val barrelHeight = h * 0.12f

    drawScope.drawRoundRect(
        color = Color(0xFF15191E),
        topLeft = Offset(barrelLeft, barrelTop),
        size = Size(barrelWidth, barrelHeight),
        cornerRadius = CornerRadius(4f, 4f)
    )

    // Handguard ventilation slots
    for (i in 0..3) {
        val slotY = barrelTop + h * 0.02f + i * (h * 0.022f)
        drawScope.drawRoundRect(
            color = Color(0xFF0A0C0E),
            topLeft = Offset(barrelLeft + barrelWidth * 0.2f, slotY),
            size = Size(barrelWidth * 0.6f, h * 0.008f),
            cornerRadius = CornerRadius(1f, 1f)
        )
    }

    // 4. Ejection Port & Brass Deflector (Metallic highlight)
    drawScope.drawRect(
        color = Color(0xFF455A64),
        topLeft = Offset(bx + w * 0.08f, by + h * 0.06f),
        size = Size(w * 0.04f, h * 0.025f)
    )

    // 5. Holographic Red Dot Sight (Center optic with illuminated reticle)
    val opticW = barrelWidth * 1.15f
    val opticH = h * 0.038f
    val opticTop = barrelTop - opticH * 0.8f

    // Sight housing
    drawScope.drawRoundRect(
        color = Color(0xFF212529),
        topLeft = Offset(barrelLeft - (opticW - barrelWidth) / 2f, opticTop),
        size = Size(opticW, opticH),
        cornerRadius = CornerRadius(3f, 3f)
    )
    // Cyan Glass Lens with Glowing Center Dot
    drawScope.drawRoundRect(
        color = Color(0x6600E5FF),
        topLeft = Offset(barrelLeft, opticTop + opticH * 0.15f),
        size = Size(barrelWidth, opticH * 0.7f),
        cornerRadius = CornerRadius(2f, 2f)
    )
    drawScope.drawCircle(
        color = Color(0xFFFF1744),
        radius = if (isAds) 4f else 2.5f,
        center = Offset(barrelLeft + barrelWidth / 2f, opticTop + opticH * 0.5f)
    )

    // 6. Muzzle Flash & Shell Ejection
    if (isFiring) {
        drawMuzzleFlash(drawScope, barrelLeft + barrelWidth / 2f, barrelTop - h * 0.02f, w * 0.08f, Color(0xFFFFE082))
    }
}

/**
 * 3D Real SPAS-12 Tactical Shotgun
 */
private fun draw3DShotgun(
    drawScope: androidx.compose.ui.graphics.drawscope.DrawScope,
    w: Float,
    h: Float,
    bx: Float,
    by: Float,
    isFiring: Boolean,
    isAds: Boolean
) {
    // 1. Heavy Gauge Steel Receiver
    val receiverPath = Path().apply {
        moveTo(bx - w * 0.02f, h)
        lineTo(bx - w * 0.05f, by + h * 0.18f)
        lineTo(bx + w * 0.03f, by + h * 0.05f)
        lineTo(bx + w * 0.18f, by + h * 0.03f)
        lineTo(bx + w * 0.25f, h)
        close()
    }
    drawScope.drawPath(receiverPath, color = Color(0xFF1A1E22), style = Fill)
    drawScope.drawPath(receiverPath, color = Color(0xFF37474F), style = Stroke(width = 2.5f))

    // 2. Wide 12-Gauge Barrel & Under-barrel Tube
    val barrelLeft = bx + w * 0.06f
    val barrelTop = by - h * 0.06f
    val barrelWidth = w * 0.075f
    val barrelHeight = h * 0.14f

    // Main barrel
    drawScope.drawRoundRect(
        color = Color(0xFF111417),
        topLeft = Offset(barrelLeft, barrelTop),
        size = Size(barrelWidth, barrelHeight),
        cornerRadius = CornerRadius(5f, 5f)
    )
    // Tubular magazine below barrel
    drawScope.drawRoundRect(
        color = Color(0xFF263238),
        topLeft = Offset(barrelLeft + barrelWidth * 0.1f, barrelTop + h * 0.04f),
        size = Size(barrelWidth * 0.8f, barrelHeight * 0.8f),
        cornerRadius = CornerRadius(4f, 4f)
    )

    // 3. Ribbed Pump Slide (Tactical Amber Polymer Grip)
    drawScope.drawRoundRect(
        color = Color(0xFFFF9100),
        topLeft = Offset(barrelLeft + barrelWidth * 0.05f, barrelTop + barrelHeight * 0.35f),
        size = Size(barrelWidth * 0.9f, barrelHeight * 0.45f),
        cornerRadius = CornerRadius(4f, 4f)
    )
    // Grip ribs
    for (i in 0..2) {
        val ribY = barrelTop + barrelHeight * 0.4f + i * (h * 0.016f)
        drawScope.drawLine(
            color = Color.Black.copy(alpha = 0.6f),
            start = Offset(barrelLeft + barrelWidth * 0.1f, ribY),
            end = Offset(barrelLeft + barrelWidth * 0.9f, ribY),
            strokeWidth = 2f
        )
    }

    // 4. Combat Ghost Ring Sight
    drawScope.drawCircle(
        color = Color(0xFF37474F),
        radius = barrelWidth * 0.28f,
        center = Offset(barrelLeft + barrelWidth / 2f, barrelTop - h * 0.015f),
        style = Stroke(width = 2.5f)
    )
    drawScope.drawCircle(
        color = Color(0xFFFF9100),
        radius = 2.5f,
        center = Offset(barrelLeft + barrelWidth / 2f, barrelTop - h * 0.015f)
    )

    // 5. Shotgun Muzzle Burst
    if (isFiring) {
        drawMuzzleFlash(drawScope, barrelLeft + barrelWidth / 2f, barrelTop - h * 0.02f, w * 0.11f, Color(0xFFFFB300))
    }
}

/**
 * 3D Real AWP Heavy Bolt Sniper Rifle
 */
private fun draw3DSniper(
    drawScope: androidx.compose.ui.graphics.drawscope.DrawScope,
    w: Float,
    h: Float,
    bx: Float,
    by: Float,
    isFiring: Boolean,
    isAds: Boolean
) {
    // 1. Olive Military Precision Stock & Receiver
    val stockPath = Path().apply {
        moveTo(bx - w * 0.02f, h)
        lineTo(bx - w * 0.06f, by + h * 0.18f)
        lineTo(bx + w * 0.02f, by + h * 0.06f)
        lineTo(bx + w * 0.18f, by + h * 0.03f)
        lineTo(bx + w * 0.26f, h)
        close()
    }
    drawScope.drawPath(stockPath, color = Color(0xFF2E3D32), style = Fill)
    drawScope.drawPath(stockPath, color = Color(0xFF455A64), style = Stroke(width = 2.5f))

    // 2. Bolt Action Handle (Silver metal handle)
    drawScope.drawRoundRect(
        color = Color(0xFF90A4AE),
        topLeft = Offset(bx + w * 0.11f, by + h * 0.07f),
        size = Size(w * 0.035f, h * 0.015f),
        cornerRadius = CornerRadius(2f, 2f)
    )
    drawScope.drawCircle(
        color = Color(0xFFCFD8DC),
        radius = 5f,
        center = Offset(bx + w * 0.14f, by + h * 0.078f)
    )

    // 3. Long Fluted Precision Barrel
    val barrelLeft = bx + w * 0.06f
    val barrelTop = by - h * 0.08f
    val barrelWidth = w * 0.042f
    val barrelHeight = h * 0.16f

    drawScope.drawRoundRect(
        color = Color(0xFF121518),
        topLeft = Offset(barrelLeft, barrelTop),
        size = Size(barrelWidth, barrelHeight),
        cornerRadius = CornerRadius(3f, 3f)
    )

    // Side-ported Muzzle Brake
    drawScope.drawRoundRect(
        color = Color(0xFF263238),
        topLeft = Offset(barrelLeft - barrelWidth * 0.15f, barrelTop - h * 0.02f),
        size = Size(barrelWidth * 1.3f, h * 0.024f),
        cornerRadius = CornerRadius(2f, 2f)
    )

    // 4. Large Optical Sniper Scope Tube
    val scopeW = barrelWidth * 1.5f
    val scopeH = h * 0.055f
    val scopeTop = barrelTop - scopeH * 0.7f

    // Scope main body
    drawScope.drawRoundRect(
        color = Color(0xFF1E242B),
        topLeft = Offset(barrelLeft - (scopeW - barrelWidth) / 2f, scopeTop),
        size = Size(scopeW, scopeH),
        cornerRadius = CornerRadius(4f, 4f)
    )
    // Windage Adjustment Turret
    drawScope.drawRect(
        color = Color(0xFFFF3D00),
        topLeft = Offset(barrelLeft + barrelWidth * 0.2f, scopeTop - h * 0.012f),
        size = Size(barrelWidth * 0.6f, h * 0.012f)
    )
    // Amber/Gold Scope Glass with Red Target Dot
    drawScope.drawCircle(
        color = Color(0x88FF6D00),
        radius = scopeW * 0.35f,
        center = Offset(barrelLeft + barrelWidth / 2f, scopeTop + scopeH * 0.5f)
    )
    drawScope.drawCircle(
        color = Color(0xFFFF1744),
        radius = if (isAds) 4f else 2.5f,
        center = Offset(barrelLeft + barrelWidth / 2f, scopeTop + scopeH * 0.5f)
    )

    // 5. Heavy Muzzle Flash
    if (isFiring) {
        drawMuzzleFlash(drawScope, barrelLeft + barrelWidth / 2f, barrelTop - h * 0.03f, w * 0.12f, Color(0xFFFF5722))
    }
}

/**
 * 3D Real Plasma Cannon with Sci-Fi Accelerator Coils
 */
private fun draw3DPlasma(
    drawScope: androidx.compose.ui.graphics.drawscope.DrawScope,
    w: Float,
    h: Float,
    bx: Float,
    by: Float,
    isFiring: Boolean,
    isAds: Boolean
) {
    // 1. Futuristic Titanium Frame
    val bodyPath = Path().apply {
        moveTo(bx - w * 0.02f, h)
        lineTo(bx - w * 0.06f, by + h * 0.16f)
        lineTo(bx + w * 0.02f, by + h * 0.03f)
        lineTo(bx + w * 0.19f, by + h * 0.02f)
        lineTo(bx + w * 0.26f, h)
        close()
    }
    drawScope.drawPath(bodyPath, color = Color(0xFF16192B), style = Fill)
    drawScope.drawPath(bodyPath, color = Color(0xFF4A148C), style = Stroke(width = 2.5f))

    // 2. Twin Magnetic Accelerator Rails
    val barrelLeft = bx + w * 0.055f
    val barrelTop = by - h * 0.07f
    val barrelWidth = w * 0.065f
    val barrelHeight = h * 0.15f

    drawScope.drawRoundRect(
        color = Color(0xFF0F1123),
        topLeft = Offset(barrelLeft, barrelTop),
        size = Size(barrelWidth, barrelHeight),
        cornerRadius = CornerRadius(6f, 6f)
    )

    // 3. Glowing Neon Plasma Conduit Core
    val coreW = barrelWidth * 0.65f
    val coreH = barrelHeight * 0.7f
    val coreTop = barrelTop + barrelHeight * 0.15f
    drawScope.drawRoundRect(
        color = Color(0xFFD500F9).copy(alpha = 0.9f),
        topLeft = Offset(barrelLeft + (barrelWidth - coreW) / 2f, coreTop),
        size = Size(coreW, coreH),
        cornerRadius = CornerRadius(4f, 4f)
    )
    // Pulsing Core White Light
    drawScope.drawCircle(
        color = Color.White,
        radius = coreW * 0.35f,
        center = Offset(barrelLeft + barrelWidth / 2f, coreTop + coreH / 2f)
    )

    // 4. Energy Emitter Ring Sight
    drawScope.drawCircle(
        color = Color(0xFFD500F9),
        radius = barrelWidth * 0.35f,
        center = Offset(barrelLeft + barrelWidth / 2f, barrelTop - h * 0.015f),
        style = Stroke(width = 2.5f)
    )
    drawScope.drawCircle(
        color = Color.White,
        radius = 3f,
        center = Offset(barrelLeft + barrelWidth / 2f, barrelTop - h * 0.015f)
    )

    // 5. Plasma Violet Burst
    if (isFiring) {
        drawMuzzleFlash(drawScope, barrelLeft + barrelWidth / 2f, barrelTop - h * 0.02f, w * 0.10f, Color(0xFFE040FB))
    }
}

/**
 * Procedural Dynamic Muzzle Flash
 */
private fun drawMuzzleFlash(
    drawScope: androidx.compose.ui.graphics.drawscope.DrawScope,
    muzzleX: Float,
    muzzleY: Float,
    baseRadius: Float,
    color: Color
) {
    val flashRadius = baseRadius * (0.85f + Random.nextFloat() * 0.35f)
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

    drawScope.drawPath(flashPath, color = color, style = Fill)
    drawScope.drawCircle(color = Color.White, radius = flashRadius * 0.4f, center = Offset(muzzleX, muzzleY))
}
