package de.tabmates.composeapp.sync

import de.tabmates.core.domain.auth.SessionStorage
import de.tabmates.core.domain.util.onSuccess
import de.tabmates.features.tabgroup.domain.group.GroupRepository
import de.tabmates.features.tabgroup.domain.tabentry.TabEntryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.koin.core.annotation.Single

@Single
class GroupSyncCoordinator(
    sessionStorage: SessionStorage,
    private val groupRepository: GroupRepository,
    private val tabEntryRepository: TabEntryRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        sessionStorage.authState
            .map { it != null }
            .distinctUntilChanged()
            .onEach { loggedIn ->
                if (loggedIn) {
                    syncGroupsAndEntries()
                } else {
                    groupRepository.deleteAllGroups()
                }
            }.launchIn(scope)
    }

    // Eagerly load each group's tab entries on login so Home/Overview balances are
    // correct immediately, instead of all groups appearing "Settled" until the user
    // opens each group detail (which is what triggers the per-group entry fetch).
    private suspend fun syncGroupsAndEntries() {
        groupRepository.fetchGroups().onSuccess { groups ->
            coroutineScope {
                groups
                    .map { group -> async { tabEntryRepository.fetchTabEntries(group.id) } }
                    .awaitAll()
            }
        }
    }
}
