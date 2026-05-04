package de.tabmates.features.tabgroup.presentation.navigation.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.tabmates.core.domain.currency.Currency
import de.tabmates.core.domain.currency.CurrencyRepository
import de.tabmates.features.tabgroup.domain.group.GroupRepository
import de.tabmates.features.tabgroup.domain.models.Group
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import kotlin.time.Duration.Companion.seconds

@KoinViewModel
class GroupViewModel(
    private val groupRepository: GroupRepository,
    private val currencyRepository: CurrencyRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(GroupState())
    private var hasLoadedInitialData = false
    private var currencies: List<Currency> = emptyList()

    val state =
        _state
            .onStart {
                if (!hasLoadedInitialData) {
                    hasLoadedInitialData = true
                    loadCurrencies()
                    observeGroups()
                    refreshGroups()
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5.seconds),
                initialValue = _state.value,
            )

    private fun loadCurrencies() {
        viewModelScope.launch {
            currencies = currencyRepository.getCurrencies()
            _state.update { it.copy(items = it.items) }
        }
    }

    private fun observeGroups() {
        groupRepository
            .getGroups()
            .onEach { groups ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        items = groups.map(::toListItem),
                    )
                }
            }.launchIn(viewModelScope)
    }

    private fun refreshGroups() {
        viewModelScope.launch {
            groupRepository.fetchGroups()
            _state.update { it.copy(isLoading = false) }
        }
    }

    private fun toListItem(group: Group): GroupListItem {
        val symbol =
            currencies.firstOrNull { it.code == group.defaultCurrencyCode }?.nativeSymbol
                ?: group.defaultCurrencyCode
        val participants = group.participants.toList()
        val initials = participants.take(MAX_AVATARS).map { it.initials }
        val extra = (participants.size - MAX_AVATARS).coerceAtLeast(0)
        return GroupListItem(
            id = group.id,
            title = group.title,
            memberCount = participants.size,
            memberInitials = initials,
            extraMembers = extra,
            currencySymbol = symbol,
            balance = GroupBalance.Settled,
        )
    }

    private companion object {
        private const val MAX_AVATARS = 3
    }
}
