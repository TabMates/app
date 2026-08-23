package de.tabmates.features.tabgroup.presentation.navigation.groupoverview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.vectorResource

/**
 * The leading badge on any row that stands for one entry or one schedule.
 *
 * Shared rather than private to the group screen: the schedules list shows the same objects one
 * level down, and a row that changed shape on the way there would read as a different kind of thing.
 */
@Composable
internal fun EntryIcon(
    icon: DrawableResource,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Box(
        modifier =
            Modifier
                .size(40.dp)
                .background(containerColor, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = vectorResource(icon),
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(20.dp),
        )
    }
}
