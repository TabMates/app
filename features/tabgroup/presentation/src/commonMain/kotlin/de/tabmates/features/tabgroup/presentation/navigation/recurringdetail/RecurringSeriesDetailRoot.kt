package de.tabmates.features.tabgroup.presentation.navigation.recurringdetail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import de.tabmates.core.designsystem.banner.StatusBanner
import de.tabmates.core.designsystem.banner.StatusBannerTone
import de.tabmates.core.designsystem.spacer.VerticalSpacer
import de.tabmates.core.designsystem.text.SectionLabel
import de.tabmates.core.presentation.navigation.TopBarActions
import de.tabmates.core.presentation.util.ObserveAsEvents
import de.tabmates.features.tabgroup.presentation.components.DetailHero
import de.tabmates.features.tabgroup.presentation.components.formatMoney
import de.tabmates.features.tabgroup.presentation.navigation.addentry.formatEntryDate
import de.tabmates.features.tabgroup.presentation.navigation.addentry.rememberMonthAbbreviations
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_calendar
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_close
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_delete
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_edit
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_refresh
import tabmatesapp.features.tabgroup.presentation.generated.resources.recurring_detail_created_by
import tabmatesapp.features.tabgroup.presentation.generated.resources.recurring_detail_edit_cd
import tabmatesapp.features.tabgroup.presentation.generated.resources.recurring_detail_end_cd
import tabmatesapp.features.tabgroup.presentation.generated.resources.recurring_detail_end_dialog_cancel
import tabmatesapp.features.tabgroup.presentation.generated.resources.recurring_detail_end_dialog_confirm
import tabmatesapp.features.tabgroup.presentation.generated.resources.recurring_detail_end_dialog_message
import tabmatesapp.features.tabgroup.presentation.generated.resources.recurring_detail_end_dialog_title
import tabmatesapp.features.tabgroup.presentation.generated.resources.recurring_detail_ended_note
import tabmatesapp.features.tabgroup.presentation.generated.resources.recurring_detail_needs_attention_message
import tabmatesapp.features.tabgroup.presentation.generated.resources.recurring_detail_needs_attention_title
import tabmatesapp.features.tabgroup.presentation.generated.resources.recurring_detail_offline_note
import tabmatesapp.features.tabgroup.presentation.generated.resources.recurring_detail_skip_cd
import tabmatesapp.features.tabgroup.presentation.generated.resources.recurring_detail_skip_note
import tabmatesapp.features.tabgroup.presentation.generated.resources.recurring_detail_skipped_section
import tabmatesapp.features.tabgroup.presentation.generated.resources.recurring_detail_unavailable
import tabmatesapp.features.tabgroup.presentation.generated.resources.recurring_detail_unskip_cd
import tabmatesapp.features.tabgroup.presentation.generated.resources.recurring_detail_upcoming_section

/**
 * Read-only detail for one schedule, mirroring the entry detail screen: the template on top, the
 * actions in the top bar, and nothing editable inline.
 */
@Composable
fun RecurringSeriesDetailRoot(
    groupId: String,
    seriesId: String,
    navKey: NavKey,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecurringSeriesDetailViewModel =
        koinViewModel(key = seriesId, parameters = { parametersOf(groupId, seriesId) }),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            RecurringSeriesDetailEvent.SeriesEnded -> {
                onBack()
            }

            RecurringSeriesDetailEvent.SeriesUnavailable -> {
                onBack()
            }

            is RecurringSeriesDetailEvent.Error -> {
                snackbarHostState.showSnackbar(event.message.asStringAsync())
            }
        }
    }

    // A series that never arrives is a series that is gone — deleted with its group, or never
    // synced here. Leaving the screen on an empty page would look like a hang.
    LaunchedEffect(state.isLoading, state.series) {
        if (!state.isLoading && state.series == null) viewModel.onMissingSeries()
    }

    // Same two actions, in the same place, as an entry's detail screen. Before this they lived
    // nowhere: editing was reachable only by tapping the banner a *broken* schedule shows, so a
    // healthy one could not be edited at all.
    TopBarActions(navKey) {
        if (state.canEdit) {
            IconButton(onClick = { onEdit(seriesId) }) {
                Icon(
                    imageVector = vectorResource(Res.drawable.ic_edit),
                    contentDescription = stringResource(Res.string.recurring_detail_edit_cd),
                )
            }
        }
        if (state.canEnd) {
            IconButton(onClick = viewModel::onEndClick) {
                Icon(
                    imageVector = vectorResource(Res.drawable.ic_delete),
                    contentDescription = stringResource(Res.string.recurring_detail_end_cd),
                )
            }
        }
    }

    RecurringSeriesDetailScreen(
        state = state,
        onSkip = viewModel::onSkipOccurrence,
        onUnskip = viewModel::onUnskipOccurrence,
        onEndDismiss = viewModel::onEndDismiss,
        onEndConfirm = viewModel::onEndConfirm,
        onEdit = { onEdit(seriesId) },
        modifier = modifier,
    )
}

