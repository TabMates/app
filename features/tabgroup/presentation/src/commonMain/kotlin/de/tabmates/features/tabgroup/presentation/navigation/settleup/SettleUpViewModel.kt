package de.tabmates.features.tabgroup.presentation.navigation.settleup

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.tabmates.core.domain.auth.CurrentAccount
import de.tabmates.core.domain.util.onFailure
import de.tabmates.core.domain.util.onSuccess
import de.tabmates.core.presentation.util.UiText
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
import de.tabmates.features.tabgroup.presentation.components.formatMoney
import de.tabmates.features.tabgroup.presentation.navigation.addentry.parseAmount
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.settle_up_amount_error_invalid
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
    currentAccount: CurrentAccount,
) : ViewModel() {
    private val currentUserId =
        currentAccount.userId().orEmpty()

    // Payer→receiver pairs with an in-flight settlement request, so their row shows a
    // spinner / disables. Pair-keyed because several plan rows can share a payer or receiver.
    private val settlingPairs = MutableStateFlow<Set<Pair<String, String>>>(emptySet())

    val state: StateFlow<SettleUpState> =
        combine(
            tabEntryRepository.getTabEntriesForGroup(groupId),
            groupRepository.getGroups(),
            currencyRepository.getCurrencies(),
            exchangeRateRepository.getExchangeRates(),
            settlingPairs,
        ) { entries, groups, currencies, rates, settling ->
            buildState(entries, groups, currencies, rates, settling)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = SettleUpState(groupId = groupId, currentUserId = currentUserId),
        )

    // The payment whose amount is being edited in the settle sheet. Kept outside `state`:
    // it is a snapshot of the tapped row, not derived repository data.
    private val pendingSettlementFlow = MutableStateFlow<SettleUpPayment?>(null)
    val pendingSettlement: StateFlow<SettleUpPayment?> = pendingSettlementFlow.asStateFlow()

    val settleAmountTextState = TextFieldState()

    private val eventChannel = Channel<SettleUpEvent>()
    val events = eventChannel.receiveAsFlow()

    fun onPaymentRowClick(payment: SettleUpPayment) {
        if (payment.fromUserId to payment.toUserId in settlingPairs.value) return
        settleAmountTextState.setTextAndPlaceCursorAtEnd(
            formatMoney("", payment.amount, state.value.currencyDecimalDigits),
        )
        pendingSettlementFlow.value = payment
    }

    fun onSettleDismiss() {
        pendingSettlementFlow.value = null
        settleAmountTextState.clearText()
    }

    // The localized default title is resolved by the composable: compose-resource lookups
    // are not available off the UI (e.g. in headless unit tests).
    fun onSettleConfirm(title: String) {
        val pending = pendingSettlementFlow.value ?: return
        val current = state.value
        val decimals = current.currencyDecimalDigits
        val epsilon = 0.5 / 10.0.pow(decimals)
        // Re-validate against the live plan: the debt may have vanished or shrunk while the
        // sheet was open (someone else settled it on another device).
        val livePayment =
            current.payments.firstOrNull {
                it.fromUserId == pending.fromUserId && it.toUserId == pending.toUserId
            }
        val amount = parseAmount(settleAmountTextState.text.toString())
        if (livePayment == null ||
            amount == null ||
            amount <= 0.0 ||
            amount > livePayment.amount + epsilon
        ) {
            onSettleDismiss()
            eventChannel.trySend(
                SettleUpEvent.Error(UiText.Resource(Res.string.settle_up_amount_error_invalid)),
            )
            return
        }
        val pair = pending.fromUserId to pending.toUserId
        settlingPairs.update { it + pair }
        onSettleDismiss()
        viewModelScope.launch {
            tabEntryRepository
                .createSettlement(
                    groupId = groupId,
                    title = title,
                    description = "",
                    amount = amount,
                    // Settle-up amounts are always in the group's default currency, so there is
                    // no rate to lock in.
                    currencyCode = current.currencyCode,
                    exchangeRate = null,
                    paidByUserId = pending.fromUserId,
                    receivedByUserId = pending.toUserId,
                    entryDate =
                        Clock.System
                            .now()
                            .toLocalDateTime(TimeZone.currentSystemDefault())
                            .date,
                ).onSuccess {
                    // The local insert re-emits the entries flow, which recomputes the plan and
                    // drops (or shrinks) this now-settled payment.
                    settlingPairs.update { it - pair }
                    eventChannel.send(
                        SettleUpEvent.PaymentRecorded(
                            fromName = pending.fromName,
                            toName = pending.toName,
                            isFromCurrentUser = pending.fromUserId == currentUserId,
                        ),
                    )
                }.onFailure { error ->
                    settlingPairs.update { it - pair }
                    eventChannel.send(SettleUpEvent.Error(error.toUiText()))
                }
        }
    }

    private fun buildState(
        entries: List<TabEntry>,
        groups: List<Group>,
        currencies: List<Currency>,
        rates: List<ExchangeRate>,
        settling: Set<Pair<String, String>>,
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
                .mapNotNull { debt ->
                    val payer =
                        participants.firstOrNull { it.userId == debt.fromUserId } ?: return@mapNotNull null
                    val recipient =
                        participants.firstOrNull { it.userId == debt.toUserId } ?: return@mapNotNull null
                    SettleUpPayment(
                        fromUserId = debt.fromUserId,
                        fromName = payer.username,
                        fromInitials = payer.initials,
                        toUserId = debt.toUserId,
                        toName = recipient.username,
                        toInitials = recipient.initials,
                        amount = roundTo(debt.amount, decimals),
                        isSettling = debt.fromUserId to debt.toUserId in settling,
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
