package at.aau.appdev.g7.pubquiz.ui.screens.player

import android.widget.ToggleButton
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconToggleButton
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
import androidx.compose.ui.unit.sp
import at.aau.appdev.g7.pubquiz.ui.components.AnswerButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerQuestions(
    questionText: String,
    answers: List<String>,
    selectedAnswer: String,
    onAnswer: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(questionText) })
        }
    ) { paddingValues ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(40.dp, Alignment.CenterVertically)
        ) {
            for (answer in answers) {
                AnswerButton(
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .heightIn(min = 96.dp),
                    text = answer,
                    fontSize = 64.sp,
                    selected = answer == selectedAnswer,
                    onSelectionChanged = onAnswer)
            }
        }
    }
}

@Preview
@Composable
fun PlayerQuestionsPreview() {
    PlayerQuestions(
        questionText = "Question 1",
        answers = listOf("A", "B", "C", "D"),
        "B",
        onAnswer = {}
    )
}