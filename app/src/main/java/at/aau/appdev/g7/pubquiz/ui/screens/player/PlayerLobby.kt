package at.aau.appdev.g7.pubquiz.ui.screens.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun PlayerLobby(
    playerName: String,
    gameStarting: Boolean = false,
    readinessConfirmed: Boolean = false,
    onReady: () -> Unit = {}
) {
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(40.dp, Alignment.Bottom)) {
        Text(text = "Joined Game as $playerName")

        if (gameStarting) {
            Text(text = "Game is starting...")

            Button(onClick = onReady, enabled = !readinessConfirmed) {
                Text("Ready!")
            }
        } else {
            Text(text = "Waiting for other players...")
        }
    }
}

@Preview
@Composable
fun PlayerLobbyPreview() {
    PlayerLobby("Team 1", true, true)
}