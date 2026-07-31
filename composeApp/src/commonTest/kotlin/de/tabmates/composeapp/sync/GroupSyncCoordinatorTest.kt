package de.tabmates.composeapp.sync

import de.tabmates.composeapp.session.FakeLocalDataResetter
import de.tabmates.composeapp.session.FakeStaleSessionStore
import de.tabmates.composeapp.session.staleSession
import de.tabmates.composeapp.session.user
import de.tabmates.core.domain.auth.AuthInfo
import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.EmptyResult
import de.tabmates.core.domain.util.Result
import de.tabmates.features.authentication.testing.FakeSessionStorage
import de.tabmates.features.tabgroup.domain.activity.ActivityFeedItem
import de.tabmates.features.tabgroup.domain.activity.ActivityRepository
import de.tabmates.features.tabgroup.domain.sync.SyncRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class GroupSyncCoordinatorTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @Test
    fun expiredSessionKeepsLocalData() =
        runTest(testDispatcher) {
            val sessionStorage = FakeSessionStorage(authInfo("user-1"))
            val staleSessionStore = FakeStaleSessionStore()
            val localDataResetter = FakeLocalDataResetter()
            createCoordinator(sessionStorage, staleSessionStore, localDataResetter)

            // What an expiry looks like: the account is recorded, then the session is dropped.
            staleSessionStore.set(staleSession(userId = "user-1"))
            sessionStorage.set(null)
            advanceUntilIdle()

            assertEquals(0, localDataResetter.resetCalls)
        }

    @Test
    fun deliberateSignOutWipesLocalData() =
        runTest(testDispatcher) {
            val sessionStorage = FakeSessionStorage(authInfo("user-1"))
            val staleSessionStore = FakeStaleSessionStore()
            val localDataResetter = FakeLocalDataResetter()
            createCoordinator(sessionStorage, staleSessionStore, localDataResetter)

            // No stale record — the session simply went away.
            sessionStorage.set(null)
            advanceUntilIdle()

            assertEquals(1, localDataResetter.resetCalls)
        }

    @Test
    fun signingInAsAnotherAccountWhileExpiredDoesNotSync() =
        runTest(testDispatcher) {
            val sessionStorage = FakeSessionStorage()
            val staleSessionStore = FakeStaleSessionStore(staleSession(userId = "user-1"))
            val localDataResetter = FakeLocalDataResetter()
            val syncRepository = FakeSyncRepository()
            createCoordinator(
                sessionStorage,
                staleSessionStore,
                localDataResetter,
                syncRepository,
            )

            sessionStorage.set(authInfo("someone-else"))
            advanceUntilIdle()

            // Syncing would pull the other account's groups into a database that still belongs to
            // the expired one.
            assertEquals(0, syncRepository.syncCalls)
            assertEquals(0, localDataResetter.resetCalls)
        }

    @Test
    fun signingBackInAsTheSameAccountSyncsAndClearsTheStaleRecord() =
        runTest(testDispatcher) {
            val sessionStorage = FakeSessionStorage()
            val staleSessionStore = FakeStaleSessionStore(staleSession(userId = "user-1"))
            val localDataResetter = FakeLocalDataResetter()
            val syncRepository = FakeSyncRepository()
            createCoordinator(
                sessionStorage,
                staleSessionStore,
                localDataResetter,
                syncRepository,
            )

            sessionStorage.set(authInfo("user-1"))
            advanceUntilIdle()

            assertEquals(1, syncRepository.syncCalls)
            assertEquals(0, localDataResetter.resetCalls)
            assertEquals(null, staleSessionStore.get())
        }

    private fun TestScope.createCoordinator(
        sessionStorage: FakeSessionStorage,
        staleSessionStore: FakeStaleSessionStore,
        localDataResetter: FakeLocalDataResetter,
        syncRepository: FakeSyncRepository = FakeSyncRepository(),
    ) = GroupSyncCoordinator(
        sessionStorage = sessionStorage,
        syncRepository = syncRepository,
        activityRepository = FakeActivityRepository(),
        staleSessionStore = staleSessionStore,
        localDataResetter = localDataResetter,
        scope = backgroundScope,
    )

    private fun authInfo(userId: String) =
        AuthInfo(
            accessToken = "token",
            refreshToken = "refresh",
            user = user(id = userId),
        )
}

private class FakeSyncRepository : SyncRepository {
    var syncCalls: Int = 0
        private set

    override suspend fun sync(): EmptyResult<DataError.Remote> {
        syncCalls += 1
        return Result.Success(Unit)
    }
}

private class FakeActivityRepository : ActivityRepository {
    override suspend fun sync(): EmptyResult<DataError.Remote> = Result.Success(Unit)

    override fun observeAccountFeed(limit: Int): Flow<List<ActivityFeedItem>> = emptyFlow()

    override fun observeGroupFeed(
        groupId: String,
        limit: Int,
    ): Flow<List<ActivityFeedItem>> = emptyFlow()
}
