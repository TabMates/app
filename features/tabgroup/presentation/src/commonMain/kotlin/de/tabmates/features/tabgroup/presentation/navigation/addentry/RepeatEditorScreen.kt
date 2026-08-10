package de.tabmates.features.tabgroup.presentation.navigation.addentry

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.tabmates.core.designsystem.spacer.VerticalSpacer
import de.tabmates.core.designsystem.text.SectionLabel
import de.tabmates.features.tabgroup.domain.recurring.RecurrenceFrequency
import de.tabmates.features.tabgroup.domain.recurring.RecurringEnd
import de.tabmates.features.tabgroup.domain.recurring.RecurringOccurrenceCalculator
import de.tabmates.features.tabgroup.domain.recurring.RecurringRule
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import org.jetbrains.compose.resources.stringResource
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_repeat_daily
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_repeat_decrease_cd
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_repeat_ends_after_count
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_repeat_ends_count_suffix
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_repeat_ends_label
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_repeat_ends_never
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_repeat_ends_on_date
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_repeat_every_label
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_repeat_frequency_label
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_repeat_increase_cd
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_repeat_interval_label
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_repeat_monthly
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_repeat_never
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_repeat_preview_label
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_repeat_replaces_entry_note
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_repeat_starts_label
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_repeat_weekday_note
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_repeat_weekly
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_repeat_yearly
import tabmatesapp.features.tabgroup.presentation.generated.resources.repeat_unit_day
import tabmatesapp.features.tabgroup.presentation.generated.resources.repeat_unit_days
import tabmatesapp.features.tabgroup.presentation.generated.resources.repeat_unit_month
import tabmatesapp.features.tabgroup.presentation.generated.resources.repeat_unit_months
import tabmatesapp.features.tabgroup.presentation.generated.resources.repeat_unit_week
import tabmatesapp.features.tabgroup.presentation.generated.resources.repeat_unit_weeks
import tabmatesapp.features.tabgroup.presentation.generated.resources.repeat_unit_year
import tabmatesapp.features.tabgroup.presentation.generated.resources.repeat_unit_years

/**
 * Full-screen editor for how an entry repeats, mirroring the split editor: an in-screen sub-view
 * rather than a nav destination, editing the form state live with no separate confirm step.
 *
 * A bottom sheet was the obvious shape and the wrong one — the frequency list, an interval stepper,
 * a start date and three end options do not fit one without scrolling a sheet inside a sheet, and
 * the preview below is what makes a schedule legible before it is saved.
 */
