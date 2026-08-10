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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.tabmates.core.designsystem.banner.StatusBanner
import de.tabmates.core.designsystem.banner.StatusBannerTone
import de.tabmates.core.designsystem.spacer.VerticalSpacer
import de.tabmates.core.designsystem.text.SectionLabel
import de.tabmates.core.presentation.util.ObserveAsEvents
import de.tabmates.features.tabgroup.presentation.components.formatMoney
import de.tabmates.features.tabgroup.presentation.navigation.addentry.formatEntryDate
import de.tabmates.features.tabgroup.presentation.navigation.addentry.rememberMonthAbbreviations
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.recurring_detail_created_by
import tabmatesapp.features.tabgroup.presentation.generated.resources.recurring_detail_end
import tabmatesapp.features.tabgroup.presentation.generated.resources.recurring_detail_end_dialog_cancel
import tabmatesapp.features.tabgroup.presentation.generated.resources.recurring_detail_end_dialog_confirm
import tabmatesapp.features.tabgroup.presentation.generated.resources.recurring_detail_end_dialog_message
import tabmatesapp.features.tabgroup.presentation.generated.resources.recurring_detail_end_dialog_title
import tabmatesapp.features.tabgroup.presentation.generated.resources.recurring_detail_ended_note
import tabmatesapp.features.tabgroup.presentation.generated.resources.recurring_detail_needs_attention_message
import tabmatesapp.features.tabgroup.presentation.generated.resources.recurring_detail_needs_attention_title
import tabmatesapp.features.tabgroup.presentation.generated.resources.recurring_detail_offline_note
import tabmatesapp.features.tabgroup.presentation.generated.resources.recurring_detail_schedule_section
import tabmatesapp.features.tabgroup.presentation.generated.resources.recurring_detail_skip
import tabmatesapp.features.tabgroup.presentation.generated.resources.recurring_detail_skip_note
import tabmatesapp.features.tabgroup.presentation.generated.resources.recurring_detail_skipped_section
import tabmatesapp.features.tabgroup.presentation.generated.resources.recurring_detail_unavailable
import tabmatesapp.features.tabgroup.presentation.generated.resources.recurring_detail_unskip
import tabmatesapp.features.tabgroup.presentation.generated.resources.recurring_detail_upcoming_section

/**
 * Read-only detail for one schedule, mirroring the entry detail screen: the template on top, the
 * actions in the top bar, and nothing editable inline.
 */
@Composable
fun RecurringSeriesDetailRoot(
    groupId: String,
    seriesId: String,
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

    RecurringSeriesDetailScreen(
        state = state,
        onSkip = viewModel::onSkipOccurrence,
        onUnskip = viewModel::onUnskipOccurrence,
        onEndClick = viewModel::onEndClick,
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
    onEndClick: () -> Unit,
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
    ) {
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
                // Tapping the banner is the repair path; the message already says so.
                onClick = onEdit.takeIf { state.canEdit },
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        if (!state.isActive) {
            Text(
                text = stringResource(Res.string.recurring_detail_ended_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        VerticalSpacer(16.dp)
        Text(text = series.rule.title, style = MaterialTheme.typography.headlineSmall)
        Text(
            text = formatMoney(state.currencySymbol, series.rule.amount, state.currencyDecimalDigits),
            style = MaterialTheme.typography.headlineMedium,
        )
        if (series.rule.description.isNotBlank()) {
            VerticalSpacer(4.dp)
            Text(text = series.rule.description, style = MaterialTheme.typography.bodyMedium)
        }
        VerticalSpacer(4.dp)
        Text(
            text = stringResource(Res.string.recurring_detail_created_by, series.createdBy.username),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        VerticalSpacer(20.dp)
        SectionLabel(text = stringResource(Res.string.recurring_detail_schedule_section))
        Text(
            text = scheduleSummary(series, monthLabels),
            style = MaterialTheme.typography.bodyLarge,
        )

        if (state.upcomingOccurrences.isNotEmpty()) {
            VerticalSpacer(20.dp)
            SectionLabel(text = stringResource(Res.string.recurring_detail_upcoming_section))
            state.upcomingOccurrences.forEach { date ->
                OccurrenceRow(
                    label = formatEntryDate(date, monthLabels),
                    actionLabel = stringResource(Res.string.recurring_detail_skip),
                    // Only a future, not-yet-created occurrence can be skipped; a created one is an
                    // ordinary entry and is deleted from the entry itself.
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
            SectionLabel(text = stringResource(Res.string.recurring_detail_skipped_section))
            state.skippedUpcoming.forEach { date ->
                OccurrenceRow(
                    label = formatEntryDate(date, monthLabels),
                    actionLabel = stringResource(Res.string.recurring_detail_unskip),
                    actionEnabled = state.canEdit,
                    onAction = { onUnskip(date) },
                )
            }
        }

        if (!state.isOnline) {
            VerticalSpacer(16.dp)
            Text(
                text = stringResource(Res.string.recurring_detail_offline_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (state.canEnd) {
            VerticalSpacer(24.dp)
            HorizontalDivider()
            TextButton(onClick = onEndClick, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(Res.string.recurring_detail_end),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        VerticalSpacer(32.dp)
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

@Composable
private fun OccurrenceRow(
    label: String,
    actionLabel: String,
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
        TextButton(onClick = onAction, enabled = actionEnabled) {
            Text(text = actionLabel)
        }
    }
}
