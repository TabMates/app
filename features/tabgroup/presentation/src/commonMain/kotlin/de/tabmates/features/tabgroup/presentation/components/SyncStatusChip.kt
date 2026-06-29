package de.tabmates.features.tabgroup.presentation.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.sync_status_pending_badge
import tabmatesapp.features.tabgroup.presentation.generated.resources.sync_status_pending_cd

/**
 * Compact pill shown next to an expense title (or a group title) while the entry is a local
 * optimistic write not yet confirmed by the server. Mirrors the owner/pending member badge style,
 * but uses a distinct label ("Not synced") and a tertiary tone to avoid clashing with the
 * invited-not-claimed member "Pending" chip.
 */
@Composable
fun SyncStatusChip(modifier: Modifier = Modifier) {
    val description = stringResource(Res.string.sync_status_pending_cd)
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        modifier = modifier.semantics { contentDescription = description },
    ) {
        Text(
            text = stringResource(Res.string.sync_status_pending_badge),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}
