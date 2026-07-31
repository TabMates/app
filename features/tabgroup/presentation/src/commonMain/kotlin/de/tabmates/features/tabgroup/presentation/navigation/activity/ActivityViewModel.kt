package de.tabmates.features.tabgroup.presentation.navigation.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.tabmates.core.domain.auth.CurrentAccount
import de.tabmates.features.tabgroup.domain.activity.ActivityFeedItem
import de.tabmates.features.tabgroup.domain.activity.ActivityRepository
import de.tabmates.features.tabgroup.domain.currency.CurrencyRepository
import de.tabmates.features.tabgroup.domain.group.GroupRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.KoinViewModel
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

/**
 * The account-wide feed: the mirrored server log merged with pending local writes.
 *
 * Names come from [GroupRepository.getAllParticipants], not from each group's member list, because a
 * diff's *old* value can name someone who has since left the group.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@KoinViewModel
class ActivityViewModel(
    activityRepository: ActivityRepository,
    groupRepository: GroupRepository,
    currencyRepository: CurrencyRepository,
    currentAccount: CurrentAccount,
) : ViewModel() {
    private val currentUserId =
        currentAccount.userId().orEmpty()

    private val pageSize = MutableStateFlow(INITIAL_PAGE_SIZE)

    val state: StateFlow<ActivityState> =
        combine(
            pageSize.flatMapLatest { limit -> activityRepository.observeAccountFeed(limit) },
            groupRepository.getGroups(),
            groupRepository.getAllParticipants(),
            currencyRepository.getCurrencies(),
            pageSize,
        ) { feed, groups, participants, currencies, limit ->
            ActivityState(
                isLoading = false,
                sections =
                    ActivityFeedBuilder.build(
                        items = feed,
                        currentUserId = currentUserId,
                        groupTitles = groups.associate { it.id to it.title },
                        participantNames = participants.associate { it.userId to it.username },
                        currencyByCode = currencies.associateBy { it.code },
                        now = Clock.System.now(),
                    ),
                // Pending rows are exempt from the limit, so only the persisted tail decides whether
                // there is more log to fetch.
                canLoadMore = feed.count { it is ActivityFeedItem.Persisted } >= limit,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5.seconds),
            initialValue = ActivityState(),
        )

    fun loadMore() {
        pageSize.update { it + PAGE_SIZE_INCREMENT }
    }

    private companion object {
        private const val INITIAL_PAGE_SIZE = 40
        private const val PAGE_SIZE_INCREMENT = 40
    }
}
