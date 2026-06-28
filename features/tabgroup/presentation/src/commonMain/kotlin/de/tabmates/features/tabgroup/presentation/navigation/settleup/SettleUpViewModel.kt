package de.tabmates.features.tabgroup.presentation.navigation.settleup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.tabmates.core.domain.auth.SessionStorage
import de.tabmates.core.domain.util.onFailure
import de.tabmates.core.domain.util.onSuccess
import de.tabmates.core.presentation.util.toUiText
import de.tabmates.features.tabgroup.domain.balance.DebtSimplifier
import de.tabmates.features.tabgroup.domain.currency.CurrencyConversion
import de.tabmates.features.tabgroup.domain.currency.CurrencyRepository
import de.tabmates.features.tabgroup.domain.currency.ExchangeRateRepository
import de.tabmates.features.tabgroup.domain.group.GroupRepository
import de.tabmates.features.tabgroup.domain.models.Currency
import de.tabmates.features.tabgroup.domain.models.ExchangeRate
import de.tabmates.features.tabgroup.domain.models.Group
import de.tabmates.features.tabgroup.domain.models.TabEntry
import de.tabmates.features.tabgroup.domain.tabentry.TabEntryRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getString
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.settle_up_default_title
import kotlin.math.pow
import kotlin.math.round
import kotlin.time.Clock

@KoinViewModel
class SettleUpViewModel(
    @InjectedParam private val groupId: String,
    private val tabEntryRepository: TabEntryRepository,
    private val groupRepository: GroupRepository,
    private val currencyRepository: CurrencyRepository,
    private val exchangeRateRepository: ExchangeRateRepository,
    sessionStorage: SessionStorage,
) : ViewModel() {
    private val currentUserId =
        sessionStorage
            .get()
            ?.user
            ?.id
            .orEmpty()

    // UserIds with an in-flight settlement request, so their row shows a spinner / disables.
    private val settlingUserIds = MutableStateFlow<Set<String>>(emptySet())

    val state: StateFlow<SettleUpState> =
        combine(
            tabEntryRepository.getTabEntriesForGroup(groupId),
            groupRepository.getGroups(),
            currencyRepository.getCurrencies(),
            exchangeRateRepository.getExchangeRates(),
            settlingUserIds,
        ) { entries, groups, currencies, rates, settling ->
            buildState(entries, groups, currencies, rates, settling)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = SettleUpState(groupId = groupId, currentUserId = currentUserId),
        )

    private val eventChannel = Channel<SettleUpEvent>()
    val events = eventChannel.receiveAsFlow()

    fun onSettleClick(payment: SettleUpPayment) {
        val current = state.value
        if (payment.toUserId in settlingUserIds.value) return
        settlingUserIds.update { it + payment.toUserId }
        viewModelScope.launch {
            tabEntryRepository
                .createSettlement(
                    groupId = groupId,
                    title = getString(Res.string.settle_up_default_title),
                    description = "",
                    amount = payment.amount,
                    currencyCode = current.currencyCode,
                    paidByUserId = currentUserId,
                    receivedByUserId = payment.toUserId,
                    entryDate =
                        Clock.System
                            .now()
                            .toLocalDateTime(TimeZone.currentSystemDefault())
                            .date,
                ).onSuccess {
                    // The local insert re-emits the entries flow, which recomputes the plan and
                    // drops this now-settled payment.
                    settlingUserIds.update { it - payment.toUserId }
                    eventChannel.send(SettleUpEvent.PaymentRecorded(payment.toName))
                }.onFailure { error ->
                    settlingUserIds.update { it - payment.toUserId }
                    eventChannel.send(SettleUpEvent.Error(error.toUiText()))
                }
        }
    }

    private fun buildState(
        entries: List<TabEntry>,
        groups: List<Group>,
        currencies: List<Currency>,
        rates: List<ExchangeRate>,
        settling: Set<String>,
    ): SettleUpState {
        val group = groups.firstOrNull { it.id == groupId }
        val currency = currencies.firstOrNull { it.code == group?.defaultCurrencyCode }
        val decimals = currency?.decimalDigits ?: DEFAULT_DECIMALS
        val symbol = currency?.nativeSymbol ?: group?.defaultCurrencyCode.orEmpty()

        if (group == null || currentUserId.isEmpty()) {
            return SettleUpState(
                groupId = groupId,
                currentUserId = currentUserId,
                isLoading = group == null,
                currencyCode = group?.defaultCurrencyCode.orEmpty(),
                currencySymbol = symbol,
                currencyDecimalDigits = decimals,
                payments = emptyList(),
            )
        }

        val participants = group.participants
        val epsilon = 0.5 / 10.0.pow(decimals)
        val plan =
            DebtSimplifier.simplifyFromEntries(
                entries = entries,
                participantIds = participants.map { it.userId },
                conversion = CurrencyConversion.from(group.defaultCurrencyCode, rates),
                epsilon = epsilon,
            )
        val payments =
            plan
                .filter { it.fromUserId == currentUserId }
                .mapNotNull { debt ->
                    val recipient =
                        participants.firstOrNull { it.userId == debt.toUserId } ?: return@mapNotNull null
                    SettleUpPayment(
                        toUserId = debt.toUserId,
                        toName = recipient.username,
                        toInitials = recipient.initials,
                        amount = roundTo(debt.amount, decimals),
                        isSettling = debt.toUserId in settling,
                    )
                }.sortedByDescending { it.amount }

        return SettleUpState(
            groupId = groupId,
            currentUserId = currentUserId,
            isLoading = false,
            currencyCode = group.defaultCurrencyCode,
            currencySymbol = symbol,
            currencyDecimalDigits = decimals,
            payments = payments,
        )
    }

    private fun roundTo(
        amount: Double,
        decimals: Int,
    ): Double {
        val factor = 10.0.pow(decimals)
        return round(amount * factor) / factor
    }

    private companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L
        private const val DEFAULT_DECIMALS = 2
    }
}
