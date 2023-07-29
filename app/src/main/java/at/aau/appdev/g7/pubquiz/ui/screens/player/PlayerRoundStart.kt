package at.aau.appdev.g7.pubquiz.ui.screens.player

import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerRoundStart(
    roundName: String
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(roundName) })
        }
    ) { paddingValues ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = spacedBy(40.dp, Alignment.CenterVertically)) {
            CircularProgressIndicator(
                modifier = Modifier.size(256.dp),
                color = Color.Blue,
                strokeWidth = 16.dp
            )

            Text("$roundName is starting...")

            Text(text = "Get ready!", fontWeight = FontWeight.Bold)
        }
    }
}

@Preview
@Composable
fun PlayerRoundStartPreview() {
    PlayerRoundStart("Round 1")
}