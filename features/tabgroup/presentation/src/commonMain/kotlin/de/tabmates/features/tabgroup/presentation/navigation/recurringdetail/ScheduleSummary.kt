package de.tabmates.features.tabgroup.presentation.navigation.recurringdetail

import androidx.compose.runtime.Composable
import de.tabmates.features.tabgroup.domain.recurring.RecurrenceFrequency
import de.tabmates.features.tabgroup.domain.recurring.RecurringEnd
import de.tabmates.features.tabgroup.domain.recurring.RecurringSeries
import de.tabmates.features.tabgroup.presentation.navigation.addentry.formatEntryDate
import org.jetbrains.compose.resources.stringResource
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_repeat_daily
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_repeat_every_n_days
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_repeat_every_n_months
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_repeat_every_n_weeks
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_repeat_every_n_years
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_repeat_monthly
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_repeat_weekly
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_repeat_yearly
import tabmatesapp.features.tabgroup.presentation.generated.resources.recurring_ends_after
import tabmatesapp.features.tabgroup.presentation.generated.resources.recurring_ends_on
import tabmatesapp.features.tabgroup.presentation.generated.resources.recurring_starts_on

/**
 * How a schedule reads in one line: cadence, when it started, and how it stops.
 *
 * Resolved in a composable and passed down rather than built in a ViewModel — `getString()` off the
 * composition crashes the headless desktop tests.
 */
@Composable
fun scheduleSummary(
    series: RecurringSeries,
    monthLabels: List<String>,
): String {
    val rule = series.rule
    val cadence = frequencyLabel(rule.frequency, rule.interval)
    val start = stringResource(Res.string.recurring_starts_on, formatEntryDate(rule.startDate, monthLabels))
    val end =
        when (val ruleEnd = rule.end) {
            RecurringEnd.Never -> {
                null
            }

            is RecurringEnd.Until -> {
                stringResource(Res.string.recurring_ends_on, formatEntryDate(ruleEnd.date, monthLabels))
            }

            is RecurringEnd.Count -> {
                stringResource(Res.string.recurring_ends_after, ruleEnd.count)
            }
        }
    return listOfNotNull(cadence, start, end).joinToString(" · ")
}

@Composable
fun frequencyLabel(
    frequency: RecurrenceFrequency,
    interval: Int,
): String =
    if (interval == 1) {
        when (frequency) {
            RecurrenceFrequency.DAILY -> stringResource(Res.string.add_entry_repeat_daily)
            RecurrenceFrequency.WEEKLY -> stringResource(Res.string.add_entry_repeat_weekly)
            RecurrenceFrequency.MONTHLY -> stringResource(Res.string.add_entry_repeat_monthly)
            RecurrenceFrequency.YEARLY -> stringResource(Res.string.add_entry_repeat_yearly)
        }
    } else {
        when (frequency) {
            RecurrenceFrequency.DAILY -> stringResource(Res.string.add_entry_repeat_every_n_days, interval)
            RecurrenceFrequency.WEEKLY -> stringResource(Res.string.add_entry_repeat_every_n_weeks, interval)
            RecurrenceFrequency.MONTHLY -> stringResource(Res.string.add_entry_repeat_every_n_months, interval)
            RecurrenceFrequency.YEARLY -> stringResource(Res.string.add_entry_repeat_every_n_years, interval)
        }
    }
