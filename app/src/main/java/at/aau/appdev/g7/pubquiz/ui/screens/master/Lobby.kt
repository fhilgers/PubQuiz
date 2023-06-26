package at.aau.appdev.g7.pubquiz.ui.screens.master

import android.os.Parcelable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import at.aau.appdev.g7.pubquiz.ui.components.PlayerAvatar
import kotlinx.parcelize.Parcelize

@Parcelize
data class Player(val name: String, val ready: Boolean) : Parcelable

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MasterLobby(
    players: List<Player>,
    password: String,
    onClose: () -> Unit,
    onStart: () -> Unit,
) {
    var showCloseDialog by remember {
        mutableStateOf(false)
    }

    var showStartDialog by remember {
        mutableStateOf(false)
    }

    //TODO
    BackHandler {
        showCloseDialog = !showCloseDialog
    }

    if (showCloseDialog) {
        AlertDialog(
            onDismissRequest = {
                showCloseDialog = false
            },
            icon = { Icon(Icons.Filled.Warning, contentDescription = null) },
            title = {
                Text(text = "Close lobby?")
            },
            text = {
                Text(text = "Closing lobby will forcefully kick all joined players.")
            },
            confirmButton = {
                TextButton(onClick = onClose) {
                    Text("Close")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCloseDialog = false }) {
                    Text("Dismiss")
                }
            }
        )
    }

    if (showStartDialog) {
        AlertDialog(
            onDismissRequest = {
                showStartDialog = false
            },
            icon = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
            title = {
                Text(text = "Start game?")
            },
            text = {
                LazyColumn {
                    item {
                        Text(text = "This will start the game even if some players are not ready yet. The following players are not ready:")
                        Divider(Modifier.padding(vertical = 10.dp))
                    }

                    items(players.filter { !it.ready }) { player ->
                        PlayerListItem(
                            name = player.name,
                            ready = player.ready,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onStart) {
                    Text("Start")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDialog = false }) {
                    Text("Dismiss")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = "Game Lobby") }, navigationIcon = {
                IconButton(onClick = {
                    showCloseDialog = true
                }) {
                    Icon(imageVector = Icons.Filled.Close, contentDescription = "Exit Lobby")
                }
            })
        },
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showStartDialog = true },
            ) {
                Text(text = "Start Game")
            }
        },
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            Card(
                Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.CenterHorizontally),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "Game Password")
                    Text(text = password, style = MaterialTheme.typography.displayMedium)
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(players) { player ->
                    PlayerListItem(
                        name = player.name,
                        ready = player.ready,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerListItem(
    name: String,
    ready: Boolean,
    style: TextStyle,
    avatarSize: Dp = 40.dp,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = {
            Text(
                text = name,
                style = style,
            )
        },
        trailingContent = {
            if (ready) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Player Ready"
                )
            }
        },
        leadingContent = { PlayerAvatar(nickname = name, size = avatarSize) },
        modifier = modifier
    )
}


@Preview
@Composable
fun LobbyPreview() {
    val players = listOf(Player("Hans Mueller", false), Player("Manfred Emmerich", true))
    MaterialTheme {
        MasterLobby(
            players = players,
            password = "456048",
            onClose = {},
            onStart = {}
        )
    }
}
