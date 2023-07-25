package at.aau.appdev.g7.pubquiz

import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.with
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import at.aau.appdev.g7.pubquiz.demo.MasterDemoConnectivitySimulator
import at.aau.appdev.g7.pubquiz.demo.PlayerDemoConnectivitySimulator
import at.aau.appdev.g7.pubquiz.domain.Game
import at.aau.appdev.g7.pubquiz.domain.GameMessage
import at.aau.appdev.g7.pubquiz.domain.UserRole
import at.aau.appdev.g7.pubquiz.domain.interfaces.ConnectivityProvider
import at.aau.appdev.g7.pubquiz.domain.interfaces.DataProvider
import at.aau.appdev.g7.pubquiz.ui.screens.master.GameConfiguration
import at.aau.appdev.g7.pubquiz.ui.screens.master.MasterAnswerTimerScreen
import at.aau.appdev.g7.pubquiz.ui.screens.master.MasterAnswersScreen
import at.aau.appdev.g7.pubquiz.ui.screens.master.MasterLobby
import at.aau.appdev.g7.pubquiz.ui.screens.master.MasterQuestionsScreen
import at.aau.appdev.g7.pubquiz.ui.screens.master.MasterRoundsScreen
import at.aau.appdev.g7.pubquiz.ui.screens.master.MasterSetup
import at.aau.appdev.g7.pubquiz.ui.screens.master.MasterStart
import at.aau.appdev.g7.pubquiz.ui.screens.master.Player
import at.aau.appdev.g7.pubquiz.ui.screens.master.PlayerAnswer
import at.aau.appdev.g7.pubquiz.ui.screens.player.PlayerScreen
import at.aau.appdev.g7.pubquiz.ui.theme.PubQuizTheme
import dev.olshevski.navigation.reimagined.AnimatedNavHost
import dev.olshevski.navigation.reimagined.NavAction
import dev.olshevski.navigation.reimagined.NavBackHandler
import dev.olshevski.navigation.reimagined.NavController
import dev.olshevski.navigation.reimagined.NavTransitionSpec
import dev.olshevski.navigation.reimagined.moveToTop
import dev.olshevski.navigation.reimagined.navigate
import dev.olshevski.navigation.reimagined.pop
import dev.olshevski.navigation.reimagined.popUpTo
import dev.olshevski.navigation.reimagined.rememberNavController
import kotlinx.parcelize.Parcelize

const val TAG = "PubQuiz"
const val DEMO_MODE = true

class MainActivity : ComponentActivity() {
    lateinit var connectivityProvider: ConnectivityProvider<GameMessage>
    lateinit var dataProvider: DataProvider
    lateinit var game: Game

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PubQuizTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavHostScreen(onUserRoleChosen = {
                        // TODO replace these provider stubs with real ones as soon as they are implemented
                        connectivityProvider = if (DEMO_MODE) {
                            when(it) {
                                UserRole.PLAYER -> PlayerDemoConnectivitySimulator()
                                UserRole.MASTER -> MasterDemoConnectivitySimulator()
                            }
                        } else /* TODO replace by real provider */ MasterDemoConnectivitySimulator()
                        dataProvider = object: DataProvider {}
                        game = Game(it, connectivityProvider, dataProvider)
                        Log.i(TAG, "MainActivity: game created: ${game.phase}")
                        game
                    })
                }
            }
        }
    }
}

sealed class MasterDestination : Parcelable {
    @Parcelize
    object Start : MasterDestination()

    @Parcelize
    data class Setup(val index: Int? = null) : MasterDestination()

    @Parcelize
    data class Lobby(val gameIndex: Int) : MasterDestination()

    @Parcelize
    data class Rounds(val gameIndex: Int) : MasterDestination()

    @Parcelize
    data class Questions(val gameIndex: Int) : MasterDestination()

    @Parcelize
    data class Answers(val gameIndex: Int) : MasterDestination()

    @Parcelize
    data class AnswerTimer(val gameIndex: Int) : MasterDestination()

    @Parcelize
    data class RoundEnd(val gameIndex: Int) : MasterDestination()
}

