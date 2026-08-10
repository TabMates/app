package de.tabmates.features.tabgroup.presentation.navigation.settings

import de.tabmates.core.domain.auth.AuthInfo
import de.tabmates.core.domain.auth.SessionStorage
import de.tabmates.core.domain.auth.UserType
import de.tabmates.core.domain.auth.UserWithPendingEmail
import de.tabmates.core.domain.preferences.AppPreferencesRepository
import de.tabmates.core.domain.preferences.ThemeMode
import de.tabmates.core.domain.util.Result
import de.tabmates.features.authentication.testing.FakeAuthService
import de.tabmates.features.notifications.domain.NotificationPermissionController
import de.tabmates.features.notifications.domain.NotificationPermissionStatus
import de.tabmates.features.tabgroup.presentation.testing.FakeAppPreferencesRepository
import de.tabmates.features.tabgroup.presentation.testing.FakeNotificationPermissionController
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
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
    fun accountFlowsIntoState() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            subscribeToState(viewModel)

            val state = viewModel.state.value
            assertFalse(state.isLoading)
            assertEquals(FakeSessionStorage.DEFAULT_USER.username, state.username)
            assertEquals(FakeSessionStorage.DEFAULT_USER.email, state.email)
            assertEquals(
                FakeSessionStorage.DEFAULT_USER.username
                    .take(2)
                    .uppercase(),
                state.initials,
            )
            assertTrue(state.isRegistered)
        }

    @Test
    fun themeSelectionIsPersisted() =
        runTest(testDispatcher) {
            val preferences = FakeAppPreferencesRepository()
            val viewModel = createViewModel(appPreferencesRepository = preferences)
            subscribeToState(viewModel)

            viewModel.onThemeSelected(ThemeMode.DARK)
            advanceUntilIdle()

            assertEquals(listOf(ThemeMode.DARK), preferences.setThemeModeCalls)
            assertEquals(ThemeMode.DARK, viewModel.state.value.themeMode)
        }

    @Test
    fun notificationsToggleIsPersisted() =
        runTest(testDispatcher) {
            val preferences = FakeAppPreferencesRepository()
            val viewModel = createViewModel(appPreferencesRepository = preferences)
            subscribeToState(viewModel)

            viewModel.onNotificationsToggle(false)
            advanceUntilIdle()

            assertEquals(listOf(false), preferences.setNotificationsEnabledCalls)
            assertFalse(viewModel.state.value.notificationsEnabled)
        }

    @Test
    fun deniedOsPermissionBlocksTheToggle() =
        runTest(testDispatcher) {
            // The preference can stay on while the OS says no — the screen has to show the OS truth.
            val permissions =
                FakeNotificationPermissionController(
                    initialStatus = NotificationPermissionStatus.DENIED,
                )
            val viewModel = createViewModel(notificationPermissionController = permissions)
            subscribeToState(viewModel)

            assertTrue(viewModel.state.value.notificationsPermissionBlocked)
            assertTrue(viewModel.state.value.notificationsEnabled)
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
            assertFalse(viewModel.state.value.isRegistered)
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

    private fun guestAuthInfo(): AuthInfo =
        FakeSessionStorage.DEFAULT_AUTH_INFO.copy(
            user =
                FakeSessionStorage.DEFAULT_USER.copy(
                    email = "",
                    hasVerifiedEmail = false,
                    userType = UserType.ANONYMOUS,
                ),
        )

    /** `state` is `WhileSubscribed`, so it has to be kept hot for the duration of a test. */
    private fun TestScope.subscribeToState(viewModel: SettingsViewModel) {
        backgroundScope.launch { viewModel.state.collect { } }
        advanceUntilIdle()
    }

    private fun createViewModel(
        sessionStorage: SessionStorage = FakeSessionStorage(),
        appPreferencesRepository: AppPreferencesRepository = FakeAppPreferencesRepository(),
        authService: FakeAuthService = FakeAuthService(),
        notificationPermissionController: NotificationPermissionController =
            FakeNotificationPermissionController(),
    ): SettingsViewModel =
        SettingsViewModel(
            appPreferencesRepository = appPreferencesRepository,
            authService = authService,
            notificationPermissionController = notificationPermissionController,
            sessionStorage = sessionStorage,
        )
}
