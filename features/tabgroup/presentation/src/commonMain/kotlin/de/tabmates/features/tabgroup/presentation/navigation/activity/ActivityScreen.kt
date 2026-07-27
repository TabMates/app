package de.tabmates.features.tabgroup.presentation.navigation.activity

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
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
import de.tabmates.features.tabgroup.domain.activity.ActivityField
import de.tabmates.features.tabgroup.presentation.components.SyncStatusChip
import de.tabmates.features.tabgroup.presentation.navigation.addentry.rememberMonthAbbreviations
import de.tabmates.features.tabgroup.presentation.navigation.groupoverview.UserAvatar
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.activity_action_added
import tabmatesapp.features.tabgroup.presentation.generated.resources.activity_action_created
import tabmatesapp.features.tabgroup.presentation.generated.resources.activity_action_deleted
import tabmatesapp.features.tabgroup.presentation.generated.resources.activity_action_edited
import tabmatesapp.features.tabgroup.presentation.generated.resources.activity_action_group_updated
import tabmatesapp.features.tabgroup.presentation.generated.resources.activity_action_member_joined
import tabmatesapp.features.tabgroup.presentation.generated.resources.activity_action_member_left
import tabmatesapp.features.tabgroup.presentation.generated.resources.activity_action_member_removed
import tabmatesapp.features.tabgroup.presentation.generated.resources.activity_diff_splits_changed
import tabmatesapp.features.tabgroup.presentation.generated.resources.activity_empty
import tabmatesapp.features.tabgroup.presentation.generated.resources.activity_field_amount
import tabmatesapp.features.tabgroup.presentation.generated.resources.activity_field_currency
import tabmatesapp.features.tabgroup.presentation.generated.resources.activity_field_default_currency
import tabmatesapp.features.tabgroup.presentation.generated.resources.activity_field_description
import tabmatesapp.features.tabgroup.presentation.generated.resources.activity_field_entry_date
import tabmatesapp.features.tabgroup.presentation.generated.resources.activity_field_exchange_rate
import tabmatesapp.features.tabgroup.presentation.generated.resources.activity_field_paid_by
import tabmatesapp.features.tabgroup.presentation.generated.resources.activity_field_received_by
import tabmatesapp.features.tabgroup.presentation.generated.resources.activity_field_splits
import tabmatesapp.features.tabgroup.presentation.generated.resources.activity_field_title
import tabmatesapp.features.tabgroup.presentation.generated.resources.activity_title
import tabmatesapp.features.tabgroup.presentation.generated.resources.activity_today
import tabmatesapp.features.tabgroup.presentation.generated.resources.activity_yesterday
import tabmatesapp.features.tabgroup.presentation.generated.resources.activity_you
import kotlin.math.abs

@Composable
fun ActivityRoot(
    onGroupClick: (String) -> Unit,
    onEntryClick: (groupId: String, entryId: String) -> Unit,
    onSettlementClick: (groupId: String, settlementId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ActivityViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ActivityScreen(
        state = state,
        onGroupClick = onGroupClick,
        onEntryClick = onEntryClick,
        onSettlementClick = onSettlementClick,
        onLoadMore = viewModel::loadMore,
        modifier = modifier,
    )
}

@Composable
internal fun ActivityScreen(
    state: ActivityState,
    onGroupClick: (String) -> Unit,
    onEntryClick: (groupId: String, entryId: String) -> Unit,
    onSettlementClick: (groupId: String, settlementId: String) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val monthLabels = rememberMonthAbbreviations()
    val listState = rememberLazyListState()
    LoadMoreOnApproachingEnd(listState, state.canLoadMore, onLoadMore)
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        item {
            Text(
                text = stringResource(Res.string.activity_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        activityFeed(
            sections = state.sections,
            isLoading = state.isLoading,
            monthLabels = monthLabels,
            onEntryClick = onEntryClick,
            onSettlementClick = onSettlementClick,
            onGroupClick = onGroupClick,
        )
    }
}

/**
 * The feed body, shared with the group History tab so both render the same rows from the same state.
 */
internal fun LazyListScope.activityFeed(
    sections: List<ActivitySection>,
    isLoading: Boolean,
    monthLabels: List<String>,
    onEntryClick: (groupId: String, entryId: String) -> Unit,
    onSettlementClick: (groupId: String, settlementId: String) -> Unit,
    /** Null inside a group's own History tab, where navigating to that same group is pointless. */
    onGroupClick: ((String) -> Unit)? = null,
) {
    if (sections.isEmpty() && !isLoading) {
        item {
            Text(
                text = stringResource(Res.string.activity_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
            )
        }
    }
    sections.forEach { section ->
        item(key = "header-${section.bucket}") {
            SectionHeader(bucket = section.bucket, monthLabels = monthLabels)
        }
        items(section.items, key = { it.id }) { activityItem ->
            val onClick: (() -> Unit)? =
                when (val target = activityItem.clickTarget) {
                    is ActivityClickTarget.Entry -> {
                        {
                            if (target.isSettlement) {
                                onSettlementClick(target.groupId, target.tabEntryId)
                            } else {
                                onEntryClick(target.groupId, target.tabEntryId)
                            }
                        }
                    }

                    is ActivityClickTarget.Group -> {
                        onGroupClick?.let { click -> { click(target.groupId) } }
                    }

                    ActivityClickTarget.None -> {
                        null
                    }
                }
            ActivityRow(item = activityItem, onClick = onClick)
        }
    }
}

/** Fetches the next page once the user scrolls within [LOAD_MORE_THRESHOLD] rows of the end. */
@Composable
internal fun LoadMoreOnApproachingEnd(
    listState: LazyListState,
    canLoadMore: Boolean,
    onLoadMore: () -> Unit,
) {
    val currentOnLoadMore by rememberUpdatedState(onLoadMore)
    val currentCanLoadMore by rememberUpdatedState(canLoadMore)
    LaunchedEffect(listState) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo
                .lastOrNull()
                ?.index
        }.filterNotNull()
            .distinctUntilChanged()
            .collect { lastVisible ->
                val total = listState.layoutInfo.totalItemsCount
                if (currentCanLoadMore && total > 0 && lastVisible >= total - LOAD_MORE_THRESHOLD) {
                    currentOnLoadMore()
                }
            }
    }
}

private const val LOAD_MORE_THRESHOLD = 5

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
        modifier =
            Modifier
                .padding(top = 20.dp, bottom = 8.dp)
                .padding(horizontal = 16.dp),
    )
}

