package de.tabmates.features.tabgroup.presentation.navigation.recurringdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.EmptyResult
import de.tabmates.core.domain.util.onFailure
import de.tabmates.core.domain.util.onSuccess
import de.tabmates.core.presentation.util.toUiText
import de.tabmates.features.tabgroup.domain.currency.CurrencyRepository
import de.tabmates.features.tabgroup.domain.group.GroupRepository
import de.tabmates.features.tabgroup.domain.recurring.RecurringOccurrenceCalculator
import de.tabmates.features.tabgroup.domain.recurring.RecurringSeries
import de.tabmates.features.tabgroup.domain.recurring.RecurringSeriesRepository
import de.tabmates.features.tabgroup.domain.sync.ConnectionStatusRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

/**
 * A read-only view of one schedule, plus the three things a member can do to it: skip a future
 * occurrence, edit the template from a future occurrence onwards, and end it.
 *
 * All three go straight to the server. There is no outbox behind a schedule — it is a standing
 * instruction to write into other people's ledgers — so every action is gated on being online and
 * reports its own failure rather than deferring.
 */
@KoinViewModel
class RecurringSeriesDetailViewModel(
    @InjectedParam private val groupId: String,
    @InjectedParam private val seriesId: String,
    private val recurringSeriesRepository: RecurringSeriesRepository,
    groupRepository: GroupRepository,
    currencyRepository: CurrencyRepository,
    connectionStatusRepository: ConnectionStatusRepository,
) : ViewModel() {
    private val mutation = MutableStateFlow(MutationState())

    private val eventChannel = Channel<RecurringSeriesDetailEvent>()
    val events = eventChannel.receiveAsFlow()

    val state: StateFlow<RecurringSeriesDetailState> =
        combine(
            recurringSeriesRepository.getSeriesById(seriesId).onStart { emit(null) },
            groupRepository.getGroups().map { groups -> groups.firstOrNull { it.id == groupId } },
            groupRepository.getAllParticipants().onStart { emit(emptyList()) },
            currencyRepository.getCurrencies().onStart { emit(emptyList()) },
            mutation,
        ) { series, group, allParticipants, currencies, mutation ->
            if (series == null) {
                return@combine RecurringSeriesDetailState(isLoading = false, isOnline = mutation.isOnline)
            }

            val today = todayUtc()
            val currency = currencies.firstOrNull { it.code == series.rule.currencyCode }
            // Names come from every known participant, not the group's current members: a template
            // can outlive the membership of the people in it, and naming them is the whole point of
            // the warning shown when it does.
            val participantsById =
                (allParticipants + group?.participants.orEmpty()).associateBy { it.userId }
            val activeMemberIds = group?.participants.orEmpty().mapTo(mutableSetOf()) { it.userId }

            RecurringSeriesDetailState(
                isLoading = false,
                series = series,
                participantsById = participantsById,
                currencySymbol = currency?.nativeSymbol ?: series.rule.currencyCode,
                currencyDecimalDigits = currency?.decimalDigits ?: DEFAULT_DECIMALS,
                upcomingOccurrences =
                    if (series.isActive) {
                        RecurringOccurrenceCalculator.upcomingOccurrences(
                            rule = series.rule,
                            after = today,
                            limit = UPCOMING_COUNT,
                            skippedDates = series.skippedOccurrenceDates,
                        )
                    } else {
                        emptyList()
                    },
                skippedUpcoming = series.skippedOccurrenceDates.filter { it > today }.sorted(),
                departedParticipants =
                    series
                        .templateParticipantIds()
                        .filterNot { it in activeMemberIds }
                        .mapNotNull { participantsById[it] },
                isOnline = mutation.isOnline,
                isMutating = mutation.isMutating,
                isEndDialogVisible = mutation.isEndDialogVisible,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5.seconds),
            initialValue = RecurringSeriesDetailState(),
        )

    init {
        connectionStatusRepository.isConnected
            .onEach { connected -> mutation.update { it.copy(isOnline = connected) } }
            .launchIn(viewModelScope)
    }

    /**
     * Skips one future occurrence. The slot is still consumed, so the schedule does not run a
     * period longer to make up for it — the copy on screen says so.
     */
    fun onSkipOccurrence(date: LocalDate) {
        runMutation { recurringSeriesRepository.skipOccurrence(seriesId, date) }
    }

    fun onUnskipOccurrence(date: LocalDate) {
        runMutation { recurringSeriesRepository.unskipOccurrence(seriesId, date) }
    }

    fun onEndClick() {
        mutation.update { it.copy(isEndDialogVisible = true) }
    }

    fun onEndDismiss() {
        mutation.update { it.copy(isEndDialogVisible = false) }
    }

    /** Stops the schedule. Entries it already produced are ordinary entries and are left alone. */
    fun onEndConfirm() {
        mutation.update { it.copy(isEndDialogVisible = false) }
        runMutation(onSuccess = RecurringSeriesDetailEvent.SeriesEnded) {
            recurringSeriesRepository.endSeries(seriesId)
        }
    }

    /**
     * Tells the screen the schedule is gone rather than leaving it on a blank page. Separate from
     * the loading state so a slow first read is not mistaken for a missing series.
     */
    fun onMissingSeries() {
        viewModelScope.launch {
            if (recurringSeriesRepository.getSeriesById(seriesId).first() == null) {
                eventChannel.send(RecurringSeriesDetailEvent.SeriesUnavailable)
            }
        }
    }

    private fun runMutation(
        onSuccess: RecurringSeriesDetailEvent? = null,
        block: suspend () -> EmptyResult<DataError.Remote>,
    ) {
        if (mutation.value.isMutating) return
        viewModelScope.launch {
            mutation.update { it.copy(isMutating = true) }
            block()
                .onSuccess {
                    mutation.update { current -> current.copy(isMutating = false) }
                    onSuccess?.let { eventChannel.send(it) }
                }.onFailure { error ->
                    mutation.update { current -> current.copy(isMutating = false) }
                    eventChannel.send(RecurringSeriesDetailEvent.Error(error.toUiText()))
                }
        }
    }

    private fun RecurringSeries.templateParticipantIds(): List<String> =
        buildList {
            add(rule.paidByUserId)
            rule.receivedByUserId?.let(::add)
            rule.splits.mapTo(this) { it.participantId }
        }.distinct()

    /** Matches the day the server's sweep measures against, so both agree on what is still future. */
    private fun todayUtc(): LocalDate =
        Clock.System
            .now()
            .toLocalDateTime(TimeZone.UTC)
            .date

    private data class MutationState(
        val isOnline: Boolean = true,
        val isMutating: Boolean = false,
        val isEndDialogVisible: Boolean = false,
    )

    private companion object {
        const val UPCOMING_COUNT = 6
        const val DEFAULT_DECIMALS = 2
    }
}
