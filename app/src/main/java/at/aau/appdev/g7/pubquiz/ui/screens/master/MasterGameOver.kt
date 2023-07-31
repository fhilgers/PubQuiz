package at.aau.appdev.g7.pubquiz.ui.screens.master

import android.os.Parcelable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.Absolute.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.aau.appdev.g7.pubquiz.ui.components.AnswerButton
import at.aau.appdev.g7.pubquiz.ui.theme.PubQuizTheme
import kotlinx.parcelize.Parcelize

@Parcelize
data class GameResultAnswer(
    val answer: String,
    val correct: Boolean,
): Parcelable

@Parcelize
data class GameResultRound(
    val answers: List<GameResultAnswer>,
): Parcelable

@Parcelize
data class GameResultPlayer(
    val name: String,
    val rounds: List<GameResultRound>
): Parcelable

@Parcelize
data class GameResult(
    val players: List<GameResultPlayer>,
): Parcelable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasterGameOver(
    result: GameResult,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {

    var selectedIndex by remember { mutableIntStateOf(0) }

    val pointsPerPlayerPerRound = result.players.map { player ->
        player.rounds.map { round ->
            round.answers.count { it.correct }
        }
    }

    val pointsPerPlayer = pointsPerPlayerPerRound.map { it.sum() }

    val placePerPlayer = pointsPerPlayer.map { points ->
        pointsPerPlayer.count { it > points } + 1
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = "Game Results") }, navigationIcon = {
                IconButton(onClick = onExit) {
                    Icon(imageVector = Icons.Filled.Close, contentDescription = "Exit Lobby")
                }
            })
        },
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                    onClick = onExit
                ) {
                    Text(text = "Play Again")
                }
            }
    ) { paddingValues ->

        Column(
            modifier = modifier.padding(paddingValues),
        ) {
            TabRow(selectedTabIndex = selectedIndex) {
                result.players.forEachIndexed { index, player ->
                    Tab(selected = selectedIndex == index, onClick = { selectedIndex = index }) {
                        Text(text = player.name)
                    }
                }

            }

            Spacer(modifier = Modifier.height(20.dp))

            Card(Modifier
                .padding(16.dp)
                .fillMaxWidth()
            ) {
                Column(

                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.CenterHorizontally),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "${placePerPlayer[selectedIndex]}. Place", style = MaterialTheme.typography.displayMedium)
                    Text(text = "${pointsPerPlayer[selectedIndex]} Points", style = MaterialTheme.typography.displaySmall)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Row(
                modifier = Modifier
                    .weight(1f),
                horizontalArrangement = Arrangement.Center,
            ) {


                result.players[selectedIndex].rounds.forEachIndexed { index, round ->

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {

                        Text(text = "Round ${index + 1}")

                        round.answers.forEachIndexed { index, answer ->
                            AnswerButton(text = answer.answer, selected = answer.correct)
                        }
                    }
                }
            }

        }
    }
}

@Preview
@Composable
fun MasterGameOverPreview() {

    val result = GameResult(
        players = listOf(
            GameResultPlayer(
                name = "Team 1",
                rounds = listOf(
                    GameResultRound(
                        answers = listOf(
                            GameResultAnswer("A", true),
                            GameResultAnswer("B", false),
                            GameResultAnswer("C", false),
                            GameResultAnswer("D", false),
                        )
                    ),
                    GameResultRound(
                        answers = listOf(
                            GameResultAnswer("A", true),
                            GameResultAnswer("B", false),
                            GameResultAnswer("C", false),
                            GameResultAnswer("D", false),
                        )
                    ),
                    GameResultRound(
                        answers = listOf(
                            GameResultAnswer("A", true),
                            GameResultAnswer("B", false),
                            GameResultAnswer("C", false),
                            GameResultAnswer("D", false),
                        )
                    ),
                )
            ),
            GameResultPlayer(
                name = "Team 2",
                rounds = listOf(
                    GameResultRound(
                        answers = listOf(
                            GameResultAnswer("A", true),
                            GameResultAnswer("C", true),
                            GameResultAnswer("C", false),
                            GameResultAnswer("D", false),
                        )
                    ),
                    GameResultRound(
                        answers = listOf(
                            GameResultAnswer("A", true),
                            GameResultAnswer("B", false),
                            GameResultAnswer("C", false),
                            GameResultAnswer("D", false),
                        )
                    ),
                    GameResultRound(
                        answers = listOf(
                            GameResultAnswer("A", true),
                            GameResultAnswer("B", false),
                            GameResultAnswer("C", false),
                            GameResultAnswer("D", false),
                        )
                    ),
                )
            ),
        )
    )

    PubQuizTheme {
        MasterGameOver(result = result, onExit = { /*TODO*/ }, Modifier.fillMaxSize())
    }
}