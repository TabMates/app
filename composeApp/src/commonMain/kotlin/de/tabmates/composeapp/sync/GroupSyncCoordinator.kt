package de.tabmates.composeapp.sync

import de.tabmates.core.data.di.APPLICATION_SCOPE
import de.tabmates.core.domain.auth.SessionStorage
import de.tabmates.core.domain.auth.StaleSessionStore
import de.tabmates.core.domain.logging.TabMatesLogger
import de.tabmates.core.domain.sync.LocalDataResetter
import de.tabmates.core.domain.util.onFailure
import de.tabmates.core.domain.util.onSuccess
import de.tabmates.features.tabgroup.domain.activity.ActivityRepository
import de.tabmates.features.tabgroup.domain.sync.SyncRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Single
class GroupSyncCoordinator(
    sessionStorage: SessionStorage,
    private val syncRepository: SyncRepository,
    private val activityRepository: ActivityRepository,
    private val staleSessionStore: StaleSessionStore,
    private val localDataResetter: LocalDataResetter,
    private val logger: TabMatesLogger,
    @Named(APPLICATION_SCOPE) scope: CoroutineScope,
) {
    init {
        sessionStorage.authState
            .map { it?.user?.id }
            .distinctUntilChanged()
            .onEach { userId ->
                if (userId != null) {
                    onSignedIn(userId)
                } else {
                    onSignedOut()
                }
            }.catch { throwable ->
                // Without this the collector dies on the first unexpected throw and never
                // restarts: the app would keep looking signed in while nothing ever synced again.
                logger.error(TAG, "Session sync coordination failed", throwable)
            }.launchIn(scope)
    }

    private suspend fun onSignedIn(userId: String) {
        // Local data belongs to whoever last synced it. Signing in as someone else while an
        // expired session still owns that data must not sync: pulling this account's groups into
        // the other's database would mix the two. The re-auth screen rejects the mismatch and
        // signs straight back out; switching accounts for real goes through an explicit,
        // warned wipe, which clears the record below and leaves nothing to collide with.
        val staleUserId = staleSessionStore.get()?.userId
        if (staleUserId != null && staleUserId != userId) return

        staleSessionStore.clear()

        // One `/api/sync` call pulls groups and all their tab entries (full snapshot on
        // first login, delta once a cursor exists), replacing the per-group fetch loop.
        // Activity events foreign-key onto their group, so their mirror is only safe to
        // write once those groups exist locally — hence the chain rather than a parallel
        // coordinator.
        syncRepository
            .sync()
            .onSuccess {
                activityRepository
                    .sync()
                    .onFailure { error -> logger.warning(TAG, "Login activity sync failed: $error") }
            }.onFailure { error -> logger.warning(TAG, "Login sync failed: $error") }
    }

    private suspend fun onSignedOut() {
        // An expired session is not a sign-out. The account is recorded, its unsynced writes are
        // still queued, and it can be asked back in — so everything stays exactly where it is
        // until the user either signs back in or explicitly chooses a different account.
        if (staleSessionStore.get() != null) return

        localDataResetter.resetLocalData()
    }

    private companion object {
        private const val TAG = "GroupSyncCoordinator"
    }
}
