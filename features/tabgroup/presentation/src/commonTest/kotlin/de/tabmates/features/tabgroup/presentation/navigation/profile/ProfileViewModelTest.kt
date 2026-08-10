package de.tabmates.features.tabgroup.presentation.navigation.profile

import app.cash.turbine.test
import de.tabmates.core.domain.auth.AuthInfo
import de.tabmates.core.domain.auth.SessionStorage
import de.tabmates.core.domain.auth.UserType
import de.tabmates.core.domain.auth.UserWithPendingEmail
import de.tabmates.core.domain.sync.PendingWrites
import de.tabmates.core.domain.util.Result
import de.tabmates.features.authentication.testing.FakeAuthService
import de.tabmates.features.notifications.domain.PushNotificationController
import de.tabmates.features.tabgroup.presentation.testing.FakePendingWrites
import de.tabmates.features.tabgroup.presentation.testing.FakePushNotificationController
import de.tabmates.features.tabgroup.presentation.testing.FakeSessionStorage
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
class ProfileViewModelTest {
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
    fun pendingWriteCountFlowsIntoState() =
        runTest(testDispatcher) {
            val pendingWrites = FakePendingWrites(initialCount = 0)
            val viewModel = createViewModel(pendingWrites = pendingWrites)
            subscribeToState(viewModel)

            assertEquals(0, viewModel.state.value.pendingWriteCount)

            pendingWrites.emit(3)
            advanceUntilIdle()

            assertEquals(3, viewModel.state.value.pendingWriteCount)
        }

    @Test
    fun signOutWithPendingWritesShowsDialogInsteadOfSigningOut() =
        runTest(testDispatcher) {
            val authService = FakeAuthService()
            val sessionStorage = FakeSessionStorage()
            val pushNotificationController = FakePushNotificationController()
            val viewModel =
                createViewModel(
                    authService = authService,
                    sessionStorage = sessionStorage,
                    pushNotificationController = pushNotificationController,
                    pendingWrites = FakePendingWrites(initialCount = 2),
                )
            subscribeToState(viewModel)

            viewModel.onSignOutClick()
            advanceUntilIdle()

            assertTrue(viewModel.state.value.showSignOutDialog)
            // Nothing was torn down — the warning has to be confirmed first.
            assertTrue(authService.logoutCalls.isEmpty())
            assertEquals(0, pushNotificationController.stopCalls)
            assertNotNull(sessionStorage.get())
        }

    @Test
    fun signOutWithoutPendingWritesSignsOutWithoutDialog() =
        runTest(testDispatcher) {
            val sessionStorage = FakeSessionStorage()
            val viewModel =
                createViewModel(
                    sessionStorage = sessionStorage,
                    pendingWrites = FakePendingWrites(initialCount = 0),
                )
            subscribeToState(viewModel)

            viewModel.events.test {
                viewModel.onSignOutClick()
                advanceUntilIdle()
                assertIs<ProfileEvent.SignedOut>(awaitItem())
                cancelAndConsumeRemainingEvents()
            }

            assertFalse(viewModel.state.value.showSignOutDialog)
            assertNull(sessionStorage.get())
        }

    @Test
    fun guestSignOutAlwaysConfirmsEvenWithNothingPending() =
        runTest(testDispatcher) {
            // A guest account only exists on this device and has no credentials to come back with,
            // so signing out destroys it — that never happens on a single tap.
            val authService = FakeAuthService()
            val sessionStorage = FakeSessionStorage(initial = guestAuthInfo())
            val viewModel =
                createViewModel(
                    sessionStorage = sessionStorage,
                    authService = authService,
                    pendingWrites = FakePendingWrites(initialCount = 0),
                )
            subscribeToState(viewModel)

            viewModel.onSignOutClick()
            advanceUntilIdle()

            assertTrue(viewModel.state.value.showSignOutDialog)
            assertTrue(authService.logoutCalls.isEmpty())
            assertNotNull(sessionStorage.get())
        }

    @Test
    fun pendingMigrationEmailFlowsIntoState() =
        runTest(testDispatcher) {
            val userWithPendingEmail =
                UserWithPendingEmail(
                    user = guestAuthInfo().user,
                    pendingEmail = "waiting@test.com",
                )
            val viewModel =
                createViewModel(
                    sessionStorage = FakeSessionStorage(initial = guestAuthInfo()),
                    authService = FakeAuthService(refreshAccountResult = Result.Success(userWithPendingEmail)),
                )
            subscribeToState(viewModel)

            assertEquals("waiting@test.com", viewModel.state.value.pendingMigrationEmail)
        }

