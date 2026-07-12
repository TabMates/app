package de.tabmates.features.tabgroup.presentation.navigation.settlementdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.tabmates.core.domain.auth.SessionStorage
import de.tabmates.core.domain.util.onFailure
import de.tabmates.core.domain.util.onSuccess
import de.tabmates.core.presentation.util.toUiText
import de.tabmates.features.tabgroup.domain.currency.CurrencyRepository
import de.tabmates.features.tabgroup.domain.group.GroupRepository
import de.tabmates.features.tabgroup.domain.models.TabEntry
import de.tabmates.features.tabgroup.domain.tabentry.TabEntryRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import kotlin.time.Duration.Companion.seconds

@KoinViewModel
class SettlementDetailViewModel(
    @InjectedParam private val settlementId: String,
    @InjectedParam private val groupId: String,
    private val tabEntryRepository: TabEntryRepository,
    groupRepository: GroupRepository,
    currencyRepository: CurrencyRepository,
    sessionStorage: SessionStorage,
) : ViewModel() {
    private val currentUserId =
        sessionStorage
            .get()
            ?.user
            ?.id
            .orEmpty()
    private val isDeleting = MutableStateFlow(false)

    val state: StateFlow<SettlementDetailState> =
        combine(
            tabEntryRepository.getTabEntryById(settlementId).onStart { emit(null) },
            groupRepository.getGroups().onStart { emit(emptyList()) },
            currencyRepository.getCurrencies().onStart { emit(emptyList()) },
            isDeleting,
        ) { entry, groups, currencies, deleting ->
            val group = groups.firstOrNull { it.id == groupId }
            val settlement = entry as? TabEntry.Settlement
            val currencyCode = settlement?.currencyCode ?: group?.defaultCurrencyCode.orEmpty()
            val currency = currencies.firstOrNull { it.code == currencyCode }
            SettlementDetailState(
                settlementId = settlementId,
                isLoading = entry == null,
                isDeleting = deleting,
                settlement = settlement,
                currentUserId = currentUserId,
                groupCurrencySymbol = currency?.nativeSymbol ?: currencyCode,
                groupCurrencyDecimalDigits = currency?.decimalDigits ?: 2,
                membersById =
                    group?.participants?.associateBy { it.userId }
                        ?: emptyMap(),
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5.seconds),
            initialValue = SettlementDetailState(settlementId = settlementId, currentUserId = currentUserId),
        )

    private val eventChannel = Channel<SettlementDetailEvent>()
    val events = eventChannel.receiveAsFlow()

    fun onConfirmDelete() {
        if (isDeleting.value) return
        viewModelScope.launch {
            isDeleting.update { true }
            tabEntryRepository
                .deleteTabEntry(settlementId)
                .onSuccess { eventChannel.send(SettlementDetailEvent.SettlementDeleted) }
                .onFailure { error ->
                    eventChannel.send(SettlementDetailEvent.Error(error.toUiText()))
                }
            isDeleting.update { false }
        }
    }
}
