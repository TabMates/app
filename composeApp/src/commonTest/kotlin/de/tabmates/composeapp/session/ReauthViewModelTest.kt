package de.tabmates.composeapp.session

import androidx.compose.runtime.snapshots.Snapshot
import app.cash.turbine.test
import de.tabmates.core.domain.auth.AuthInfo
import de.tabmates.core.domain.auth.StaleSession
import de.tabmates.core.domain.auth.UserType
import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.Result
import de.tabmates.features.authentication.testing.FakeAuthService
import de.tabmates.features.authentication.testing.FakeSessionStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ReauthViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun registeredAccountPrefillsAndLocksTheEmail() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()

            assertEquals("lena@example.com", viewModel.state.value.emailTextFieldState.text.toString())
            assertTrue(viewModel.state.value.isEmailLocked)
            assertFalse(viewModel.state.value.isGuest)
        }

    @Test
    fun emailChangedSessionLeavesTheFieldEmptyAndUnlocked() =
        runTest(testDispatcher) {
            val viewModel = createViewModel(stale = staleSession(email = null))

            assertEquals("", viewModel.state.value.emailTextFieldState.text.toString())
            assertFalse(viewModel.state.value.isEmailLocked)
        }

    @Test
    fun guestSessionIsMarkedAsSuch() =
        runTest(testDispatcher) {
            val viewModel = createViewModel(stale = staleSession(email = null, userType = UserType.ANONYMOUS))

            assertTrue(viewModel.state.value.isGuest)
        }

    @Test
    fun signingBackInAsTheSameAccountKeepsLocalDataAndClearsTheStaleRecord() =
        runTest(testDispatcher) {
            val staleSessionStore = FakeStaleSessionStore(staleSession(userId = "user-1"))
            val localDataResetter = FakeLocalDataResetter()
            val viewModel =
                createViewModel(
                    authService = FakeAuthService(loginResult = Result.Success(authInfo("user-1"))),
                    staleSessionStore = staleSessionStore,
                    localDataResetter = localDataResetter,
                )
            fillCredentials(viewModel)

            viewModel.events.test {
                viewModel.onSignIn()
                assertIs<ReauthEvent.ReauthSucceeded>(awaitItem())
            }

            assertNull(staleSessionStore.get())
            assertEquals(0, localDataResetter.resetCalls)
        }

    @Test
    fun signingInAsAnotherAccountIsRejectedWithoutTouchingLocalData() =
        runTest(testDispatcher) {
            val sessionStorage = FakeSessionStorage(authInfo("someone-else"))
            val staleSessionStore = FakeStaleSessionStore(staleSession(userId = "user-1"))
            val localDataResetter = FakeLocalDataResetter()
            val viewModel =
                createViewModel(
                    authService = FakeAuthService(loginResult = Result.Success(authInfo("someone-else"))),
                    sessionStorage = sessionStorage,
                    staleSessionStore = staleSessionStore,
                    localDataResetter = localDataResetter,
                )
            fillCredentials(viewModel)

            viewModel.events.test {
                viewModel.onSignIn()
                assertIs<ReauthEvent.ReauthFailed>(awaitItem())
            }

            // Session dropped again, so the app stays expired rather than adopting the other
            // account — and the previous account's data and stale record are still there.
            assertNull(sessionStorage.get())
            assertNotNull(staleSessionStore.get())
            assertEquals(0, localDataResetter.resetCalls)
        }

    @Test
    fun invalidCredentialsSurfaceAsAFailureAndKeepTheStaleRecord() =
        runTest(testDispatcher) {
            val staleSessionStore = FakeStaleSessionStore(staleSession())
            val viewModel =
                createViewModel(
                    authService = FakeAuthService(loginResult = Result.Failure(DataError.Remote.UNAUTHORIZED)),
                    staleSessionStore = staleSessionStore,
                )
            fillCredentials(viewModel)

            viewModel.events.test {
                viewModel.onSignIn()
                assertIs<ReauthEvent.ReauthFailed>(awaitItem())
            }

            assertNotNull(staleSessionStore.get())
        }

    @Test
    fun confirmingAnAccountSwitchWipesLocalDataAndTheStaleRecord() =
        runTest(testDispatcher) {
            val staleSessionStore = FakeStaleSessionStore(staleSession())
            val localDataResetter = FakeLocalDataResetter()
            val authService = FakeAuthService(loginResult = Result.Success(authInfo()))
            val viewModel =
                createViewModel(
                    authService = authService,
                    staleSessionStore = staleSessionStore,
                    localDataResetter = localDataResetter,
                )

            viewModel.onSwitchAccountClick()
            assertTrue(viewModel.state.value.showSwitchAccountDialog)

            viewModel.onConfirmSwitchAccount()
            advanceUntilIdle()

            assertEquals(1, localDataResetter.resetCalls)
            assertNull(staleSessionStore.get())
            assertFalse(viewModel.state.value.showSwitchAccountDialog)
            // Nothing calls logout on this path, so the token of the account being left behind is
            // only dropped here — otherwise it authenticates the next account's first sync.
            assertEquals(1, authService.clearCachedTokensCalls)
        }

    @Test
    fun dismissingTheSwitchDialogChangesNothing() =
        runTest(testDispatcher) {
            val localDataResetter = FakeLocalDataResetter()
            val viewModel = createViewModel(localDataResetter = localDataResetter)

            viewModel.onSwitchAccountClick()
            viewModel.onDismissSwitchAccountDialog()
            advanceUntilIdle()

            assertFalse(viewModel.state.value.showSwitchAccountDialog)
            assertEquals(0, localDataResetter.resetCalls)
        }

    @Test
    fun pendingWriteCountIsSurfaced() =
        runTest(testDispatcher) {
            val viewModel = createViewModel(pendingWrites = FakePendingWrites(3))

            assertEquals(3, viewModel.state.value.pendingWriteCount)
        }

    private fun TestScope.createViewModel(
        authService: FakeAuthService = FakeAuthService(loginResult = Result.Success(authInfo())),
        sessionStorage: FakeSessionStorage = FakeSessionStorage(),
        stale: StaleSession = staleSession(),
        staleSessionStore: FakeStaleSessionStore = FakeStaleSessionStore(stale),
        localDataResetter: FakeLocalDataResetter = FakeLocalDataResetter(),
        pendingWrites: FakePendingWrites = FakePendingWrites(),
    ): ReauthViewModel {
        val viewModel =
            ReauthViewModel(
                authService = authService,
                sessionStorage = sessionStorage,
                staleSessionStore = staleSessionStore,
                localDataResetter = localDataResetter,
                pendingWrites = pendingWrites,
            )
        // `stateIn(WhileSubscribed)` only runs `onStart` once something collects.
        backgroundScope.launch { viewModel.state.collect { } }
        advanceUntilIdle()
        return viewModel
    }

    private fun TestScope.fillCredentials(viewModel: ReauthViewModel) {
        viewModel.state.value.emailTextFieldState.edit {
            replace(0, length, "lena@example.com")
        }
        viewModel.state.value.passwordTextFieldState.edit {
            replace(0, length, "password123")
        }
        Snapshot.sendApplyNotifications()
        advanceUntilIdle()
    }

    private fun authInfo(userId: String = "user-1") =
        AuthInfo(
            accessToken = "token",
            refreshToken = "refresh",
            user = user(id = userId),
        )
}
