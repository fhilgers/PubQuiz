package at.aau.appdev.g7.pubquiz.ui.screens.master

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.aau.appdev.g7.pubquiz.domain.PlayerRoundScoreRecord
import at.aau.appdev.g7.pubquiz.ui.components.HandleUnimplementedBackNavigation
import at.aau.appdev.g7.pubquiz.ui.components.PlayerAvatar



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasterRoundEndScreen(
    roundName: String,
    playerAnswers: List<PlayerRoundScoreRecord>,
    canContinue: Boolean,
    remainingTime: Int,
    timerStarted: Boolean,
    onStartTimer: () -> Unit,
    onPauseTimer: () -> Unit,
    onContinue: () -> Unit = {},
) {
    HandleUnimplementedBackNavigation()

    Scaffold(
            floatingActionButtonPosition = FabPosition.End,
            floatingActionButton = {
                if(canContinue) {
                    ExtendedFloatingActionButton(onClick = onContinue) {
                        Text(text = "Continue")
                    }
                }
            },
        topBar = {
            TopAppBar(title = { Text(text = "$roundName Scores") })
        }
    ) { paddingValues ->
        Column(
            Modifier.padding(paddingValues),
        ) {
            Card(Modifier.padding(16.dp)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "$remainingTime seconds remaining",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    // ExtendedFloatingActionButton(onClick = { if (it) onPauseTimer() else onStartTimer() }) {
                    //                        Text(text = if (timerStarted) "Pause Timer" else "Start Timer")
                    //                }
                    Spacer(Modifier.weight(1f))
                    if (timerStarted) {
                        IconButton(onClick = onPauseTimer) {
                            Icon(Icons.Filled.Pause, contentDescription = "Pause Timer")
                        }
                    } else {
                        IconButton(onClick = onStartTimer) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "Start Timer")
                        }
                    }
                }
            }
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier.padding(paddingValues)
            ) {
                items(playerAnswers) { answer ->
                    ListItem(
                        leadingContent = { PlayerAvatar(nickname = answer.player, size = 40.dp) },
                        headlineContent = {
                            Text(
                                text = answer.player,
                                style = MaterialTheme.typography.headlineSmall,
                            )
                        },
                        supportingContent = {
                            Row(horizontalArrangement = spacedBy(8.dp)) {
                                for (a in answer.roundAnswers) {
                                    Text(a)
                                }
                            }
                        },
                        trailingContent = {
                            Text(text = "${answer.roundScore}", fontSize = 32.sp)
                        },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun MasterRoundEndScreenPreview() {
    MasterRoundEndScreen(
        roundName = "Round 1",
        playerAnswers = listOf(
            PlayerRoundScoreRecord("Player 1", listOf("A", "B", "C", "D"), 4),
            PlayerRoundScoreRecord("Player 2", listOf("A", "B", "C", "B"), 3),
            PlayerRoundScoreRecord("Team 3", listOf("A", "C", "B", "C"), 1),
            PlayerRoundScoreRecord("Team Zero", listOf("C", "C", "B", "C"), 0),
        ),
        remainingTime = 30,
        timerStarted = true,
        onStartTimer = {},
        canContinue = true,
        onPauseTimer = {},
    )
}
