package de.tabmates.features.tabgroup.presentation.navigation.groupdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.tabmates.core.domain.auth.SessionStorage
import de.tabmates.features.tabgroup.domain.balance.PerPersonBalanceCalculator
import de.tabmates.features.tabgroup.domain.currency.CurrencyRepository
import de.tabmates.features.tabgroup.domain.group.GroupRepository
import de.tabmates.features.tabgroup.domain.models.GroupParticipant
import de.tabmates.features.tabgroup.domain.models.TabEntry
import de.tabmates.features.tabgroup.domain.tabentry.TabEntryRepository
import de.tabmates.features.tabgroup.presentation.navigation.groupoverview.GroupOverviewItem
import de.tabmates.features.tabgroup.presentation.navigation.groupoverview.toUiItem
import de.tabmates.features.tabgroup.presentation.navigation.groupoverview.withStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import kotlin.time.Duration.Companion.seconds

data class GroupDetailState(
    val item: GroupOverviewItem? = null,
    val currentUserId: String = "",
    val currentUserInitials: String = "",
    val members: List<GroupParticipant> = emptyList(),
    val expenses: List<TabEntry.Expense> = emptyList(),
    val perPersonBalances: Map<String, Double> = emptyMap(),
)

@KoinViewModel
class GroupDetailViewModel(
    @InjectedParam private val groupId: String,
    private val groupRepository: GroupRepository,
    private val tabEntryRepository: TabEntryRepository,
    currencyRepository: CurrencyRepository,
    sessionStorage: SessionStorage,
) : ViewModel() {
    private val currentUser = sessionStorage.get()?.user
    private val currentUserId = currentUser?.id.orEmpty()
    private val isRotatingInvite = MutableStateFlow(false)
    private var hasFetchedRemote = false

    val state: StateFlow<GroupDetailState> =
        combine(
            groupRepository
                .getGroups()
                .map { groups -> groups.firstOrNull { it.id == groupId } }
                .onStart { emit(null) },
            currencyRepository.getCurrencies().onStart { emit(emptyList()) },
            tabEntryRepository
                .getTabEntriesForGroup(groupId)
                .onStart {
                    emit(emptyList())
                    if (!hasFetchedRemote) {
                        hasFetchedRemote = true
                        viewModelScope.launch { tabEntryRepository.fetchTabEntries(groupId) }
                    }
                },
        ) { group, currencies, entries ->
            val visibleEntries = entries.filterNot { it.isDeleted }
            val item =
                group?.let {
                    val currency = currencies.firstOrNull { c -> c.code == it.defaultCurrencyCode }
                    it.toUiItem(currency).withStats(entries, currentUserId)
                }
            GroupDetailState(
                item = item,
                currentUserId = currentUserId,
                currentUserInitials =
                    currentUser
                        ?.username
                        ?.take(2)
                        ?.uppercase()
                        .orEmpty(),
                members = group?.participants?.toList().orEmpty(),
                expenses =
                    visibleEntries
                        .filterIsInstance<TabEntry.Expense>()
                        .sortedByDescending { it.createdAt },
                perPersonBalances =
                    PerPersonBalanceCalculator.computeByParticipant(visibleEntries, currentUserId),
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5.seconds),
            initialValue = GroupDetailState(),
        )

    fun rotateInvite() {
        if (groupId.isBlank() || isRotatingInvite.value) return
        viewModelScope.launch {
            isRotatingInvite.update { true }
            groupRepository.rotateInviteToken(groupId)
            isRotatingInvite.update { false }
        }
    }
}