@Composable
private fun RecurringSeriesDetailScreen(
    state: RecurringSeriesDetailState,
    onSkip: (LocalDate) -> Unit,
    onUnskip: (LocalDate) -> Unit,
    onEndDismiss: () -> Unit,
    onEndConfirm: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val series = state.series
    if (state.isLoading || series == null) {
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (state.isLoading) {
                VerticalSpacer(48.dp)
                CircularProgressIndicator()
            } else {
                VerticalSpacer(48.dp)
                Text(
                    text = stringResource(Res.string.recurring_detail_unavailable),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        return
    }

    val monthLabels = rememberMonthAbbreviations()

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
    ) {
        // Both notices sit above the masthead: they change what the rest of the screen means, so
        // reading them after the amount would be reading them too late.
        if (state.needsAttention) {
            StatusBanner(
                text =
                    stringResource(Res.string.recurring_detail_needs_attention_title) +
                        " · " +
                        stringResource(
                            Res.string.recurring_detail_needs_attention_message,
                            state.departedParticipants.joinToString { it.username },
                        ),
                tone = StatusBannerTone.Attention,
                // A second, louder way to the edit form than the pencil in the top bar.
                onClick = onEdit.takeIf { state.canEdit },
                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 12.dp),
            )
        }
        if (!state.isActive) {
            Text(
                text = stringResource(Res.string.recurring_detail_ended_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 12.dp),
            )
        }

        VerticalSpacer(8.dp)
        // The cadence takes the slot an entry's date takes, because it is the same answer to the
        // same question: when does this hit the ledger?
        DetailHero(
            icon = Res.drawable.ic_calendar,
            title = series.rule.title,
            amountFormatted =
                formatMoney(state.currencySymbol, series.rule.amount, state.currencyDecimalDigits),
            subtitle = scheduleSummary(series, monthLabels),
            description = series.rule.description,
        )
        VerticalSpacer(8.dp)
        Text(
            text = stringResource(Res.string.recurring_detail_created_by, series.createdBy.username),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            textAlign = TextAlign.Center,
        )

        VerticalSpacer(24.dp)
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            if (state.upcomingOccurrences.isNotEmpty()) {
                SectionLabel(
                    text = stringResource(Res.string.recurring_detail_upcoming_section),
                    fontWeight = FontWeight.SemiBold,
                )
                VerticalSpacer(8.dp)
                state.upcomingOccurrences.forEach { date ->
                    val label = formatEntryDate(date, monthLabels)
                    OccurrenceRow(
                        label = label,
                        actionIcon = Res.drawable.ic_close,
                        actionDescription = stringResource(Res.string.recurring_detail_skip_cd, label),
                        // Only a future, not-yet-created occurrence can be skipped; a created one is
                        // an ordinary entry and is deleted from the entry itself.
                        actionEnabled = state.canEdit,
                        onAction = { onSkip(date) },
                    )
                }
                Text(
                    text = stringResource(Res.string.recurring_detail_skip_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            if (state.skippedUpcoming.isNotEmpty()) {
                VerticalSpacer(20.dp)
                SectionLabel(
                    text = stringResource(Res.string.recurring_detail_skipped_section),
                    fontWeight = FontWeight.SemiBold,
                )
                VerticalSpacer(8.dp)
                state.skippedUpcoming.forEach { date ->
                    val label = formatEntryDate(date, monthLabels)
                    OccurrenceRow(
                        label = label,
                        actionIcon = Res.drawable.ic_refresh,
                        actionDescription = stringResource(Res.string.recurring_detail_unskip_cd, label),
                        actionEnabled = state.canEdit,
                        onAction = { onUnskip(date) },
                    )
                }
            }

            // Offline the top bar shows no actions at all, and nothing else on screen would say why.
            if (!state.isOnline) {
                VerticalSpacer(16.dp)
                Text(
                    text = stringResource(Res.string.recurring_detail_offline_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        VerticalSpacer(24.dp)
    }

    if (state.isEndDialogVisible) {
        AlertDialog(
            onDismissRequest = onEndDismiss,
            title = { Text(stringResource(Res.string.recurring_detail_end_dialog_title)) },
            text = { Text(stringResource(Res.string.recurring_detail_end_dialog_message)) },
            confirmButton = {
                TextButton(onClick = onEndConfirm) {
                    Text(
                        text = stringResource(Res.string.recurring_detail_end_dialog_confirm),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onEndDismiss) {
                    Text(stringResource(Res.string.recurring_detail_end_dialog_cancel))
                }
            },
        )
    }
}

/**
 * One future date and the one thing you can do to it.
 *
 * The action is an icon rather than a labelled button because these rows repeat: six identical
 * "Skip" buttons were the loudest thing on the screen and six identical screen-reader
 * announcements. The description names the date so each one still says which date it acts on.
 */
@Composable
private fun OccurrenceRow(
    label: String,
    actionIcon: DrawableResource,
    actionDescription: String,
    actionEnabled: Boolean,
    onAction: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onAction, enabled = actionEnabled) {
            Icon(
                imageVector = vectorResource(actionIcon),
                contentDescription = actionDescription,
            )
        }
    }
}
