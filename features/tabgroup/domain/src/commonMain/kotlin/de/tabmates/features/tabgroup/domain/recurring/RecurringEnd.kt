package de.tabmates.features.tabgroup.domain.recurring

import kotlinx.datetime.LocalDate

/** How a recurring series stops producing occurrences. */
sealed class RecurringEnd {
    /** The series runs until somebody ends it. */
    data object Never : RecurringEnd()

    /** Inclusive: an occurrence landing exactly on [date] is still produced. */
    data class Until(
        val date: LocalDate,
    ) : RecurringEnd()

    /**
     * Total occurrences the series may produce, **counting ones a skip left empty**. Skipping one
     * month of a twelve-occurrence schedule leaves eleven entries; it does not run a month longer.
     */
    data class Count(
        val count: Int,
    ) : RecurringEnd()
}
