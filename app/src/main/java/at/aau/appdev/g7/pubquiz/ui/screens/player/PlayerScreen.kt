package at.aau.appdev.g7.pubquiz.ui.screens.player

import android.util.Log
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import at.aau.appdev.g7.pubquiz.PlayerDestination
import at.aau.appdev.g7.pubquiz.TAG
import at.aau.appdev.g7.pubquiz.customTransitionSpec
import at.aau.appdev.g7.pubquiz.domain.Game
import at.aau.appdev.g7.pubquiz.domain.GamePhase
import dev.olshevski.navigation.reimagined.AnimatedNavHost
import dev.olshevski.navigation.reimagined.navigate
import dev.olshevski.navigation.reimagined.rememberNavController
import dev.olshevski.navigation.reimagined.replaceAll

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PlayerScreen(
    game: Game
) {
    val playerController = rememberNavController<PlayerDestination>(
        startDestination = PlayerDestination.Start)

    var playerReady by remember {
        mutableStateOf(false)
    }

    var connected by remember {
        mutableStateOf(false)
    }
    var gameStarting by remember {
        mutableStateOf(false)
    }
    var selectedAnswers by remember {
        mutableStateOf(listOf<String>())
    }

    AnimatedNavHost(controller = playerController, transitionSpec = customTransitionSpec) { destination ->
        when(destination) {
            is PlayerDestination.Start -> {
                Log.d(TAG, "PlayerDestination.Start: game.phase=${game.phase}")
                PlayerStart(
                    connected = connected,
                    onSearchGame = {
                        game.searchGame()
                        connected = (game.phase == GamePhase.CREATED)
                    },
                    onJoinGame = { name ->
                        game.joinGameAs(name)

                        game.onGameStarting = {
                            gameStarting = true
                        }
                        game.onNewRoundStart = {
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
                    onAnswer = { a ->
                        game.answerQuestion(a)
                        selectedAnswers = game.currentRound.answers.toList()
                    }
                )
            }

            is PlayerDestination.RoundEnd -> {}

            is PlayerDestination.GameOver -> {}
        }
    }
}

