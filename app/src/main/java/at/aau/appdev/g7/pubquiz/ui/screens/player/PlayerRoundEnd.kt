package at.aau.appdev.g7.pubquiz.ui.screens.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedIconToggleButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.aau.appdev.g7.pubquiz.domain.Question
import at.aau.appdev.g7.pubquiz.ui.components.AnswerButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerRoundEnd(
    roundName: String,
    submitted: Boolean,
    questions: List<Question>,
    selectedAnswers: List<String>,
    onAnswerChanged: (Int, String) -> Unit,
    onSubmit: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(roundName) })
        },
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            if (!submitted) {
                ExtendedFloatingActionButton(onClick = onSubmit) {
                    Text(text = "Submit Round")
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(40.dp, Alignment.CenterVertically)
        ) {
            Text("$roundName is about to end...")
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp),
                color = Color.Red,
                //trackColor = Color.Yellow
            )
            if (submitted) {
                Text("You have submitted the following answers:")
            } else {
                Text("Please verify and submit your answers:")
            }

            for ((i, question) in questions.withIndex()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${question.text}:")
                    if (submitted) {
                        AnswerButton(
                            text = selectedAnswers[i],
                            selected = true)
                    } else {
                        for (answer in question.answers) {
                            AnswerButton(
                                text = answer,
                                selected = selectedAnswers[i] == answer,
                                onSelectionChanged = {
                                    onAnswerChanged(i, answer)
                                })
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun PlayerRoundEndPreview() {
    PlayerRoundEnd(
        roundName = "Round 1",
        questions = (1..5).map {
            Question(it, "Question $it", listOf("A", "B", "C", "D")) },
        selectedAnswers = listOf("A", "B", "C", "D", "A"),
        onAnswerChanged = { _, _ -> },
        onSubmit = {},
        submitted = false
    )
}