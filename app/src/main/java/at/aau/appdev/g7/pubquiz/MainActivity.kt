package at.aau.appdev.g7.pubquiz

import android.os.Bundle
import android.os.Parcelable
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
import at.aau.appdev.g7.pubquiz.ui.screens.master.GameConfiguration
import at.aau.appdev.g7.pubquiz.ui.screens.master.MasterLobby
import at.aau.appdev.g7.pubquiz.ui.screens.master.MasterSetup
import at.aau.appdev.g7.pubquiz.ui.screens.master.MasterStart
import at.aau.appdev.g7.pubquiz.ui.screens.master.Player
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PubQuizTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    NavHostScreen()
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
    object Lobby : MasterDestination()
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
fun NavHostScreen() {


    val navController = rememberNavController<BottomDestination>(
        startDestination = BottomDestination.values()[0],
    )


    BottomNavigationBackHandler(navController = navController)

    var showBottomNavigation by remember {
        mutableStateOf(true)
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
                        showBottomNavigation = true
                    }

                    BottomDestination.Master -> MasterScreen(showBottomNavigation = {
                        showBottomNavigation = it
                    })
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MasterScreen(
    showBottomNavigation: (Boolean) -> Unit
) {
    var configuredGames by rememberSaveable {
        mutableStateOf(listOf<GameConfiguration>())
    }
    val players =
        listOf(Player("Hans Mueller", false), Player("Manfred Emmerich", true))

    val masterController = rememberNavController<MasterDestination>(
        startDestination = MasterDestination.Start
    )
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
                    masterController.navigate(MasterDestination.Lobby)
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

                showBottomNavigation(false)

                MasterLobby(
                    players = players,
                    password = "456048",
                    onClose = {
                        masterController.popUpTo { it == MasterDestination.Start }
                    },
                    onStart = {}
                )
            }
        }
    }
}