    @Test
    fun pendingEmailOfARegisteredUserIsNotTreatedAsAMigration() =
        runTest(testDispatcher) {
            // The server reuses one field for both: a pending address change for a registered user
            // and a pending migration for a guest. Only the guest meaning belongs in this state.
            val userWithPendingEmail =
                UserWithPendingEmail(
                    user = FakeSessionStorage.DEFAULT_USER,
                    pendingEmail = "changed@test.com",
                )
            val viewModel =
                createViewModel(
                    sessionStorage = FakeSessionStorage(),
                    authService = FakeAuthService(refreshAccountResult = Result.Success(userWithPendingEmail)),
                )
            subscribeToState(viewModel)

            assertNull(viewModel.state.value.pendingMigrationEmail)
        }

    @Test
    fun confirmSignOutHidesDialogRevokesTokenAndClearsSession() =
        runTest(testDispatcher) {
            val authService = FakeAuthService()
            val sessionStorage = FakeSessionStorage()
            val pushNotificationController = FakePushNotificationController()
            val viewModel =
                createViewModel(
                    authService = authService,
                    sessionStorage = sessionStorage,
                    pushNotificationController = pushNotificationController,
                    pendingWrites = FakePendingWrites(initialCount = 2),
                )
            subscribeToState(viewModel)

            viewModel.onSignOutClick()
            advanceUntilIdle()
            assertTrue(viewModel.state.value.showSignOutDialog)

            viewModel.events.test {
                viewModel.onConfirmSignOut()
                advanceUntilIdle()
                assertIs<ProfileEvent.SignedOut>(awaitItem())
                cancelAndConsumeRemainingEvents()
            }

            assertFalse(viewModel.state.value.showSignOutDialog)
            assertEquals(1, pushNotificationController.stopCalls)
            // Revoked with the refresh token that was still in the session at the time.
            assertEquals(listOf("refresh"), authService.logoutCalls)
            assertNull(sessionStorage.get())
        }

    @Test
    fun confirmSignOutUnregistersDeviceWhileSessionIsStillValid() =
        runTest(testDispatcher) {
            val sessionStorage = FakeSessionStorage()
            val pushNotificationController = FakePushNotificationController()
            // The DELETE needs the bearer token, so the session must still be there when it runs.
            var sessionDuringStop: Boolean? = null
            pushNotificationController.onStop = { sessionDuringStop = sessionStorage.get() != null }
            val viewModel =
                createViewModel(
                    sessionStorage = sessionStorage,
                    pushNotificationController = pushNotificationController,
                )
            subscribeToState(viewModel)

            viewModel.events.test {
                viewModel.onConfirmSignOut()
                advanceUntilIdle()
                assertIs<ProfileEvent.SignedOut>(awaitItem())
                cancelAndConsumeRemainingEvents()
            }

            assertEquals(true, sessionDuringStop)
        }

    @Test
    fun dismissSignOutDialogHidesItAndKeepsSession() =
        runTest(testDispatcher) {
            val authService = FakeAuthService()
            val sessionStorage = FakeSessionStorage()
            val viewModel =
                createViewModel(
                    authService = authService,
                    sessionStorage = sessionStorage,
                    pendingWrites = FakePendingWrites(initialCount = 1),
                )
            subscribeToState(viewModel)

            viewModel.onSignOutClick()
            advanceUntilIdle()
            assertTrue(viewModel.state.value.showSignOutDialog)

            viewModel.onDismissSignOutDialog()
            advanceUntilIdle()

            assertFalse(viewModel.state.value.showSignOutDialog)
            assertTrue(authService.logoutCalls.isEmpty())
            assertNotNull(sessionStorage.get())
        }

    /**
     * `state` is `WhileSubscribed`, and `onSignOutClick` reads `state.value` to decide whether the
     * pending-write warning is needed — so the state has to be kept hot for the duration of a test.
     */
    private fun guestAuthInfo(): AuthInfo =
        FakeSessionStorage.DEFAULT_AUTH_INFO.copy(
            user =
                FakeSessionStorage.DEFAULT_USER.copy(
                    email = "",
                    hasVerifiedEmail = false,
                    userType = UserType.ANONYMOUS,
                ),
        )

    private fun TestScope.subscribeToState(viewModel: ProfileViewModel) {
        backgroundScope.launch { viewModel.state.collect { } }
        advanceUntilIdle()
    }

    private fun createViewModel(
        sessionStorage: SessionStorage = FakeSessionStorage(),
        authService: FakeAuthService = FakeAuthService(),
        pushNotificationController: PushNotificationController = FakePushNotificationController(),
        pendingWrites: PendingWrites = FakePendingWrites(),
    ): ProfileViewModel =
        ProfileViewModel(
            sessionStorage = sessionStorage,
            authService = authService,
            pushNotificationController = pushNotificationController,
            pendingWrites = pendingWrites,
        )
}
