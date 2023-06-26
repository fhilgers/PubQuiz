package at.aau.appdev.g7.pubquiz.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun BackAlertDialog(
    onDimissRequest: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDimissRequest,
        icon = { Icon(Icons.Filled.Warning, contentDescription = null) },
        title = {
            Text(text = "Cancel?")
        },
        text = {
            Text(text = "Cancelling gracefully is not implemented yet...")
        },
        confirmButton = {
        },
        dismissButton = {
            TextButton(onClick = onDimissRequest) {
                Text("Dismiss")
            }
        }
    )
}

@Composable
fun HandleUnimplementedBackNavigation() {

    var showHandlerDialog by remember {
        mutableStateOf(false)
    }

    //TODO
    BackHandler {
        showHandlerDialog = !showHandlerDialog
    }

    if (showHandlerDialog) {
     BackAlertDialog {
         showHandlerDialog = false
     }
    }
}