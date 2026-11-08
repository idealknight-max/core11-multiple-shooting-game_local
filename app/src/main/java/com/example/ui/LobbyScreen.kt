package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.game.GameViewModel
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.PlasmaPurple
import com.example.ui.theme.TacticalBorder
import com.example.ui.theme.TacticalDark
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TacticalSurfaceVariant
import com.example.ui.theme.ToxicGreen

@Composable
fun LobbyScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val playerName by viewModel.playerName.collectAsState()
    val discoveredRooms by viewModel.networkManager.discoveredRooms.collectAsState()
    val connectionStatus by viewModel.networkManager.connectionStatus.collectAsState()

    var showGuideDialog by remember { mutableStateOf(false) }
    var directIpInput by remember { mutableStateOf("") }
    var showDirectIpDialog by remember { mutableStateOf(false) }
    val localIp = remember { viewModel.networkManager.getLocalIpAddress() }

    LaunchedEffect(Unit) {
        viewModel.networkManager.startDiscovery()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TacticalDark)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // LEFT COLUMN: Game Title, Profile & Local Wi-Fi Info
            Column(
                modifier = Modifier
                    .weight(0.42f)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    // Title Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                text = "LOCAL STRIKE",
                                color = NeonCyan,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "3D WI-FI MULTIPLAYER FPS",
                                color = AmberGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                        }

                        IconButton(
                            onClick = { showGuideDialog = true },
                            modifier = Modifier.testTag("help_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Rules & Guide", tint = NeonCyan)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Player Name Customization
                    Card(
                        colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TacticalBorder),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "OPERATIVE CALLSIGN",
                                color = Color.Gray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = playerName,
                                onValueChange = { viewModel.playerName.value = it },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = NeonCyan) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = TacticalBorder,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("player_name_input")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Local Wi-Fi Network Info Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TacticalBorder),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Router, contentDescription = null, tint = ToxicGreen, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "YOUR WI-FI IP ADDRESS",
                                    color = Color.Gray,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = localIp,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Practice Mode Button (Offline with AI Bots)
                Button(
                    onClick = { viewModel.startSoloGame() },
                    colors = ButtonDefaults.buttonColors(containerColor = TacticalSurfaceVariant),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, AmberGold),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("solo_practice_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = AmberGold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SOLO PRACTICE (VS BOTS)",
                        color = AmberGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // RIGHT COLUMN: Multiplayer Host & Join Options
            Column(
                modifier = Modifier
                    .weight(0.58f)
                    .fillMaxSize()
            ) {
                // Host Button Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, NeonCyan),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "CREATE WI-FI MATCH",
                                    color = NeonCyan,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "Host a 3D deathmatch for your friend on this Wi-Fi",
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                            }

                            Button(
                                onClick = { viewModel.hostGame() },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("host_game_button")
                            ) {
                                Text(
                                    text = "HOST MATCH",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        if (connectionStatus.contains("Hosting") || connectionStatus.contains("Waiting")) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = connectionStatus,
                                color = ToxicGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Discovered Rooms List Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Wifi, contentDescription = null, tint = ToxicGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "AVAILABLE LOCAL ROOMS (${discoveredRooms.size})",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row {
                        IconButton(
                            onClick = { viewModel.networkManager.startDiscovery() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = NeonCyan, modifier = Modifier.size(18.dp))
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        Button(
                            onClick = { showDirectIpDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = TacticalSurfaceVariant),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("Direct IP", fontSize = 10.sp, color = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // List of detected Wi-Fi matches
                if (discoveredRooms.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(TacticalSurface.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .border(1.dp, TacticalBorder, RoundedCornerShape(10.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Scanning common Wi-Fi for active rooms...",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Ask your friend to tap 'HOST MATCH' or use Direct IP",
                                color = Color.DarkGray,
                                fontSize = 11.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(discoveredRooms) { room ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ToxicGreen),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.joinGame(room.hostIp) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = room.roomName,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "IP: ${room.hostIp} • Host: ${room.hostPlayerName}",
                                            color = Color.Gray,
                                            fontSize = 11.sp
                                        )
                                    }

                                    Button(
                                        onClick = { viewModel.joinGame(room.hostIp) },
                                        colors = ButtonDefaults.buttonColors(containerColor = ToxicGreen),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("JOIN", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Direct IP Connect Dialog
        if (showDirectIpDialog) {
            Dialog(onDismissRequest = { showDirectIpDialog = false }) {
                Box(
                    modifier = Modifier
                        .width(360.dp)
                        .background(TacticalDark, RoundedCornerShape(12.dp))
                        .border(1.dp, NeonCyan, RoundedCornerShape(12.dp))
                        .padding(18.dp)
                ) {
                    Column {
                        Text(
                            text = "CONNECT VIA DIRECT IP",
                            color = NeonCyan,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Enter your friend's local Wi-Fi IP address shown on their screen:",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = directIpInput,
                            onValueChange = { directIpInput = it },
                            placeholder = { Text("e.g. 192.168.1.50", color = Color.DarkGray) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = TacticalBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = { showDirectIpDialog = false },
                                colors = ButtonDefaults.buttonColors(containerColor = TacticalSurface)
                            ) {
                                Text("Cancel", color = Color.White)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (directIpInput.isNotBlank()) {
                                        showDirectIpDialog = false
                                        viewModel.joinGame(directIpInput.trim())
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                            ) {
                                Text("Connect", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Help & Rules Guide Dialog
        if (showGuideDialog) {
            Dialog(onDismissRequest = { showGuideDialog = false }) {
                Box(
                    modifier = Modifier
                        .width(460.dp)
                        .background(TacticalDark, RoundedCornerShape(12.dp))
                        .border(1.dp, AmberGold, RoundedCornerShape(12.dp))
                        .padding(20.dp)
                ) {
                    Column {
                        Text(
                            text = "HOW TO PLAY & COMBAT RULES",
                            color = AmberGold,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "📡 LOCAL WI-FI MULTIPLAYER:",
                            color = NeonCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "1. Connect both devices to the same Wi-Fi router or Mobile Hotspot.\n2. Player 1 taps 'HOST MATCH'.\n3. Player 2 taps 'JOIN' in the list (or enters Player 1's IP).",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "🛡️ HEALTH & ARMOR MECHANICS:",
                            color = ToxicGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "• Armor absorbs 66% of incoming shot damage!\n• When armor breaks, health takes 100% direct damage.\n• Always pick up Armor Vests to stay protected.",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "📦 ARENA LOOTS (RESPAWNS EVERY 15s):",
                            color = PlasmaPurple,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "• 💚 MedKit: Heals +35 Health\n• 🛡️ Armor Vest: +50 Armor Plate\n• 📦 Ammo Crate: +30 Ammunition\n• 💥 Shotgun: High close-range burst damage\n• ⚡ Plasma Cannon: Devastating rapid energy blaster",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { showGuideDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = AmberGold),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("GOT IT", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
