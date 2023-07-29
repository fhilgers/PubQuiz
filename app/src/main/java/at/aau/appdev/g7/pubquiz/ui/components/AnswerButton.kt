package at.aau.appdev.g7.pubquiz.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconToggleButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

@Composable
fun AnswerButton(
    modifier: Modifier = Modifier,
    fontSize: TextUnit = TextUnit.Unspecified,
    text: String,
    selected: Boolean = false,
    onSelectionChanged: (String) -> Unit = {}
) {
    val outline = if (selected) { BorderStroke(4.dp, MaterialTheme.colorScheme.onPrimaryContainer) }
        else { IconButtonDefaults.outlinedIconButtonBorder(true) }
    OutlinedIconToggleButton(
        modifier = modifier,
        colors = IconButtonDefaults.outlinedIconToggleButtonColors(
            checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            checkedContainerColor = MaterialTheme.colorScheme.primaryContainer),
        border = outline,
        checked = selected,
        onCheckedChange = {
            onSelectionChanged(text)
        }) {
        Text(
            fontSize = fontSize,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            text = text
        )
    }

}