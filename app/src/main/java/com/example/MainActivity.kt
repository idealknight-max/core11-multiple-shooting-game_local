package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.game.GameMode
import com.example.game.GameViewModel
import com.example.ui.GameOverDialog
import com.example.ui.GameScreen
import com.example.ui.LobbyScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TacticalDark

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Keep screen on during intense gameplay
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Immersive sticky full-screen for 3D game
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = TacticalDark
                ) {
                    val viewModel: GameViewModel = viewModel()
                    MainGameApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun MainGameApp(viewModel: GameViewModel) {
    val gameMode by viewModel.gameMode.collectAsState()
    val localPlayer by viewModel.localPlayer.collectAsState()
    val remotePlayer by viewModel.remotePlayer.collectAsState()
    val winnerName by viewModel.winnerName.collectAsState()

    // Handle back button to safely return to lobby
    BackHandler(enabled = gameMode != GameMode.LOBBY) {
        viewModel.exitToLobby()
    }

    when (gameMode) {
        GameMode.LOBBY -> {
            LobbyScreen(viewModel = viewModel)
        }
        GameMode.PLAYING -> {
            GameScreen(viewModel = viewModel)
        }
        GameMode.GAME_OVER -> {
            GameScreen(viewModel = viewModel)
            GameOverDialog(
                winnerName = winnerName,
                localPlayer = localPlayer,
                remotePlayer = remotePlayer,
                onRematch = {
                    if (viewModel.isMultiplayer.value) {
                        viewModel.hostGame()
                    } else {
                        viewModel.startSoloGame()
                    }
                },
                onExitLobby = { viewModel.exitToLobby() }
            )
        }
    }
}
