package at.aau.appdev.g7.pubquiz.ui.screens.master

import android.os.Parcelable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.aau.appdev.g7.pubquiz.domain.PlayerRoundScoreRecord
import at.aau.appdev.g7.pubquiz.ui.components.HandleUnimplementedBackNavigation
import at.aau.appdev.g7.pubquiz.ui.theme.PubQuizTheme
import kotlinx.parcelize.Parcelize



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasterRoundEndScreen(
    roundName: String,
    playerAnswers: List<PlayerRoundScoreRecord>,
    onContinue: () -> Unit,
) {
    HandleUnimplementedBackNavigation()
    val selectedCardColors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
    )

    Scaffold(
            floatingActionButtonPosition = FabPosition.End,
            floatingActionButton = {
                ExtendedFloatingActionButton(onClick = onContinue) {
                    Text(text = "Continue")
                }
            },
        topBar = {
            TopAppBar(title = { Text(text = roundName) })
        }
    ) { paddingValues ->
        LazyColumn(
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier.padding(paddingValues)
        ) {
//            items(roundNames.size) {
//                val name = roundNames[it]
//                Card(
//                        modifier = Modifier.padding(vertical = 10.dp),
//                        colors = if (name == nextRoundName) selectedCardColors else CardDefaults.cardColors()
//                )
//                {
//                    Row(
//                            horizontalArrangement = Arrangement.Center,
//                            modifier = Modifier
//                                    .fillMaxWidth()
//                                    .padding(32.dp)
//                    ) {
//                        Text(text = name, style = MaterialTheme.typography.headlineSmall)
//                    }
//                }
//            }
        }
    }
}