sealed class PlayerDestination : Parcelable {
    @Parcelize
    object Start : PlayerDestination()


    @Parcelize
    object Lobby : PlayerDestination()
}

enum class BottomDestination {
    Player,
    Master,
}

val BottomDestination.title
    get() = when (this) {
        BottomDestination.Player -> "Player"
        BottomDestination.Master -> "Master"
    }

val BottomDestination.icon
    get() = when (this) {
        BottomDestination.Player -> Icons.Filled.Face
        BottomDestination.Master -> Icons.Filled.Person
    }

@OptIn(ExperimentalAnimationApi::class)
val customTransitionSpec = NavTransitionSpec<Any?> { action: NavAction, _, _ ->
    val direction = if (action == NavAction.Pop) {
        AnimatedContentScope.SlideDirection.End
    } else {
        AnimatedContentScope.SlideDirection.Start
    }
    slideIntoContainer(direction).with(slideOutOfContainer(direction))
}

@Composable
private fun BottomNavigationBackHandler(
    navController: NavController<BottomDestination>
) {
    BackHandler(enabled = navController.backstack.entries.size > 1) {
        val lastEntry = navController.backstack.entries.last()
        if (lastEntry.destination == BottomDestination.values()[0]) {
            // The start destination should always be the last to pop. We move it to the start
            // to preserve its saved state and view models.
            navController.moveLastEntryToStart()
        } else {
            navController.pop()
        }
    }
}

private fun NavController<BottomDestination>.moveLastEntryToStart() {
    setNewBackstack(
        entries = backstack.entries.toMutableList().also {
            val entry = it.removeLast()
            it.add(0, entry)
        },
        action = NavAction.Pop
    )
}


