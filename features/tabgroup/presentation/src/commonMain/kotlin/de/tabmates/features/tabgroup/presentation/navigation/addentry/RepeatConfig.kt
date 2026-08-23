package de.tabmates.features.tabgroup.presentation.navigation.addentry

import de.tabmates.features.tabgroup.domain.recurring.RecurrenceFrequency
import de.tabmates.features.tabgroup.domain.recurring.RecurringEnd
import kotlinx.datetime.LocalDate

/**
 * The repeat half of the entry form, assembled from the editor's live fields.
 *
 * Null on the state means "does not repeat", which is what makes the form save an ordinary one-off
 * entry instead of a schedule.
 */
data class RepeatConfig(
    val frequency: RecurrenceFrequency,
    /** Repeat every N periods of [frequency]; 1 means every period. */
    val interval: Int = 1,
    /**
     * First occurrence, and the anchor every later date is computed from.
     *
     * The server refuses a start date in the past — a schedule may not reach back and invent entries
     * nobody agreed to — so the form clamps this to today or later rather than relying on the one
     * day of slack the server allows for clock skew.
     */
    val startDate: LocalDate,
    val end: RecurringEnd = RecurringEnd.Never,
)

/** What the "Ends" section of the repeat editor is currently set to. */
enum class RepeatEndKind {
    NEVER,
    ON_DATE,
    AFTER_COUNT,
}

val RecurringEnd.kind: RepeatEndKind
    get() =
        when (this) {
            RecurringEnd.Never -> RepeatEndKind.NEVER
            is RecurringEnd.Until -> RepeatEndKind.ON_DATE
            is RecurringEnd.Count -> RepeatEndKind.AFTER_COUNT
        }
