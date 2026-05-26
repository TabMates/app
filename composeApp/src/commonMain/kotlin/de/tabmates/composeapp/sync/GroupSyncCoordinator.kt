package de.tabmates.composeapp.sync

import de.tabmates.core.domain.auth.SessionStorage
import de.tabmates.features.tabgroup.domain.group.GroupRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.koin.core.annotation.Single

@Single
class GroupSyncCoordinator(
    sessionStorage: SessionStorage,
    private val groupRepository: GroupRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        sessionStorage.authState
            .map { it != null }
            .distinctUntilChanged()
            .onEach { loggedIn ->
                if (loggedIn) {
                    groupRepository.fetchGroups()
                } else {
                    groupRepository.deleteAllGroups()
                }
            }.launchIn(scope)
    }
}
