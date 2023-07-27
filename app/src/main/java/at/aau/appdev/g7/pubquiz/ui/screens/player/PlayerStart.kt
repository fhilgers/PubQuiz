package at.aau.appdev.g7.pubquiz.ui.screens.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun PlayerStart(
    connected: Boolean = false,
    spacing: Dp = 40.dp,
    onSearchGame: () -> Unit = {},
    onJoinGame: (name:String) -> Unit = {}
) {
    var teamName by remember {
        mutableStateOf("")
    }

    Column(modifier = Modifier.fillMaxSize().padding(spacing),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = spacedBy(spacing, Alignment.Bottom)) {
        if (connected) {
            Text(text = "Connected to server")

            TextField(value = teamName,
                singleLine = true,
                label = { Text("Team Name") },
                onValueChange = { teamName = it })

            Button(onClick = { onJoinGame(teamName) }) {
                Text("Join Game")
            }
        } else {
            Button(onClick = onSearchGame) {
                Text("Search Game")
            }
        }
    }
}

@Preview
@Composable
fun PlayerStartPreview() {
    PlayerStart(true)
}