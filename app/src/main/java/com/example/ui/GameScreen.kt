package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material.icons.filled.North
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.South
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.GameViewModel
import com.example.game.model.Player
import com.example.game.model.WeaponType
import com.example.ui.components.MinimapRadar
import com.example.ui.components.VirtualJoystick
import com.example.ui.components.WeaponViewModel3D
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.DamageOverlayRed
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TacticalBorder
import com.example.ui.theme.TacticalDark
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.ToxicGreen

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val localPlayer by viewModel.localPlayer.collectAsState()
    val remotePlayer by viewModel.remotePlayer.collectAsState()
    val bots by viewModel.bots.collectAsState()
    val loots by viewModel.loots.collectAsState()
    val killFeed by viewModel.killFeed.collectAsState()
    val lootAlert by viewModel.lootAlert.collectAsState()
    val hitmarkerActive by viewModel.hitmarkerActive.collectAsState()
    val isMuzzleFlashing by viewModel.isMuzzleFlashing.collectAsState()
    val gunKick by viewModel.gunKick.collectAsState()
    val respawnSeconds by viewModel.respawnSeconds.collectAsState()
    val isConnected by viewModel.networkManager.isConnected.collectAsState()
    val pingMs by viewModel.networkManager.pingMs.collectAsState()
    val isMultiplayer by viewModel.isMultiplayer.collectAsState()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Collect enemies list without per-frame recomposition
        val otherEnemies = ArrayList<Player>(4).apply {
            remotePlayer?.let { add(it) }
            addAll(bots)
        }

        // 1. FAST 3D RAYCASTING CANVAS (Using system time inside draw loop for 60fps smoothness)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gameTimeSec = (System.currentTimeMillis() % 1000000L) / 1000f
            viewModel.raycaster.renderScene(
                drawScope = this,
                player = localPlayer,
                otherPlayers = otherEnemies,
                loots = loots,
                gameTimeSec = gameTimeSec
            )
        }

        // 2. FIRST-PERSON REAL 3D WEAPON MODEL
        WeaponViewModel3D(
            weapon = localPlayer.weapon,
            isFiring = isMuzzleFlashing,
            recoilKick = gunKick,
            isReloading = localPlayer.isReloading,
            isAimingDownSights = localPlayer.isAimingDownSights,
            modifier = Modifier.fillMaxSize()
        )

        // 3. FULL RIGHT-SCREEN CAMERA AIM TOUCH SURFACE
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            // Look drag applies on the right 65% of screen
                            if (change.position.x > size.width * 0.35f) {
                                change.consume()
                                viewModel.addLookDelta(dragAmount.x)
                            }
                        }
                    )
                }
        )

        // 4. LOW-HEALTH VIGNETTE
        if (localPlayer.health < 30) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DamageOverlayRed.copy(alpha = 0.28f))
            )
        }

        // 5. SNIPER SCOPE OVERLAY (When Aiming Down Sights with AWP Sniper)
        if (localPlayer.isAimingDownSights && localPlayer.weapon == WeaponType.SNIPER) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val scopeRadius = size.height * 0.44f

                // Outer black scope vignette
                drawCircle(
                    color = Color(0xEE0A0C0E),
                    radius = size.width,
                    center = Offset(cx, cy),
                    style = Stroke(width = size.width - scopeRadius * 2f)
                )

                // Scope crosshair lines
                drawLine(Color(0xDDFF1744), Offset(cx - scopeRadius, cy), Offset(cx + scopeRadius, cy), 1.5f)
                drawLine(Color(0xDDFF1744), Offset(cx, cy - scopeRadius), Offset(cx, cy + scopeRadius), 1.5f)

                // Mil-dot distance hashes
                for (d in listOf(-60f, -30f, 30f, 60f)) {
                    drawLine(Color(0xDDFF1744), Offset(cx + d, cy - 6f), Offset(cx + d, cy + 6f), 1.5f)
                    drawLine(Color(0xDDFF1744), Offset(cx - 6f, cy + d), Offset(cx + 6f, cy + d), 1.5f)
                }

                // Scope ring border
                drawCircle(
                    color = Color(0xFF263238),
                    radius = scopeRadius,
                    center = Offset(cx, cy),
                    style = Stroke(width = 4f)
                )
            }
        }

        // 6. TACTICAL RETICLE & HITMARKER (Screen Center)
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(36.dp)) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val reticleColor = if (hitmarkerActive) CrimsonRed else Color(0xCC00E5FF)
                val reticleGap = if (hitmarkerActive) 8f else if (localPlayer.isAimingDownSights) 3f else 6f
                val reticleLen = if (hitmarkerActive) 10f else 6f

                // Crosshair ticks
                drawLine(reticleColor, Offset(cx - reticleGap - reticleLen, cy), Offset(cx - reticleGap, cy), 2f)
                drawLine(reticleColor, Offset(cx + reticleGap, cy), Offset(cx + reticleGap + reticleLen, cy), 2f)
                drawLine(reticleColor, Offset(cx, cy - reticleGap - reticleLen), Offset(cx, cy - reticleGap), 2f)
                drawLine(reticleColor, Offset(cx, cy + reticleGap), Offset(cx, cy + reticleGap + reticleLen), 2f)

                // Center precision dot
                drawCircle(reticleColor, radius = 2f, center = Offset(cx, cy))

                // Hitmarker diagonal X when shot lands
                if (hitmarkerActive) {
                    val hitLen = 9f
                    drawLine(CrimsonRed, Offset(cx - hitLen, cy - hitLen), Offset(cx - 3f, cy - 3f), 2.5f)
                    drawLine(CrimsonRed, Offset(cx + 3f, cy + 3f), Offset(cx + hitLen, cy + hitLen), 2.5f)
                    drawLine(CrimsonRed, Offset(cx - hitLen, cy + hitLen), Offset(cx - 3f, cy + 3f), 2.5f)
                    drawLine(CrimsonRed, Offset(cx + 3f, cy - 3f), Offset(cx + hitLen, cy - hitLen), 2.5f)
                }
            }
        }

        // 7. TOP BAR HUD (Exit, Kills, Network Indicator & Minimap Radar)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Left: Exit & Killfeed
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.exitToLobby() },
                        modifier = Modifier
                            .size(36.dp)
                            .background(TacticalSurface.copy(alpha = 0.85f), CircleShape)
                            .border(1.dp, TacticalBorder, CircleShape)
                            .testTag("exit_game_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Exit to Lobby", tint = Color.White, modifier = Modifier.size(20.dp))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .background(TacticalSurface.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                            .border(1.dp, TacticalBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "KILLS: ${localPlayer.kills} / 10",
                            color = NeonCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    killFeed.take(3).forEach { entry ->
                        Box(
                            modifier = Modifier
                                .background(Color(0x99000000), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${entry.killerName} ⚔️ ${entry.victimName} (${entry.weaponName})",
                                color = Color.White,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Center: Connection Status Pill
            if (isMultiplayer) {
                Box(
                    modifier = Modifier
                        .background(TacticalDark.copy(alpha = 0.85f), RoundedCornerShape(16.dp))
                        .border(1.dp, if (isConnected) ToxicGreen else CrimsonRed, RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = null,
                            tint = if (isConnected) ToxicGreen else CrimsonRed,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isConnected) "Wi-Fi Connected (${pingMs}ms)" else "Waiting for Friend...",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .background(TacticalDark.copy(alpha = 0.85f), RoundedCornerShape(16.dp))
                        .border(1.dp, AmberGold, RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "Practice Bot Mode",
                        color = AmberGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Right: Minimap Radar
            MinimapRadar(
                player = localPlayer,
                otherPlayers = otherEnemies,
                loots = loots,
                size = 85.dp
            )
        }

        // 8. LOOT PICKUP ALERT BANNER
        AnimatedVisibility(
            visible = lootAlert != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -40 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -40 }),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 65.dp)
        ) {
            lootAlert?.let { alertText ->
                Box(
                    modifier = Modifier
                        .background(Color(0xDD0D1B2A), RoundedCornerShape(20.dp))
                        .border(1.5.dp, AmberGold, RoundedCornerShape(20.dp))
                        .padding(horizontal = 18.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = alertText,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 9. BOTTOM COMBAT CONTROLS (REAL MOBILE SHOOTER ICON CONTROLS)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // LEFT SIDE: Health/Armor Gauges + Movement Joystick + Crouch
                Column {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MeterBar(
                            label = "${localPlayer.health} HP",
                            current = localPlayer.health,
                            max = localPlayer.maxHealth,
                            fillColor = if (localPlayer.health > 40) ToxicGreen else CrimsonRed,
                            icon = Icons.Default.Favorite
                        )

                        MeterBar(
                            label = "${localPlayer.armor} AP",
                            current = localPlayer.armor,
                            max = localPlayer.maxArmor,
                            fillColor = NeonCyan,
                            icon = Icons.Default.Security
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.Bottom) {
                        VirtualJoystick(
                            size = 125.dp,
                            onMove = { x, y -> viewModel.setJoystickMovement(x, y) },
                            modifier = Modifier.testTag("movement_joystick")
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Crouch Action Icon Button
                        RoundCombatIconButton(
                            icon = Icons.Default.South,
                            label = "CROUCH",
                            isActive = localPlayer.isCrouching,
                            activeColor = AmberGold,
                            size = 46.dp,
                            onClick = { viewModel.toggleCrouch() },
                            modifier = Modifier.testTag("crouch_button")
                        )
                    }
                }

                // CENTER-RIGHT: ACTIVE SINGLE WEAPON DISPLAY CARD
                EquippedWeaponCard(
                    weapon = localPlayer.weapon,
                    ammoInClip = localPlayer.ammoInClip,
                    reserveAmmo = localPlayer.reserveAmmo,
                    isReloading = localPlayer.isReloading,
                    onReloadClick = { viewModel.reloadWeapon() },
                    modifier = Modifier.align(Alignment.Bottom)
                )

                // RIGHT SIDE: REAL SHOOTING CONTROLS (FIRE, SCOPE/ADS, RELOAD, JUMP)
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Top Row: Scope/ADS & Jump Icon Buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Scope / ADS Icon Button
                        RoundCombatIconButton(
                            icon = Icons.Default.FilterCenterFocus,
                            label = if (localPlayer.isAimingDownSights) "AIMED" else "SCOPE",
                            isActive = localPlayer.isAimingDownSights,
                            activeColor = NeonCyan,
                            size = 54.dp,
                            onClick = { viewModel.toggleAimDownSights() },
                            modifier = Modifier.testTag("ads_button")
                        )

                        // Jump Icon Button
                        RoundCombatIconButton(
                            icon = Icons.Default.North,
                            label = "JUMP",
                            isActive = localPlayer.jumpOffset != 0f,
                            activeColor = NeonCyan,
                            size = 54.dp,
                            onClick = { viewModel.jump() },
                            modifier = Modifier.testTag("jump_button")
                        )
                    }

                    // Bottom Row: Reload Icon Button & LARGE PRIMARY FIRE BUTTON
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Reload Action Icon Button
                        RoundCombatIconButton(
                            icon = Icons.Default.Cached,
                            label = if (localPlayer.isReloading) "LOADING" else "RELOAD",
                            isActive = localPlayer.isReloading,
                            activeColor = AmberGold,
                            size = 52.dp,
                            onClick = { viewModel.reloadWeapon() },
                            modifier = Modifier.testTag("reload_button")
                        )

                        // LARGE REAL PRIMARY FIRE ICON BUTTON (PUBG/COD Mobile Style)
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(CrimsonRed.copy(alpha = 0.88f))
                                .border(3.dp, Color.White.copy(alpha = 0.9f), CircleShape)
                                .clickable { viewModel.fireWeapon() }
                                .testTag("fire_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Adjust,
                                    contentDescription = "Fire Weapon",
                                    tint = Color.White,
                                    modifier = Modifier.size(34.dp)
                                )
                                Text(
                                    text = "FIRE",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }
        }

        // 10. RESPAWN COUNTDOWN OVERLAY
        if (respawnSeconds > 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xDD000000)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "ELIMINATED",
                        color = CrimsonRed,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Respawning in $respawnSeconds...",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Real Mobile Shooting Game Icon Button (Circular tactical button)
 */
@Composable
private fun RoundCombatIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    activeColor: Color,
    size: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (isActive) activeColor.copy(alpha = 0.35f) else TacticalSurface.copy(alpha = 0.88f))
            .border(2.dp, if (isActive) activeColor else TacticalBorder, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) activeColor else Color.White,
                modifier = Modifier.size(size * 0.44f)
            )
            Text(
                text = label,
                color = if (isActive) activeColor else Color.LightGray,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Single Equipped Gun Display Card
 */
@Composable
private fun EquippedWeaponCard(
    weapon: WeaponType,
    ammoInClip: Int,
    reserveAmmo: Int,
    isReloading: Boolean,
    onReloadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val weaponColor = Color(weapon.colorHex)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(TacticalDark.copy(alpha = 0.92f))
            .border(1.5.dp, weaponColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onReloadClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column {
                Text(
                    text = weapon.displayName.uppercase(),
                    color = weaponColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = weapon.description,
                    color = Color.Gray,
                    fontSize = 9.sp
                )
                Text(
                    text = "Loot drops to swap",
                    color = AmberGold.copy(alpha = 0.8f),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Box(
                modifier = Modifier
                    .background(TacticalSurface, RoundedCornerShape(8.dp))
                    .border(1.dp, TacticalBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (isReloading) "RELOAD..." else "$ammoInClip / $reserveAmmo",
                    color = if (ammoInClip <= 3) CrimsonRed else Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
private fun MeterBar(
    label: String,
    current: Int,
    max: Int,
    fillColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    val fraction = (current.toFloat() / max).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .width(110.dp)
            .background(TacticalSurface.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
            .border(1.dp, TacticalBorder, RoundedCornerShape(8.dp))
            .padding(6.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, tint = fillColor, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0x55000000))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(fillColor)
                )
            }
        }
    }
}