@OptIn(
    ExperimentalAnimationApi::class, ExperimentalAnimationApi::class,
    ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class
)
@Composable
fun NavHostScreen(
    onUserRoleChosen: (UserRole) -> Game
) {


    val navController = rememberNavController<BottomDestination>(
        startDestination = BottomDestination.values()[0],
    )


    BottomNavigationBackHandler(navController = navController)

    var showBottomNavigation by remember {
        mutableStateOf(true)
    }

    var game: Game? by remember {
        mutableStateOf(null)
    }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(visible = showBottomNavigation) {
                NavigationBar {
                    val lastDestination = navController.backstack.entries.last().destination

                    BottomDestination.values().forEach { destination ->
                        NavigationBarItem(
                            selected = destination == lastDestination,
                            onClick = {
                                if (!navController.moveToTop { it == destination }) {
                                    game = onUserRoleChosen.invoke(when (destination) {
                                        BottomDestination.Player -> UserRole.PLAYER
                                        BottomDestination.Master -> UserRole.MASTER
                                    })
                                    navController.navigate(destination)
                                }

                            },
                            icon = {
                                Icon(
                                    destination.icon,
                                    contentDescription = destination.title
                                )
                            },
                            label = { Text(text = destination.title) })
                    }
                }
            }
        }
    ) {
        Surface(Modifier.padding(it)) {
            AnimatedNavHost(navController, transitionSpec = customTransitionSpec) { destination ->
                when (destination) {
                    BottomDestination.Player -> {
                        showBottomNavigation = false
                        PlayerScreen(game!!)
                    }

                    BottomDestination.Master -> {
                        MasterScreen(
                            game = game!!,
                            showBottomNavigation = {
                                showBottomNavigation = it
                            })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MasterScreen(
    game: Game,
    showBottomNavigation: (Boolean) -> Unit
) {
    var configuredGames by rememberSaveable {
        mutableStateOf(listOf<GameConfiguration>())
    }
    var players by rememberSaveable {
        mutableStateOf(listOf<Player>())
    }
    var playerAnswers by rememberSaveable {
        mutableStateOf(listOf<PlayerAnswer>())
    }

//    val basePlayerAnswers = players.map {
//        PlayerAnswer(it.name, null)
//    }
//
//    val makeAnswers: (tick: Int) -> List<PlayerAnswer> = {tick ->
//        if (tick <= 1) {
//            basePlayerAnswers
//        } else if (tick <= 2) {
//            basePlayerAnswers.toMutableList().also {
//                it[0].answer = 0
//            }
//        } else if (tick <= 3 ) {
//             basePlayerAnswers.toMutableList().also {
//                it[0].answer = 1
//                it[1].answer = 0
//                it[2].answer = 1
//            }
//        } else {
//            basePlayerAnswers.toMutableList().also {
//                it[0].answer = 1
//                it[1].answer = 0
//                it[2].answer = 2
//                it[3].answer = 1
//            }
//        }
//    }

    val masterController = rememberNavController<MasterDestination>(
        startDestination = MasterDestination.Start
    )

    var currentRound by remember {
        mutableStateOf(0)
    }

    var currentQuestion by remember {
        mutableStateOf(0)
    }

    NavBackHandler(controller = masterController)

    AnimatedNavHost(masterController, transitionSpec = customTransitionSpec) { destination ->
        when (destination) {
            is MasterDestination.Start -> {
                showBottomNavigation(true)
                MasterStart(gameConfigurations = configuredGames, onSetupGame = {
                    masterController.navigate(MasterDestination.Setup())
                }, onEdit = {
                    masterController.navigate(MasterDestination.Setup(it))
                }, onDelete = {
                    configuredGames = configuredGames.toMutableList().also { list ->
                        list.removeAt(it)
                    }
                }, onHost = {
                    val c = configuredGames[it]
                    game.setupGame(c.numberOfRounds, c.numberOfQuestions, c.numberOfAnswers)
                    Log.d(TAG, "MasterScreen: game setup: $c -> ${game.phase}")
                    game.createGame()
                    Log.d(TAG, "MasterScreen: game created: ${game.phase}")
                    game.onPlayerJoined = { p ->
                        if (players.none { it.name == p }) {
                            players = players.toMutableList().also { it.add(Player(p, false)) }
                        }
                        Log.d(TAG, "MasterScreen: player $p joined")
                    }
                    game.onPlayerLeft = { p ->
                        players = players.toMutableList().also { it.removeIf { it.name == p } }
                        Log.d(TAG, "MasterScreen: player $p left")
                    }
                    game.onPlayerReady = { p ->
                        players = players.map { item ->
                            if (item.name == p) {
                                item.copy(ready = true)
                            } else {
                                item
                            }
                        }
                        Log.d(TAG, "MasterScreen: player $p ready")
                    }
                    game.onPlayerAnswer = { p ->
                        playerAnswers = playerAnswers.map { item ->
                            if (item.from == p) {
                                item.copy(answered = true)
                            } else {
                                item
                            }
                        }
                        Log.d(TAG, "MasterScreen: player $p answered: $playerAnswers")
                    }
                    game.onPlayerSubmitRound = { p ->
                        playerAnswers = playerAnswers.map { item ->
                            if (item.from == p) {
                                item.copy(answered = true)
                            } else {
                                item
                            }
                        }
                        Log.d(TAG, "MasterScreen: player $p submit round")
                    }
                    masterController.navigate(MasterDestination.Lobby(it))
                })
            }

            is MasterDestination.Setup -> {
                showBottomNavigation(true)
                destination.index?.also {
                    MasterSetup(
                        onCreate = { config ->
                            configuredGames = configuredGames.toMutableList().also { list ->
                                list[it] = config
                            }
                            masterController.pop()
                        },
                        onExit = { masterController.pop() },
                        initialGameConfiguration = configuredGames[it]
                    )
                } ?: run {
                    MasterSetup(
                        onCreate = { config ->
                            configuredGames = configuredGames.toMutableList().also { list ->
                                list.add(config)
                            }
                            masterController.pop()
                        },
                        onExit = { masterController.pop() },
                    )
                }
            }

            is MasterDestination.Lobby -> {
                Log.d(TAG, "MasterScreen: MasterDestination.Lobby")
                showBottomNavigation(false)

                MasterLobby(
                    players = players,
                    password = "456048",
                    onClose = {
                        masterController.popUpTo { it == MasterDestination.Start }
                    },
                    onReady = {
                        game.startGame()
                        Log.d(TAG, "MasterScreen: game started: ${game.phase}")
                    },
                    onStart = {
                        game.forceStartGame()
                        masterController.navigate(MasterDestination.Rounds(destination.gameIndex))
                    }
                )
            }

            is MasterDestination.Rounds -> {
                showBottomNavigation(false)

                val roundNames = game.roundNames

                MasterRoundsScreen(
                    roundNames = roundNames,
                    nextRoundName = roundNames[game.currentRoundIdx + 1]
                ) {
                    game.startNextRound()
                    masterController.navigate(MasterDestination.Questions(destination.gameIndex))
                }
            }

            is MasterDestination.Questions -> {
                showBottomNavigation(false)

                val questions = remember {
                    game.currentRound.questions.map { it.text ?: "Question ${it.index}" }
                }

                MasterQuestionsScreen(
                    questions = questions,
                    nextQuestion = game.currentQuestionIdx + 1
                ) {
                    game.startNextQuestion()
                    playerAnswers = game.players.values.map { PlayerAnswer(it.name) }
                    masterController.navigate(MasterDestination.Answers(destination.gameIndex))
                }
            }

            is MasterDestination.Answers -> {
                showBottomNavigation(false)

                MasterAnswersScreen(answers = game.currentQuestion.answers) {
                    game.selectCorrectAnswer(it)
                    masterController.navigate(MasterDestination.AnswerTimer(destination.gameIndex))
                }
            }

            is MasterDestination.AnswerTimer -> {
                showBottomNavigation(false)
                // TODO we should consider to move timer functionality to the game class
                var ticks by remember {
                    mutableStateOf(0)
                }
                val time = configuredGames[destination.gameIndex].timePerQuestion
                var timerStarted by remember {
                    mutableStateOf(false)
                }

                MasterAnswerTimerScreen(
                    title = game.currentQuestion.text ?: "Question ${game.currentQuestion.index}",
                    maxTicks = time,
                    ticks = ticks,
                    playerAnswers = playerAnswers,
                    onTick = {
                        ticks++
                        if (ticks >= time) {
                            if (currentQuestion == configuredGames[destination.gameIndex].numberOfQuestions - 1) {
                                playerAnswers = game.players.values.map { PlayerAnswer(it.name) }
                                game.endRound()
                                masterController.navigate(MasterDestination.RoundEnd(destination.gameIndex))
                            } else {
                                currentQuestion++
                                masterController.popUpTo { it == MasterDestination.Questions(destination.gameIndex) }
                            }
                        } },
                    timerStarted = timerStarted,
                    onStartTimer = { timerStarted = true },
                    onPauseTimer = { timerStarted = false },
                    onSkipTimer = {
                        ticks = time
                        timerStarted = true
                    })
            }

            is MasterDestination.RoundEnd -> {
                showBottomNavigation(false)
                // TODO
                var ticks by remember {
                    mutableStateOf(0)
                }
                val timeout = configuredGames[destination.gameIndex].timePerQuestion
                var timerStarted by remember {
                    mutableStateOf(true)
                }

                MasterAnswerTimerScreen(
                    title = game.currentRound.name ?: "Round ${game.currentRound.index}",
                    maxTicks = timeout,
                    ticks = ticks,
                    playerAnswers = playerAnswers,
                    onTick = {
                        ticks++
                        if (ticks >= timeout) {
                            if (currentRound == configuredGames[destination.gameIndex].numberOfRounds - 1) {
                                masterController.popUpTo { it == MasterDestination.Start }
                                // TODO game finished
                            } else {
                                currentQuestion = 0
                                currentRound++
                                masterController.popUpTo { it == MasterDestination.Rounds(destination.gameIndex) }
                            }
                        } },
                    timerStarted = timerStarted,
                    onStartTimer = { timerStarted = true },
                    onPauseTimer = { timerStarted = false },
                    onSkipTimer = {
                        ticks = timeout
                        timerStarted = true
                    })

            }
        }
    }
}