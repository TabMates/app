package de.tabmates.features.tabgroup.presentation.navigation.groupschedules

import de.tabmates.features.tabgroup.domain.models.Currency
import de.tabmates.features.tabgroup.domain.recurring.RecurringSeries

/**
 * Every schedule in one group, split the way the screen shows them.
 *
 * Ended schedules are kept rather than dropped: they explain entries that already exist, and hiding
 * them would make those entries look like they came from nowhere.
 */
data class GroupSchedulesState(
    val isLoading: Boolean = true,
    val active: List<RecurringSeries> = emptyList(),
    val ended: List<RecurringSeries> = emptyList(),
    val currencyByCode: Map<String, Currency> = emptyMap(),
) {
    val isEmpty: Boolean get() = active.isEmpty() && ended.isEmpty()
}
