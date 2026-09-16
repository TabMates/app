package de.tabmates.features.tabgroup.presentation.navigation.recurringdetail

import de.tabmates.core.presentation.util.UiText

sealed interface RecurringSeriesDetailEvent {
    /** The schedule was ended; the screen has nothing left to show. */
    data object SeriesEnded : RecurringSeriesDetailEvent

    /** The schedule is gone — deleted with its group, or never synced to this device. */
    data object SeriesUnavailable : RecurringSeriesDetailEvent

    data class Error(
        val message: UiText,
    ) : RecurringSeriesDetailEvent
}