@Composable
internal fun RepeatEditorScreen(
    state: AddEntryState,
    monthLabels: List<String>,
    onFrequencyChange: (RecurrenceFrequency?) -> Unit,
    onIntervalChange: (Int) -> Unit,
    onStartDateClick: () -> Unit,
    onEndChange: (RecurringEnd) -> Unit,
    onEndDateClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .verticalScroll(rememberScrollState()),
    ) {
        VerticalSpacer(8.dp)
        SectionLabel(
            text = stringResource(Res.string.add_entry_repeat_frequency_label),
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        RepeatOptionRow(
            label = stringResource(Res.string.add_entry_repeat_never),
            selected = state.repeatFrequency == null,
            onClick = { onFrequencyChange(null) },
        )
        RecurrenceFrequency.entries.forEach { candidate ->
            RepeatOptionRow(
                label = candidate.label(),
                selected = state.repeatFrequency == candidate,
                onClick = { onFrequencyChange(candidate) },
            )
        }

        if (state.repeatFrequency != null) {
            VerticalSpacer(8.dp)
            HorizontalDivider()
            VerticalSpacer(8.dp)

            SectionLabel(
                text = stringResource(Res.string.add_entry_repeat_interval_label),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            IntervalStepper(
                frequency = state.repeatFrequency,
                interval = state.repeatInterval,
                onIntervalChange = onIntervalChange,
            )

            FieldRow(
                label = stringResource(Res.string.add_entry_repeat_starts_label),
                value = formatEntryDate(state.repeatStartDate, monthLabels),
                onClick = onStartDateClick,
                leadingIcon = null,
            )

            VerticalSpacer(8.dp)
            SectionLabel(
                text = stringResource(Res.string.add_entry_repeat_ends_label),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            RepeatOptionRow(
                label = stringResource(Res.string.add_entry_repeat_ends_never),
                selected = state.repeatEnd.kind == RepeatEndKind.NEVER,
                onClick = { onEndChange(RecurringEnd.Never) },
            )
            RepeatOptionRow(
                label = stringResource(Res.string.add_entry_repeat_ends_on_date),
                selected = state.repeatEnd.kind == RepeatEndKind.ON_DATE,
                // Opening the picker is how this option gets a date at all, so selecting it and
                // picking one are the same gesture rather than two.
                onClick = onEndDateClick,
                trailing =
                    (state.repeatEnd as? RecurringEnd.Until)
                        ?.let { formatEntryDate(it.date, monthLabels) },
            )
            RepeatOptionRow(
                label = stringResource(Res.string.add_entry_repeat_ends_after_count),
                selected = state.repeatEnd.kind == RepeatEndKind.AFTER_COUNT,
                onClick = { onEndChange(RecurringEnd.Count(defaultEndCount(state.repeatEnd))) },
            )
            if (state.repeatEnd is RecurringEnd.Count) {
                CountStepper(
                    count = state.repeatEnd.count,
                    onCountChange = { onEndChange(RecurringEnd.Count(it)) },
                )
            }

            VerticalSpacer(16.dp)
            HorizontalDivider()
            VerticalSpacer(8.dp)
            RepeatPreview(state = state, monthLabels = monthLabels)
        }
        VerticalSpacer(32.dp)
    }
}

/** One-line summary of the current repeat setting, for the form row that opens this editor. */
@Composable
internal fun repeatSummary(state: AddEntryState): String {
    val frequency = state.repeatFrequency ?: return stringResource(Res.string.add_entry_repeat_never)
    return if (state.repeatInterval == 1) {
        frequency.label()
    } else {
        stringResource(
            Res.string.add_entry_repeat_every_label,
            state.repeatInterval,
            frequency.unitLabel(state.repeatInterval),
        )
    }
}

/**
 * The next few dates this schedule will actually produce.
 *
 * Worth the space: a monthly schedule anchored on the 31st lands on the 28th in February and back
 * on the 31st in March, and no amount of label copy explains that as well as showing it.
 */
@Composable
private fun RepeatPreview(
    state: AddEntryState,
    monthLabels: List<String>,
) {
    val repeat = state.repeat ?: return
    // Anchored one day before the start so the first occurrence is included: the calculator
    // returns dates strictly after what it is given. Remembered because it walks the schedule slot
    // by slot, and nothing about it changes between recompositions of the same config.
    val dates =
        remember(repeat) {
            RecurringOccurrenceCalculator.upcomingOccurrences(
                rule = repeat.toPreviewRule(),
                after = repeat.startDate.minus(1, DateTimeUnit.DAY),
                limit = PREVIEW_COUNT,
            )
        }

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        SectionLabel(text = stringResource(Res.string.add_entry_repeat_preview_label))
        VerticalSpacer(4.dp)
        dates.forEach { date ->
            Text(
                text = formatEntryDate(date, monthLabels),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (state.repeatFrequency == RecurrenceFrequency.WEEKLY) {
            VerticalSpacer(8.dp)
            RepeatNote(stringResource(Res.string.add_entry_repeat_weekday_note))
        }
        VerticalSpacer(8.dp)
        RepeatNote(stringResource(Res.string.add_entry_repeat_replaces_entry_note))
    }
}

@Composable
private fun RepeatOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    trailing: String? = null,
) {
    // The row is the whole target, and the button inside it is decoration — two separate click
    // handlers would have a screen reader announce the same option twice.
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
                .padding(horizontal = 8.dp),
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp),
            )
        }
    }
}

@Composable
private fun IntervalStepper(
    frequency: RecurrenceFrequency,
    interval: Int,
    onIntervalChange: (Int) -> Unit,
) {
    StepperRow(
        label = stringResource(Res.string.add_entry_repeat_every_label, interval, frequency.unitLabel(interval)),
        value = interval,
        minValue = 1,
        maxValue = MAX_INTERVAL,
        onValueChange = onIntervalChange,
    )
}

