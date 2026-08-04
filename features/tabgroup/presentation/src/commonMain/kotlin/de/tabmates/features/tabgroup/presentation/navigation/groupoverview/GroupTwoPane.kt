package de.tabmates.features.tabgroup.presentation.navigation.groupoverview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.tabmates.core.designsystem.spacer.VerticalSpacer
import org.jetbrains.compose.resources.stringResource
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_placeholder_caption
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_placeholder_title

/**
 * Wide-window layout for the Groups tab: the group list on the left, whatever the back stack has
 * pushed on top of it on the right.
 *
 * The panes are supplied as slots because their content is nav-entry content owned by the
 * `SceneStrategy` in `:composeApp` — this module keeps the geometry and the empty state, the
 * strategy keeps the back-stack reading.
 */
@Composable
fun GroupTwoPane(
    listPane: @Composable () -> Unit,
    detailPane: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxSize()) {
        Box(
            modifier =
                Modifier
                    .widthIn(min = ListPaneMinWidth, max = ListPaneMaxWidth)
                    .fillMaxHeight(),
        ) {
            listPane()
        }
        VerticalDivider(
            modifier = Modifier.fillMaxHeight(),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            if (detailPane == null) {
                DetailPlaceholder(modifier = Modifier.fillMaxSize())
            } else {
                detailPane()
            }
        }
    }
}

@Composable
private fun DetailPlaceholder(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(Res.string.groups_detail_placeholder_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        VerticalSpacer(8.dp)
        Text(
            text = stringResource(Res.string.groups_detail_placeholder_caption),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private val ListPaneMinWidth = 280.dp
private val ListPaneMaxWidth = 360.dp
