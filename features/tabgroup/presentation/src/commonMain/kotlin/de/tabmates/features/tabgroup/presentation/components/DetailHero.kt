package de.tabmates.features.tabgroup.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.vectorResource

/**
 * The masthead every "one thing, in detail" screen opens with: a big round badge, the title, the
 * amount, and one muted line under it.
 *
 * Shared rather than copied. Entries, settlements and schedules are three different objects, but a
 * member reads all three the same way, and three private copies of this block drifted apart is
 * exactly how a screen ends up looking like it came from somewhere else.
 *
 * @param subtitle the one muted line: a date for an entry or settlement, a cadence for a schedule
 * @param description optional free text under the subtitle, centred; blank hides it
 */
@Composable
fun DetailHero(
    icon: DrawableResource,
    title: String,
    amountFormatted: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    description: String = "",
    isPendingSync: Boolean = false,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(96.dp)
                    .background(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = CircleShape,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = vectorResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(40.dp),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        if (isPendingSync) {
            SyncStatusChip()
        }
        Text(
            text = amountFormatted,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (description.isNotBlank()) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
        }
    }
}
