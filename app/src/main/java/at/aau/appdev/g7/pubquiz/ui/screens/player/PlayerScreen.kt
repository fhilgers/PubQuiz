package at.aau.appdev.g7.pubquiz.ui.screens.player

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import at.aau.appdev.g7.pubquiz.PlayerDestination
import at.aau.appdev.g7.pubquiz.TAG
import at.aau.appdev.g7.pubquiz.customTransitionSpec
import at.aau.appdev.g7.pubquiz.domain.Game
import at.aau.appdev.g7.pubquiz.domain.GamePhase
import dev.olshevski.navigation.reimagined.AnimatedNavHost
import dev.olshevski.navigation.reimagined.navigate
import dev.olshevski.navigation.reimagined.popUpTo
import dev.olshevski.navigation.reimagined.rememberNavController
import dev.olshevski.navigation.reimagined.replaceAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PlayerScreen(
    game: Game,
    onRestart: () -> Unit,
    onClose: () -> Unit,
) {
    val playerController = rememberNavController<PlayerDestination>(
        startDestination = PlayerDestination.Start)

    var playerReady by remember {
        mutableStateOf(false)
    }

    var connected by remember {
        mutableStateOf(false)
    }
    var searching by remember {
        mutableStateOf(false)
    }
    var connecting by remember {
        mutableStateOf(false)
    }

    var gameStarting by remember {
        mutableStateOf(false)
    }
    var selectedAnswers by remember {
        mutableStateOf(listOf<String>())
    }

    var roundSubmitted by remember {
        mutableStateOf(false)
    }

    val endpoints = game.endpoints.collectAsState()

    val composableScope = rememberCoroutineScope()

    var showCloseDialog by remember {
        mutableStateOf(false)
    }

    //TODO
    BackHandler {
        showCloseDialog = !showCloseDialog
    }

    if (showCloseDialog) {
        AlertDialog(
            onDismissRequest = {
                showCloseDialog = false
            },
            icon = { Icon(Icons.Filled.Warning, contentDescription = null) },
            title = {
                Text(text = "Go back?")
            },
            text = {
                Text(text = "Going back will stop discovering nearby games.")
            },
            confirmButton = {
                TextButton(onClick = onClose) {
                    Text("Close")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCloseDialog = false }) {
                    Text("Dismiss")
                }
            }
        )
    }


    AnimatedNavHost(controller = playerController, transitionSpec = customTransitionSpec) { destination ->
        when(destination) {
            is PlayerDestination.Start -> {
                Log.d(TAG, "PlayerDestination.Start: game.phase=${game.phase}")
                PlayerStart(
                    connected = connected,
                    searching = searching,
                    connecting = connecting,
                    endpoints = endpoints.value.toList(),
                    onSearchGame = {
                        game.searchGame()
                        //connected = (game.phase == GamePhase.CREATED)
                        searching = true
                    },
                    onConnectGame = { endpoint ->
                        composableScope.launch {
                            game.connectToGame(endpoint)
                            connecting = false
                            connected = true
                        }
                        searching = false
                        connecting = true
                    },
                    onJoinGame = { name ->
                        game.joinGameAs(name)

                        game.onGameStarting = {
                            gameStarting = true
                        }
                        game.onNewRoundStart = {
                            roundSubmitted = false
                            playerController.replaceAll(PlayerDestination.RoundStart)
                        }
                        game.onNewQuestion = {
                            selectedAnswers = game.currentRound.answers.toList()
                            playerController.navigate(PlayerDestination.Question(game.currentQuestionIdx))
                        }
                        game.onRoundEnd = {
                            playerController.navigate(PlayerDestination.RoundEnd)
                        }
                        game.onGameOver = {
                            playerController.replaceAll(PlayerDestination.GameOver)
                        }

                        playerController.navigate(PlayerDestination.Lobby)
                    }
                )
            }
            is PlayerDestination.Lobby -> {
                PlayerLobby(
                    playerName = game.playerName,
                    gameStarting = gameStarting,
                    readinessConfirmed = playerReady) {
                        playerReady = true
                        game.readyPlayer()
                    }
            }

            is PlayerDestination.RoundStart -> {
                PlayerRoundStart(
                    roundName = game.currentRound.name
                )
            }

            is PlayerDestination.Question -> {
                val index = destination.index
                PlayerQuestions(
                    questionText = game.currentQuestion.text,
                    answers = game.currentQuestion.answers,
                    selectedAnswer = selectedAnswers[index],
                    onAnswer = { answer ->
                        game.answerQuestion(answer)
                        selectedAnswers = game.currentRound.answers.toList()
                    }
                )
            }

            is PlayerDestination.RoundEnd -> {
                PlayerRoundEnd(
                    roundName = game.currentRound.name,
                    submitted = roundSubmitted,
                    questions = game.currentRound.questions,
                    selectedAnswers = selectedAnswers,
                    onAnswerChanged = { index, answer ->
                        game.answerQuestion(answer, index)
                        selectedAnswers = game.currentRound.answers.toList()
                    }
                ) {
                    game.submitRoundAnswers()
                    roundSubmitted = true
                }
            }

            is PlayerDestination.GameOver -> {
                PlayerGameOver() {
                    // reset state
                    playerReady = false
                    connected = false
                    gameStarting = false
                    selectedAnswers = listOf()
                    roundSubmitted = false
                    playerController.replaceAll(PlayerDestination.Start)
                    // and propagate restart
                    onRestart()
                }
            }
        }
    }
}

