package de.tabmates.features.tabgroup.presentation.navigation.recurringdetail

import de.tabmates.features.tabgroup.domain.models.GroupParticipant
import de.tabmates.features.tabgroup.domain.recurring.RecurringSeries
import kotlinx.datetime.LocalDate

data class RecurringSeriesDetailState(
    val isLoading: Boolean = true,
    val series: RecurringSeries? = null,
    val participantsById: Map<String, GroupParticipant> = emptyMap(),
    val currencySymbol: String = "",
    val currencyDecimalDigits: Int = 2,
    /** The next occurrences the schedule will produce, skipped dates already left out. */
    val upcomingOccurrences: List<LocalDate> = emptyList(),
    /** Dates a member skipped that have not passed yet, so they can be un-skipped. */
    val skippedUpcoming: List<LocalDate> = emptyList(),
    /**
     * Members named by the template who are no longer in the group. This is what
     * [RecurringSeries.needsAttention] actually means, and naming them is the difference between a
     * warning somebody can act on and one they cannot.
     */
    val departedParticipants: List<GroupParticipant> = emptyList(),
    val isOnline: Boolean = true,
    val isMutating: Boolean = false,
    val isEndDialogVisible: Boolean = false,
) {
    val needsAttention: Boolean
        get() = series?.needsAttention == true

    val isActive: Boolean
        get() = series?.isActive == true

    /**
     * Ending is one-way in this UI. The server's only route back is an edit, which revives the
     * series as a side effect — not something to expose behind a button labelled anything else.
     */
    val canEdit: Boolean
        get() = isActive && isOnline && !isMutating

    val canEnd: Boolean
        get() = isActive && isOnline && !isMutating
}
