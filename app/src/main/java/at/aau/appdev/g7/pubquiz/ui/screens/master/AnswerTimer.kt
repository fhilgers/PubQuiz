package at.aau.appdev.g7.pubquiz.ui.screens.master

import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.expandIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.with
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.aau.appdev.g7.pubquiz.TAG
import at.aau.appdev.g7.pubquiz.ui.components.HandleUnimplementedBackNavigation
import at.aau.appdev.g7.pubquiz.ui.components.PlayerAvatar
import at.aau.appdev.g7.pubquiz.ui.theme.PubQuizTheme
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

data class PlayerAnswer(val from: String, val answered: Boolean = false)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun MasterAnswerTimerScreen(
    title: String,
    maxTicks: Int,
    ticks: Int,
    playerAnswers: List<PlayerAnswer>,
    onTick: () -> Unit,
    timerStarted: Boolean,
    onStartTimer: () -> Unit,
    onPauseTimer: () -> Unit,
    onSkipTimer: () -> Unit,
) {
//    Log.d(TAG,"MasterAnswerTimerScreen(playerAnswers: $playerAnswers)")

    HandleUnimplementedBackNavigation()

    LaunchedEffect(timerStarted) {
        if (timerStarted) {
            while (true) {
                delay(1.seconds)
                onTick()
            }
        }
    }
    val titleSuffix = if (timerStarted) "Timer running" else "Timer stopped"
    Scaffold(
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {

//            AnimatedContent(
//                targetState = timerStarted,
//                label = "",
//                transitionSpec = fabTransitionSpec
//            ) {
//                ExtendedFloatingActionButton(onClick = { if (it) onPauseTimer() else onStartTimer() }) {
//                        Text(text = if (timerStarted) "Pause Timer" else "Start Timer")
//                }
//            }
            if (playerAnswers.all { it.answered }) {
                ExtendedFloatingActionButton(onClick = onSkipTimer) {
                    Text(text = "Skip Timer")
                }
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    AnimatedContent(targetState = "$title: $titleSuffix", label = "") {
                        Text(text = it)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        //showCloseDialog = true
                    }) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Exit Waiting Screen")
                    }
                }
            )
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
                    Text(text = "${ticks}s / ${maxTicks}s", style = MaterialTheme.typography.headlineSmall)
                    // ExtendedFloatingActionButton(onClick = { if (it) onPauseTimer() else onStartTimer() }) {
                    //                        Text(text = if (timerStarted) "Pause Timer" else "Start Timer")
                    //                }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onStartTimer) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Start Timer")
                    }
                    IconButton(onClick = onPauseTimer) {
                        Icon(Icons.Filled.Add, contentDescription = "Pause Timer")
                    }
                }
            }
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
            ) {
                items(playerAnswers) { answer ->
                    ListItem(
                        headlineContent = {
                            Text(
                                text = answer.from,
                                style = MaterialTheme.typography.headlineSmall,
                            )
                        },
                        trailingContent = {
                            if (answer.answered) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Player answered"
                                )
                            }
                        },
                        leadingContent = { PlayerAvatar(nickname = answer.from, size = 40.dp) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
val fabTransitionSpec: AnimatedContentTransitionScope<Boolean>.() -> ContentTransform =
    {
        (scaleIn(
            transformOrigin = TransformOrigin(
                0.8f,
                0.8f
            )
        ) + expandIn(expandFrom = Alignment.TopStart, clip = false) with scaleOut(
            transformOrigin = TransformOrigin(0.8f, 0.8f)
        ) + shrinkOut(
            shrinkTowards = Alignment.TopStart,
            clip = false
        )) using SizeTransform(clip = false)
    }

@Preview
@Composable
fun MasterAnswerTimerScreenPreview() {
    var timerStarted by remember {
        mutableStateOf(false)
    }
    var ticks by remember {
        mutableStateOf(0)
    }
    val playerAnswers by remember {
        mutableStateOf(listOf(
            PlayerAnswer("Hans Peter"),
            PlayerAnswer("Manfred Emmerich", true)
        ))
    }
    val maxTicks = 15
    PubQuizTheme {
        MasterAnswerTimerScreen(
            title = "Question 1",
            timerStarted = timerStarted,
            onStartTimer = { timerStarted = true },
            onPauseTimer = { timerStarted = false },
            onSkipTimer = { ticks = maxTicks },
            ticks = ticks,
            onTick = {
                if (ticks == maxTicks) {
                    timerStarted = false
                    return@MasterAnswerTimerScreen
                }
                ticks++
//            if (ticks == 3) {
//                playerAnswers[0] = playerAnswers.copy {.answered = true}
//            }
//            if (ticks == 10) {
//                playerAnswers[1].answer = 4
//            }
            },
            maxTicks = maxTicks,
            playerAnswers = playerAnswers)
    }
}