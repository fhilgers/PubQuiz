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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun PlayerLobby(
    playerName: String,
    spacing: Dp = 40.dp,
    gameStarting: Boolean = false,
    readinessConfirmed: Boolean = false,
    onReady: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxSize().padding(spacing),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing, Alignment.Bottom)) {

        val joinedText = "Joined Game as $playerName"
        Text(text = AnnotatedString(text = joinedText,
            spanStyles =  listOf(AnnotatedString.Range(SpanStyle(fontWeight = FontWeight.Bold),
                start = joinedText.lastIndexOf(' ') + 1,
                end = joinedText.length))))

        if (gameStarting) {
            Text(text = "Game is starting...")

            if (readinessConfirmed) {
                Text(text = "Waiting for other players...")
            } else {
                Text(text = "Please confirm readiness!", fontWeight = FontWeight.Bold)

                Button(onClick = onReady) {
                    Text("Ready!")
                }
            }
        }
    }
}

@Preview
@Composable
fun PlayerLobbyPreview() {
    PlayerLobby("Team 1", gameStarting = true, readinessConfirmed = true)
}