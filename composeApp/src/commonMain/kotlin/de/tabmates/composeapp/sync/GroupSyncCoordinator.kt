package de.tabmates.composeapp.sync

import de.tabmates.core.domain.auth.SessionStorage
import de.tabmates.core.domain.sync.ActivityCursorStore
import de.tabmates.core.domain.sync.LastServerContactStore
import de.tabmates.core.domain.sync.PendingTabEntryBackfillStore
import de.tabmates.core.domain.sync.SyncCursorStore
import de.tabmates.core.domain.util.onSuccess
import de.tabmates.features.tabgroup.domain.activity.ActivityRepository
import de.tabmates.features.tabgroup.domain.group.GroupRepository
import de.tabmates.features.tabgroup.domain.sync.SyncRepository
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
    private val syncRepository: SyncRepository,
    private val activityRepository: ActivityRepository,
    private val groupRepository: GroupRepository,
    private val syncCursorStore: SyncCursorStore,
    private val activityCursorStore: ActivityCursorStore,
    private val lastServerContactStore: LastServerContactStore,
    private val pendingBackfillStore: PendingTabEntryBackfillStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        sessionStorage.authState
            .map { it != null }
            .distinctUntilChanged()
            .onEach { loggedIn ->
                if (loggedIn) {
                    // One `/api/sync` call pulls groups and all their tab entries (full snapshot on
                    // first login, delta once a cursor exists), replacing the per-group fetch loop.
                    // Activity events foreign-key onto their group, so their mirror is only safe to
                    // write once those groups exist locally — hence the chain rather than a parallel
                    // coordinator.
                    syncRepository.sync().onSuccess { activityRepository.sync() }
                } else {
                    syncCursorStore.clear()
                    activityCursorStore.clear()
                    lastServerContactStore.clear()
                    pendingBackfillStore.clearAll()
                    groupRepository.deleteAllGroups()
                }
            }.launchIn(scope)
    }
}
