package de.tabmates.features.tabgroup.presentation.navigation.groupschedules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.tabmates.features.tabgroup.domain.currency.CurrencyRepository
import de.tabmates.features.tabgroup.domain.recurring.RecurringSeriesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import kotlin.time.Duration.Companion.seconds

/**
 * The group's schedules, listed. Deliberately thin: it reads the local mirror and nothing else.
 *
 * Separate from `GroupDetailViewModel` rather than reusing it — that one carries the whole group
 * screen (entries, balances, history, pagination) and none of it is needed to show a list of rules.
 */
@KoinViewModel
class GroupSchedulesViewModel(
    @InjectedParam private val groupId: String,
    recurringSeriesRepository: RecurringSeriesRepository,
    currencyRepository: CurrencyRepository,
) : ViewModel() {
    val state: StateFlow<GroupSchedulesState> =
        combine(
            recurringSeriesRepository.getSeriesForGroup(groupId),
            currencyRepository.getCurrencies().onStart { emit(emptyList()) },
        ) { series, currencies ->
            val (active, ended) = series.partition { it.isActive }
            GroupSchedulesState(
                isLoading = false,
                // Newest first within each group, matching how the rest of the app orders things a
                // member created. The list is short enough that no other ordering earns its keep.
                active = active.sortedByDescending { it.createdAt },
                ended = ended.sortedByDescending { it.createdAt },
                currencyByCode = currencies.associateBy { it.code },
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5.seconds),
            initialValue = GroupSchedulesState(),
        )
}
