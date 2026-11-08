package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.game.model.Player
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TacticalBorder
import com.example.ui.theme.TacticalDark
import com.example.ui.theme.TacticalSurface

@Composable
fun GameOverDialog(
    winnerName: String?,
    localPlayer: Player,
    remotePlayer: Player?,
    onRematch: () -> Unit,
    onExitLobby: () -> Unit
) {
    val isVictory = winnerName == localPlayer.name

    Dialog(onDismissRequest = onExitLobby) {
        Box(
            modifier = Modifier
                .width(420.dp)
                .background(TacticalDark, RoundedCornerShape(16.dp))
                .border(2.dp, if (isVictory) NeonCyan else CrimsonRed, RoundedCornerShape(16.dp))
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = if (isVictory) AmberGold else CrimsonRed,
                    modifier = Modifier.size(54.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (isVictory) "VICTORY!" else "DEFEAT",
                    color = if (isVictory) NeonCyan else CrimsonRed,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black
                )

                Text(
                    text = "Winner: ${winnerName ?: "Match Finished"}",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Stats Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    PlayerStatCard(
                        playerName = "You (${localPlayer.name})",
                        kills = localPlayer.kills,
                        deaths = localPlayer.deaths,
                        color = NeonCyan
                    )

                    remotePlayer?.let { remote ->
                        PlayerStatCard(
                            playerName = remote.name,
                            kills = remote.kills,
                            deaths = remote.deaths,
                            color = CrimsonRed
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onExitLobby,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("exit_lobby_button"),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TacticalBorder)
                    ) {
                        Icon(Icons.Default.Home, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Lobby", color = Color.White)
                    }

                    Button(
                        onClick = onRematch,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("rematch_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Rematch", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerStatCard(
    playerName: String,
    kills: Int,
    deaths: Int,
    color: Color
) {
    Column(
        modifier = Modifier
            .background(TacticalSurface, RoundedCornerShape(10.dp))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = playerName, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "$kills Kills", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
        Text(text = "$deaths Deaths", color = Color.Gray, fontSize = 12.sp)
    }
}
