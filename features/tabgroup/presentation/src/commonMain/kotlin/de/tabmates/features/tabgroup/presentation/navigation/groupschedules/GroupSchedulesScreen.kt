package de.tabmates.features.tabgroup.presentation.navigation.groupschedules

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.tabmates.core.designsystem.spacer.HorizontalSpacer
import de.tabmates.core.designsystem.text.SectionLabel
import de.tabmates.features.tabgroup.domain.models.Currency
import de.tabmates.features.tabgroup.domain.recurring.RecurringOccurrenceCalculator
import de.tabmates.features.tabgroup.domain.recurring.RecurringSeries
import de.tabmates.features.tabgroup.presentation.navigation.addentry.formatEntryDate
import de.tabmates.features.tabgroup.presentation.navigation.addentry.rememberMonthAbbreviations
import de.tabmates.features.tabgroup.presentation.navigation.groupoverview.EntryIcon
import de.tabmates.features.tabgroup.presentation.navigation.groupoverview.formatAmount
import de.tabmates.features.tabgroup.presentation.navigation.recurringdetail.frequencyLabel
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_calendar
import tabmatesapp.features.tabgroup.presentation.generated.resources.recurring_chip_ended
import tabmatesapp.features.tabgroup.presentation.generated.resources.recurring_chip_needs_attention
import tabmatesapp.features.tabgroup.presentation.generated.resources.recurring_empty_hint
import tabmatesapp.features.tabgroup.presentation.generated.resources.recurring_next_on
import tabmatesapp.features.tabgroup.presentation.generated.resources.recurring_section_active
import tabmatesapp.features.tabgroup.presentation.generated.resources.recurring_section_ended
import kotlin.time.Clock

@Composable
fun GroupSchedulesRoot(
    groupId: String,
    onSeriesClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GroupSchedulesViewModel =
        koinViewModel(
            key = groupId,
            parameters = { parametersOf(groupId) },
        ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    GroupSchedulesScreen(
        state = state,
        onSeriesClick = onSeriesClick,
        modifier = modifier,
    )
}

/**
 * Every schedule in the group, read-only.
 *
 * Everything you can do to a schedule lives on its detail screen, so a row is a link and nothing
 * else. Ended schedules stay in the list rather than vanishing: they explain entries that already
 * exist, and hiding them would make those entries look like they came from nowhere.
 */
@Composable
private fun GroupSchedulesScreen(
    state: GroupSchedulesState,
    onSeriesClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    if (state.isEmpty) {
        Box(modifier = modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(Res.string.recurring_empty_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        return
    }

    val monthLabels = rememberMonthAbbreviations()
    val today = rememberTodayUtc()
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        scheduleSection(
            sectionKey = "active",
            title = Res.string.recurring_section_active,
            series = state.active,
            currencyByCode = state.currencyByCode,
            monthLabels = monthLabels,
            today = today,
            onSeriesClick = onSeriesClick,
        )
        scheduleSection(
            sectionKey = "ended",
            title = Res.string.recurring_section_ended,
            series = state.ended,
            currencyByCode = state.currencyByCode,
            monthLabels = monthLabels,
            today = today,
            onSeriesClick = onSeriesClick,
        )
    }
}

private fun LazyListScope.scheduleSection(
    sectionKey: String,
    title: StringResource,
    series: List<RecurringSeries>,
    currencyByCode: Map<String, Currency>,
    monthLabels: List<String>,
    today: LocalDate,
    onSeriesClick: (String) -> Unit,
) {
    if (series.isEmpty()) return
    item(key = "header-$sectionKey") {
        SectionLabel(
            text = stringResource(title),
            // Same 24.dp rail the rows below use, which is the rail the transactions tab this
            // screen is reached from uses too.
            modifier = Modifier.padding(horizontal = 24.dp).padding(top = 12.dp, bottom = 4.dp),
        )
    }
    items(series, key = { it.seriesId }) { candidate ->
        ScheduleRow(
            series = candidate,
            currency = currencyByCode[candidate.rule.currencyCode],
            monthLabels = monthLabels,
            today = today,
            onClick = { onSeriesClick(candidate.seriesId) },
        )
    }
}

/**
 * The same row the transactions tab's upcoming section draws, so a schedule does not change shape
 * on the way to the screen that lists all of them.
 */
@Composable
private fun ScheduleRow(
    series: RecurringSeries,
    currency: Currency?,
    monthLabels: List<String>,
    today: LocalDate,
    onClick: () -> Unit,
) {
    // A parked schedule has no next occurrence to promise — the server writes nothing for it until
    // the template is repaired — so the subtitle falls back to the cadence alone. Remembered because
    // finding the date walks the schedule slot by slot from its start.
    val nextOccurrence =
        remember(series, today) {
            if (series.isActive && !series.needsAttention) {
                RecurringOccurrenceCalculator
                    .upcomingOccurrences(
                        rule = series.rule,
                        after = today,
                        limit = 1,
                        skippedDates = series.skippedOccurrenceDates,
                    ).firstOrNull()
            } else {
                null
            }
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (series.needsAttention) {
            EntryIcon(
                icon = Res.drawable.ic_calendar,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            )
        } else {
            EntryIcon(Res.drawable.ic_calendar)
        }
        HorizontalSpacer(12.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = series.rule.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text =
                    listOfNotNull(
                        frequencyLabel(series.rule.frequency, series.rule.interval),
                        nextOccurrence?.let {
                            stringResource(Res.string.recurring_next_on, formatEntryDate(it, monthLabels))
                        },
                    ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        HorizontalSpacer(8.dp)
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text =
                    formatAmount(
                        series.rule.amount,
                        currency?.nativeSymbol ?: series.rule.currencyCode,
                        currency?.decimalDigits ?: DEFAULT_DECIMALS,
                    ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                // A template is not a booked amount, so it reads muted here exactly as it does in
                // the upcoming section.
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            when {
                series.needsAttention -> {
                    ScheduleStateChip(
                        text = stringResource(Res.string.recurring_chip_needs_attention),
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                !series.isActive -> {
                    ScheduleStateChip(
                        text = stringResource(Res.string.recurring_chip_ended),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduleStateChip(
    text: String,
    color: Color,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
    )
}

/** The day the server's sweep measures against, so both agree on what is still upcoming. */
@Composable
private fun rememberTodayUtc(): LocalDate =
    remember {
        Clock.System
            .now()
            .toLocalDateTime(TimeZone.UTC)
            .date
    }

private const val DEFAULT_DECIMALS = 2
