package at.aau.appdev.g7.pubquiz.ui.screens.master

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.aau.appdev.g7.pubquiz.ui.components.HandleUnimplementedBackNavigation
import at.aau.appdev.g7.pubquiz.ui.theme.PubQuizTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun MasterAnswersScreen(
    //numberOfAnswers: Int,
    answers: List<String>,
    onRightAnswerSelect: (String) -> Unit,
) {
    var checkedAnswer by remember {
        mutableStateOf("")
    }

    HandleUnimplementedBackNavigation()

    Scaffold(
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            AnimatedVisibility(visible = checkedAnswer != "", enter = scaleIn(), exit = scaleOut()) {
                ExtendedFloatingActionButton(onClick = { onRightAnswerSelect(checkedAnswer) }) {
                    Text(text = "Select Question")
                }
            }
        },
        topBar = {
            TopAppBar(title = { Text(text = "Select Correct Answer") }, navigationIcon = {
                IconButton(onClick = {
                    //showCloseDialog = true
                }) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Exit Answer Screen"
                    )
                }
            })
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
            ) {
                items(answers.size) { index ->
                    val answer = answers[index]
                    Card(
                        modifier = Modifier.padding(vertical = 10.dp),
                        onClick = { checkedAnswer = answer },
                    )
                    {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp)
                        ) {
                            Text(
                                text = answer,
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Checkbox(
                                checked = checkedAnswer == answer,
                                onCheckedChange = { },
                                enabled = false
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun MasterAnswersScreenPreview() {
    PubQuizTheme {
        MasterAnswersScreen(listOf("A", "B", "C", "D"), onRightAnswerSelect = {})
    }
}