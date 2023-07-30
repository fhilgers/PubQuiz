package at.aau.appdev.g7.pubquiz.ui.screens.master

import android.os.Parcel
import android.os.Parcelable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.aau.appdev.g7.pubquiz.domain.GameConfiguration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasterStart(
    gameConfigurations: List<GameConfiguration>,
    onHost: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    onEdit: (Int) -> Unit,
    onSetupGame: () -> Unit,
) {
    var selected by remember {
        mutableStateOf<Int?>(null)
    }

    Scaffold(
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onSetupGame) {
                Text(text = "Setup Game")
            }
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .padding(vertical = 8.dp)
        ) {
            itemsIndexed(gameConfigurations) { index, config ->
                Card(
                    onClick = {
                        if (index == selected) {
                            selected = null
                        } else {
                            selected = index
                        }

                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        )
                        {
                            Text(
                                text = config.name,
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.weight(1f)
                            )

                            Column {
                                Text(
                                    text = "${config.numberOfRounds} Rounds per Game",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    text = "${config.numberOfQuestions} Questions per Round",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    text = "${config.numberOfAnswers} Answers per Question",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    text = "${config.timePerQuestion} Seconds per Question",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }

                        AnimatedVisibility(
                            index == selected,
                            enter = expandVertically(
                                expandFrom = Alignment.Top,
                                animationSpec = tween(durationMillis = 100, easing = LinearEasing)
                            ),
                            exit = shrinkVertically(
                                shrinkTowards = Alignment.Top,
                                animationSpec = tween(durationMillis = 100, easing = LinearEasing)
                            )
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                ) {
                                    Button(
                                        onClick = { onDelete(index) },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Text(text = "Delete")
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    ElevatedButton(
                                        onClick = { onEdit(index) },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                    ) {
                                        Text(text = "Edit")
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    ElevatedButton(
                                        onClick = { onHost(index) },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Text(text = "Host")
                                    }
                                }
                            }
                        }


                    }
                }
            }
        }
    }
}