package at.aau.appdev.g7.pubquiz.ui.screens.master

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasterSetup(
    onExit: () -> Unit,
    onCreate: (GameConfiguration) -> Unit,
    initialGameConfiguration: GameConfiguration = GameConfiguration("Game", 4, 4, 4, 120)
) {
    var numberOfRounds by remember {
        mutableStateOf(initialGameConfiguration.numberOfRounds.toFloat())
    }
    var numberOfQuestions by remember {
        mutableStateOf(initialGameConfiguration.numberOfRounds.toFloat())
    }
    var numberOfAnswers by remember {
        mutableStateOf(initialGameConfiguration.numberOfAnswers.toFloat())
    }
    var timePerQuestion by remember {
        mutableStateOf(initialGameConfiguration.timePerQuestion.toFloat())
    }
    var name by remember {
        mutableStateOf(initialGameConfiguration.name)
    }

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Text(text = "Setup Game")
            }, navigationIcon = {
                IconButton(onClick = onExit) {
                    Icon(imageVector = Icons.Filled.Close, contentDescription = "Go Back")
                }
            }, actions = {
                TextButton(onClick = {
                    onCreate(
                        GameConfiguration(
                            name = name,
                            numberOfRounds = numberOfRounds.roundToInt(),
                            numberOfQuestions = numberOfQuestions.roundToInt(),
                            numberOfAnswers = numberOfAnswers.roundToInt(),
                            timePerQuestion = timePerQuestion.roundToInt()
                        )
                    )
                }, enabled = name != "") {
                    Text(text = "Save")
                }
            })
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier.align(Alignment.Center)
            ) {

                OutlinedTextField(value = name,
                    onValueChange = { name = it },
                    isError = name == "",
                    modifier = Modifier
                        .padding(vertical = 16.dp, horizontal = 32.dp)
                        .fillMaxWidth(),
                    label = { Text(text = "Name") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(32.dp))

                MasterSetupSliderListItem(
                    headlineText = "Number of Rounds",
                    value = numberOfRounds,
                    onValueChange = { numberOfRounds = it },
                    valueRange = 0f..5f,
                    steps = 4,
                    valueText = "${numberOfRounds.roundToInt()}",
                    modifier = Modifier.padding(vertical = 16.dp, horizontal = 32.dp)
                )

                Divider()

                MasterSetupSliderListItem(
                    headlineText = "Questions per Round",
                    value = numberOfQuestions,
                    onValueChange = { numberOfQuestions = it },
                    valueRange = 0f..5f,
                    steps = 4,
                    valueText = "${numberOfQuestions.roundToInt()}",
                    modifier = Modifier.padding(vertical = 16.dp, horizontal = 32.dp)
                )

                Divider()

                MasterSetupSliderListItem(
                    headlineText = "Answers per Question",
                    value = numberOfAnswers,
                    onValueChange = { numberOfAnswers = it },
                    valueRange = 0f..5f,
                    steps = 4,
                    valueText = "${numberOfAnswers.roundToInt()}",
                    modifier = Modifier.padding(vertical = 16.dp, horizontal = 32.dp)
                )

                Divider()

                MasterSetupSliderListItem(
                    headlineText = "Time per Question",
                    value = timePerQuestion,
                    onValueChange = { timePerQuestion = it },
                    valueText = "${timePerQuestion.roundToInt()}s",
                    valueRange = 5f..120f,
                    modifier = Modifier.padding(vertical = 16.dp, horizontal = 32.dp)
                )

            }
        }

    }
}

@Composable
fun MasterSetupSliderListItem(
    headlineText: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    valueText: String,
    modifier: Modifier
) {
    Column(modifier) {
        Row(
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .padding(top = 10.dp)
        ) {
            Text(
                text = headlineText,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
        )
    }
}