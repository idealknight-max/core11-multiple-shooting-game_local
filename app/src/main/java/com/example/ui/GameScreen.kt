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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
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
import com.example.ui.theme.PlasmaPurple
import com.example.ui.theme.TacticalBorder
import com.example.ui.theme.TacticalDark
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.ToxicGreen
import kotlinx.coroutines.delay

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

    val textMeasurer = rememberTextMeasurer()
    var gameTimeSec by remember { mutableFloatStateOf(0f) }

    // Frame timer
    LaunchedEffect(Unit) {
        while (true) {
            delay(16)
            gameTimeSec += 0.016f
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. 3D Raycasting Canvas
        val otherEnemies = mutableListOf<Player>()
        remotePlayer?.let { otherEnemies.add(it) }
        otherEnemies.addAll(bots)

        Canvas(modifier = Modifier.fillMaxSize()) {
            viewModel.raycaster.renderScene(
                drawScope = this,
                player = localPlayer,
                otherPlayers = otherEnemies,
                loots = loots,
                textMeasurer = textMeasurer,
                gameTimeSec = gameTimeSec
            )
        }

        // 2. First-Person 3D Weapon Model
        WeaponViewModel3D(
            weapon = localPlayer.weapon,
            isFiring = isMuzzleFlashing,
            recoilKick = gunKick,
            isReloading = localPlayer.isReloading,
            modifier = Modifier.fillMaxSize()
        )

        // 3. Screen Touch Drag Overlay for Camera Look (Right half of the screen)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            // If touch starts on the right side of screen, use it to look around
                            if (change.position.x > size.width * 0.35f) {
                                change.consume()
                                viewModel.addLookDelta(dragAmount.x)
                            }
                        }
                    )
                }
        )

        // 4. Red Damage Flash Vignette when low health or hit
        if (localPlayer.health < 30) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DamageOverlayRed.copy(alpha = 0.25f))
            )
        }

        // 5. Crosshair and Hitmarker (Screen Center)
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(36.dp)) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val reticleColor = if (hitmarkerActive) CrimsonRed else Color(0xCC00E5FF)
                val reticleGap = if (hitmarkerActive) 8f else 5f
                val reticleLen = if (hitmarkerActive) 10f else 7f

                // Crosshair ticks
                drawLine(reticleColor, Offset(cx - reticleGap - reticleLen, cy), Offset(cx - reticleGap, cy), 2f)
                drawLine(reticleColor, Offset(cx + reticleGap, cy), Offset(cx + reticleGap + reticleLen, cy), 2f)
                drawLine(reticleColor, Offset(cx, cy - reticleGap - reticleLen), Offset(cx, cy - reticleGap), 2f)
                drawLine(reticleColor, Offset(cx, cy + reticleGap), Offset(cx, cy + reticleGap + reticleLen), 2f)

                // Center dot
                drawCircle(reticleColor, radius = 2f, center = Offset(cx, cy))

                // Hitmarker diagonal X when hitting target
                if (hitmarkerActive) {
                    val hitLen = 9f
                    drawLine(CrimsonRed, Offset(cx - hitLen, cy - hitLen), Offset(cx - 3f, cy - 3f), 2.5f)
                    drawLine(CrimsonRed, Offset(cx + 3f, cy + 3f), Offset(cx + hitLen, cy + hitLen), 2.5f)
                    drawLine(CrimsonRed, Offset(cx - hitLen, cy + hitLen), Offset(cx - 3f, cy + 3f), 2.5f)
                    drawLine(CrimsonRed, Offset(cx + 3f, cy - 3f), Offset(cx + hitLen, cy - hitLen), 2.5f)
                }
            }
        }

        // 6. TOP BAR HUD (Exit button, Killfeed, Room Status & Radar)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Left: Exit Button & Killfeed
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

                    // Player Score
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

                // Killfeed notifications
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
                size = 90.dp
            )
        }

        // 7. LOOT PICKUP BANNER (Floating Center Alert)
        AnimatedVisibility(
            visible = lootAlert != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -40 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -40 }),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 70.dp)
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
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 8. BOTTOM CONTROLS & HUD
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // LEFT SIDE: Movement Virtual Joystick + Health/Armor Bars
                Column {
                    // Health & Armor Meters
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Health Bar (Red/Green)
                        MeterBar(
                            label = "${localPlayer.health} HP",
                            current = localPlayer.health,
                            max = localPlayer.maxHealth,
                            fillColor = if (localPlayer.health > 40) ToxicGreen else CrimsonRed,
                            icon = Icons.Default.Favorite
                        )

                        // Armor Bar (Cyan)
                        MeterBar(
                            label = "${localPlayer.armor} AP",
                            current = localPlayer.armor,
                            max = localPlayer.maxArmor,
                            fillColor = NeonCyan,
                            icon = Icons.Default.Security
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Virtual Movement Joystick
                    VirtualJoystick(
                        size = 135.dp,
                        onMove = { x, y -> viewModel.setJoystickMovement(x, y) },
                        modifier = Modifier.testTag("movement_joystick")
                    )
                }

                // RIGHT SIDE: Weapon Selector, Ammo, Reload, and FIRE Button
                Column(horizontalAlignment = Alignment.End) {
                    // Quick Weapon Selector Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        WeaponPill(
                            weapon = WeaponType.RIFLE,
                            isSelected = localPlayer.weapon == WeaponType.RIFLE,
                            onClick = { viewModel.switchWeapon(WeaponType.RIFLE) }
                        )
                        WeaponPill(
                            weapon = WeaponType.SHOTGUN,
                            isSelected = localPlayer.weapon == WeaponType.SHOTGUN,
                            onClick = { viewModel.switchWeapon(WeaponType.SHOTGUN) }
                        )
                        WeaponPill(
                            weapon = WeaponType.PLASMA,
                            isSelected = localPlayer.weapon == WeaponType.PLASMA,
                            onClick = { viewModel.switchWeapon(WeaponType.PLASMA) }
                        )
                    }

                    // Ammo Count & Reload Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 10.dp)
                    ) {
                        // Reload Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(TacticalSurface.copy(alpha = 0.9f))
                                .border(1.dp, TacticalBorder, RoundedCornerShape(8.dp))
                                .clickable { viewModel.reloadWeapon() }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("reload_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Refresh, contentDescription = "Reload", tint = AmberGold, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (localPlayer.isReloading) "RELOADING..." else "RELOAD",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Ammo readout
                        Box(
                            modifier = Modifier
                                .background(TacticalDark.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                                .border(1.dp, AmberGold, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${localPlayer.ammoInClip} / ${localPlayer.reserveAmmo}",
                                color = AmberGold,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    // LARGE ERGONOMIC FIRE BUTTON
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(CrimsonRed.copy(alpha = 0.85f))
                            .border(3.dp, Color.White.copy(alpha = 0.9f), CircleShape)
                            .clickable { viewModel.fireWeapon() }
                            .testTag("fire_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.FlashOn,
                                contentDescription = "Fire",
                                tint = Color.White,
                                modifier = Modifier.size(34.dp)
                            )
                            Text(
                                text = "FIRE",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }

        // 9. RESPAWN OVERLAY COUNTDOWN
        if (respawnSeconds > 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC000000)),
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
            .width(115.dp)
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

            // Gauge Fill
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

@Composable
private fun WeaponPill(
    weapon: WeaponType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val weaponColor = Color(weapon.colorHex)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) weaponColor.copy(alpha = 0.25f) else TacticalSurface.copy(alpha = 0.85f))
            .border(1.5.dp, if (isSelected) weaponColor else TacticalBorder, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Text(
            text = when (weapon) {
                WeaponType.RIFLE -> "RIFLE"
                WeaponType.SHOTGUN -> "SHOTGUN"
                WeaponType.PLASMA -> "PLASMA"
            },
            color = if (isSelected) weaponColor else Color.Gray,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