@Composable
private fun CountStepper(
    count: Int,
    onCountChange: (Int) -> Unit,
) {
    StepperRow(
        label = "$count ${stringResource(Res.string.add_entry_repeat_ends_count_suffix)}",
        value = count,
        minValue = 1,
        maxValue = MAX_END_COUNT,
        onValueChange = onCountChange,
    )
}

/**
 * The buttons are bare glyphs, so each carries its own description — a screen reader has nothing
 * else to announce them by.
 */
@Composable
private fun StepperRow(
    label: String,
    value: Int,
    minValue: Int,
    maxValue: Int,
    onValueChange: (Int) -> Unit,
) {
    val decreaseLabel = stringResource(Res.string.add_entry_repeat_decrease_cd)
    val increaseLabel = stringResource(Res.string.add_entry_repeat_increase_cd)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        IconButton(
            onClick = { onValueChange((value - 1).coerceAtLeast(minValue)) },
            enabled = value > minValue,
            modifier = Modifier.semantics { contentDescription = decreaseLabel },
        ) {
            Text(text = "−", style = MaterialTheme.typography.titleLarge)
        }
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        IconButton(
            onClick = { onValueChange((value + 1).coerceAtMost(maxValue)) },
            enabled = value < maxValue,
            modifier = Modifier.semantics { contentDescription = increaseLabel },
        ) {
            Text(text = "+", style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun RepeatNote(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun RecurrenceFrequency.label(): String =
    when (this) {
        RecurrenceFrequency.DAILY -> stringResource(Res.string.add_entry_repeat_daily)
        RecurrenceFrequency.WEEKLY -> stringResource(Res.string.add_entry_repeat_weekly)
        RecurrenceFrequency.MONTHLY -> stringResource(Res.string.add_entry_repeat_monthly)
        RecurrenceFrequency.YEARLY -> stringResource(Res.string.add_entry_repeat_yearly)
    }

/** "day"/"days" etc., for the "Every N …" stepper label. */
@Composable
private fun RecurrenceFrequency.unitLabel(count: Int): String =
    stringResource(
        when (this) {
            RecurrenceFrequency.DAILY -> {
                if (count ==
                    1
                ) {
                    Res.string.repeat_unit_day
                } else {
                    Res.string.repeat_unit_days
                }
            }

            RecurrenceFrequency.WEEKLY -> {
                if (count ==
                    1
                ) {
                    Res.string.repeat_unit_week
                } else {
                    Res.string.repeat_unit_weeks
                }
            }

            RecurrenceFrequency.MONTHLY -> {
                if (count == 1) Res.string.repeat_unit_month else Res.string.repeat_unit_months
            }

            RecurrenceFrequency.YEARLY -> {
                if (count ==
                    1
                ) {
                    Res.string.repeat_unit_year
                } else {
                    Res.string.repeat_unit_years
                }
            }
        },
    )

/** Keeps a previously chosen count when the option is re-selected, rather than resetting it. */
private fun defaultEndCount(current: RecurringEnd): Int =
    (current as? RecurringEnd.Count)?.count ?: DEFAULT_END_COUNT

/**
 * The config as a rule the occurrence calculator can walk. Only the schedule fields matter for a
 * preview, so the template half is filled with placeholders that are never read.
 */
private fun RepeatConfig.toPreviewRule(): RecurringRule =
    RecurringRule(
        ruleId = "",
        title = "",
        description = "",
        amount = 0.0,
        currencyCode = "",
        exchangeRate = null,
        paidByUserId = "",
        receivedByUserId = null,
        splits = emptyList(),
        frequency = frequency,
        interval = interval,
        startDate = startDate,
        end = end,
    )

private const val PREVIEW_COUNT = 4
private const val DEFAULT_END_COUNT = 12

// Stepper ceilings, not a server contract — it states no bound. They exist so a held-down button
// cannot walk the value somewhere nobody meant it to go.
private const val MAX_INTERVAL = 99
private const val MAX_END_COUNT = 999