@Composable
private fun ActivityRow(
    item: ActivityItem,
    onClick: (() -> Unit)?,
) {
    val (container, content) = avatarColors(item.colorSeed, item.isDeleted)
    val dimmed = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top,
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = dimmed,
                    textDecoration = if (item.isDeleted) TextDecoration.LineThrough else null,
                )
                if (item.isPending) {
                    HorizontalSpacer(8.dp)
                    SyncStatusChip()
                }
            }
            item.diffs.forEach { diff ->
                VerticalSpacer(2.dp)
                Text(
                    text = diffText(diff),
                    style = MaterialTheme.typography.bodySmall,
                    color = dimmed,
                )
            }
        }
    }
}

@Composable
private fun activityText(item: ActivityItem) =
    buildAnnotatedString {
        val actorLabel = if (item.actorIsYou) stringResource(Res.string.activity_you) else item.actor
        val bold = SpanStyle(fontWeight = FontWeight.SemiBold)
        withStyle(bold) { append(actorLabel) }
        when (val kind = item.kind) {
            is ActivityKind.EntryAdded -> {
                appendAction(stringResource(Res.string.activity_action_added), kind.target, bold)
            }

            is ActivityKind.EntryEdited -> {
                appendAction(stringResource(Res.string.activity_action_edited), kind.target, bold)
            }

            is ActivityKind.EntryDeleted -> {
                appendAction(stringResource(Res.string.activity_action_deleted), kind.target, bold)
            }

            is ActivityKind.GroupCreated -> {
                appendAction(stringResource(Res.string.activity_action_created), kind.target, bold)
            }

            is ActivityKind.GroupUpdated -> {
                appendAction(stringResource(Res.string.activity_action_group_updated), kind.target, bold)
            }

            is ActivityKind.MemberJoined -> {
                appendAction(stringResource(Res.string.activity_action_member_joined), kind.member, bold)
            }

            is ActivityKind.MemberLeft -> {
                appendAction(stringResource(Res.string.activity_action_member_left), kind.member, bold)
            }

            is ActivityKind.MemberRemoved -> {
                appendAction(stringResource(Res.string.activity_action_member_removed), kind.member, bold)
            }

            // An event type this build does not know: the actor line alone still says who did it.
            ActivityKind.Unknown -> {}
        }
    }

private fun AnnotatedString.Builder.appendAction(
    action: String,
    target: String,
    bold: SpanStyle,
) {
    append(" ")
    append(action)
    if (target.isNotBlank()) {
        append(" ")
        withStyle(bold) { append(target) }
    }
}

@Composable
private fun diffText(diff: ActivityDiff): String {
    if (diff.field == ActivityField.SPLITS) return stringResource(Res.string.activity_diff_splits_changed)
    val label = diff.field.label()
    val old = diff.oldValue
    val new = diff.newValue
    return when {
        old != null && new != null -> "$label: $old → $new"
        new != null -> "$label: $new"
        old != null -> "$label: $old →"
        else -> label
    }
}

@Composable
private fun ActivityField.label(): String =
    when (this) {
        ActivityField.TITLE -> stringResource(Res.string.activity_field_title)

        ActivityField.DESCRIPTION -> stringResource(Res.string.activity_field_description)

        ActivityField.AMOUNT -> stringResource(Res.string.activity_field_amount)

        ActivityField.CURRENCY -> stringResource(Res.string.activity_field_currency)

        ActivityField.EXCHANGE_RATE -> stringResource(Res.string.activity_field_exchange_rate)

        ActivityField.ENTRY_DATE -> stringResource(Res.string.activity_field_entry_date)

        ActivityField.PAID_BY -> stringResource(Res.string.activity_field_paid_by)

        ActivityField.RECEIVED_BY -> stringResource(Res.string.activity_field_received_by)

        ActivityField.SPLITS -> stringResource(Res.string.activity_field_splits)

        ActivityField.DEFAULT_CURRENCY -> stringResource(Res.string.activity_field_default_currency)

        // A field this build does not know still shows its values, just without a name.
        ActivityField.UNKNOWN -> ""
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
