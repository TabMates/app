package de.tabmates.core.designsystem.text

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight

/** Small uppercase-style section header (e.g. "ACCOUNT", "RECENT") used across settings/detail screens. */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight? = null,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = fontWeight,
        modifier = modifier,
    )
}
