package de.tabmates.features.tabgroup.presentation.navigation.activity

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.tabmates.core.designsystem.spacer.HorizontalSpacer
import de.tabmates.core.designsystem.spacer.VerticalSpacer
import de.tabmates.features.tabgroup.presentation.navigation.addexpense.rememberMonthAbbreviations
import de.tabmates.features.tabgroup.presentation.navigation.groupoverview.UserAvatar
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.activity_action_added
import tabmatesapp.features.tabgroup.presentation.generated.resources.activity_action_created
import tabmatesapp.features.tabgroup.presentation.generated.resources.activity_action_deleted
import tabmatesapp.features.tabgroup.presentation.generated.resources.activity_action_edited
import tabmatesapp.features.tabgroup.presentation.generated.resources.activity_empty
import tabmatesapp.features.tabgroup.presentation.generated.resources.activity_settled_with
import tabmatesapp.features.tabgroup.presentation.generated.resources.activity_settled_with_you
import tabmatesapp.features.tabgroup.presentation.generated.resources.activity_title
import tabmatesapp.features.tabgroup.presentation.generated.resources.activity_today
import tabmatesapp.features.tabgroup.presentation.generated.resources.activity_yesterday
import tabmatesapp.features.tabgroup.presentation.generated.resources.activity_you
import kotlin.math.abs

@Composable
fun ActivityRoot(
    onGroupClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ActivityViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ActivityScreen(state = state, onGroupClick = onGroupClick, modifier = modifier)
}

@Composable
internal fun ActivityScreen(
    state: ActivityState,
    onGroupClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val monthLabels = rememberMonthAbbreviations()
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    ) {
        item {
            Text(
                text = stringResource(Res.string.activity_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
        if (state.sections.isEmpty() && !state.isLoading) {
            item {
                Text(
                    text = stringResource(Res.string.activity_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            }
        }
        state.sections.forEach { section ->
            item(key = "header-${section.bucket}") {
                SectionHeader(bucket = section.bucket, monthLabels = monthLabels)
            }
            items(section.items, key = { it.id }) { activityItem ->
                ActivityRow(item = activityItem, onClick = { onGroupClick(activityItem.groupId) })
            }
        }
    }
}

@Composable
private fun SectionHeader(
    bucket: ActivityBucket,
    monthLabels: List<String>,
) {
    val text =
        when (bucket) {
            ActivityBucket.Today -> {
                stringResource(Res.string.activity_today)
            }

            ActivityBucket.Yesterday -> {
                stringResource(Res.string.activity_yesterday)
            }

            is ActivityBucket.OnDate -> {
                "${monthLabels.getOrNull(bucket.monthIndex).orEmpty()} ${bucket.day}".uppercase()
            }
        }
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun ActivityRow(
    item: ActivityItem,
    onClick: () -> Unit,
) {
    val (container, content) = avatarColors(item.colorSeed, item.isDeleted)
    val dimmed = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UserAvatar(
            initials = item.initials,
            size = 40.dp,
            containerColor = container,
            contentColor = content,
        )
        HorizontalSpacer(12.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = activityText(item),
                style = MaterialTheme.typography.bodyLarge,
                color = if (item.isDeleted) dimmed else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (item.isDeleted) TextDecoration.LineThrough else null,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            VerticalSpacer(2.dp)
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = dimmed,
                textDecoration = if (item.isDeleted) TextDecoration.LineThrough else null,
            )
        }
    }
}

@Composable
private fun activityText(item: ActivityItem) =
    buildAnnotatedString {
        val actorLabel = if (item.actorIsYou) stringResource(Res.string.activity_you) else item.actor
        val bold = SpanStyle(fontWeight = FontWeight.SemiBold)
        withStyle(bold) { append(actorLabel) }
        append(" ")
        when (val kind = item.kind) {
            is ActivityKind.Added -> {
                append(stringResource(Res.string.activity_action_added))
                append(" ")
                withStyle(bold) { append(kind.target) }
            }

            is ActivityKind.Edited -> {
                append(stringResource(Res.string.activity_action_edited))
                append(" ")
                withStyle(bold) { append(kind.target) }
            }

            is ActivityKind.Deleted -> {
                append(stringResource(Res.string.activity_action_deleted))
                append(" ")
                withStyle(bold) { append(kind.target) }
            }

            is ActivityKind.Created -> {
                append(stringResource(Res.string.activity_action_created))
                append(" ")
                withStyle(bold) { append(kind.target) }
            }

            ActivityKind.SettledWithYou -> {
                append(stringResource(Res.string.activity_settled_with_you))
            }

            is ActivityKind.SettledWith -> {
                append(stringResource(Res.string.activity_settled_with))
                append(" ")
                withStyle(bold) { append(kind.other) }
            }
        }
    }

@Composable
private fun avatarColors(
    seed: String,
    isDeleted: Boolean,
): Pair<Color, Color> {
    val scheme = MaterialTheme.colorScheme
    if (isDeleted) return scheme.surfaceVariant to scheme.onSurfaceVariant
    if (seed == "self") return scheme.tertiaryContainer to scheme.onTertiaryContainer
    val palette =
        listOf(
            scheme.secondaryContainer to scheme.onSecondaryContainer,
            scheme.tertiaryContainer to scheme.onTertiaryContainer,
            scheme.primaryContainer to scheme.onPrimaryContainer,
        )
    return palette[abs(seed.hashCode()) % palette.size]
}
