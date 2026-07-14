package de.tabmates.features.tabgroup.presentation.navigation.profile

import androidx.compose.runtime.snapshots.Snapshot
import app.cash.turbine.test
import de.tabmates.core.domain.auth.AuthInfo
import de.tabmates.core.domain.auth.SessionStorage
import de.tabmates.core.domain.auth.User
import de.tabmates.core.domain.auth.UserType
import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.Result
import de.tabmates.core.presentation.util.UiText
import de.tabmates.core.presentation.util.toUiText
import de.tabmates.features.authentication.testing.FakeAuthService
import de.tabmates.features.tabgroup.presentation.testing.FakeSessionStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.delete_account_error_password_required
import tabmatesapp.features.tabgroup.presentation.generated.resources.delete_account_error_wrong_password
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
class DeleteAccountViewModelTest {
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
    fun registeredUserWithBlankPasswordSendsPasswordRequiredAndDoesNotCallService() =
        runTest(testDispatcher) {
            val authService = FakeAuthService()
            val viewModel = createViewModel(authService = authService)
            assertTrue(viewModel.state.value.isRegistered)

            viewModel.events.test {
                viewModel.onDeleteClick()
                val event = assertIs<DeleteAccountEvent.Error>(awaitItem())
                val message = assertIs<UiText.Resource>(event.message)
                assertEquals(Res.string.delete_account_error_password_required, message.id)
                cancelAndConsumeRemainingEvents()
            }
            assertFalse(viewModel.state.value.showConfirmDialog)
            assertTrue(authService.deleteAccountCalls.isEmpty())
        }

    @Test
    fun registeredUserWithPasswordShowsConfirmDialogWithoutCallingService() =
        runTest(testDispatcher) {
            val authService = FakeAuthService()
            val viewModel = createViewModel(authService = authService)
            setPassword(viewModel, "password")

            viewModel.onDeleteClick()

            assertTrue(viewModel.state.value.showConfirmDialog)
            assertTrue(authService.deleteAccountCalls.isEmpty())
        }

    @Test
    fun onConfirmDeleteSuccessCallsServiceClearsSessionAndSendsDeleted() =
        runTest(testDispatcher) {
            val authService = FakeAuthService(deleteAccountResult = Result.Success(Unit))
            val sessionStorage = FakeSessionStorage()
            val viewModel = createViewModel(authService = authService, sessionStorage = sessionStorage)
            setPassword(viewModel, "password")
            viewModel.onDeleteClick()

            viewModel.events.test {
                viewModel.onConfirmDelete()
                assertIs<DeleteAccountEvent.Deleted>(awaitItem())
                cancelAndConsumeRemainingEvents()
            }
            assertEquals(listOf<String?>("password"), authService.deleteAccountCalls)
            assertNull(sessionStorage.get())
            assertFalse(viewModel.state.value.showConfirmDialog)
        }

    @Test
    fun anonymousUserDeletesWithoutPassword() =
        runTest(testDispatcher) {
            val authService = FakeAuthService(deleteAccountResult = Result.Success(Unit))
            val sessionStorage = anonymousSessionStorage()
            val viewModel = createViewModel(authService = authService, sessionStorage = sessionStorage)
            assertFalse(viewModel.state.value.isRegistered)

            // No password required: clicking delete goes straight to the confirm dialog.
            viewModel.onDeleteClick()
            assertTrue(viewModel.state.value.showConfirmDialog)

            viewModel.events.test {
                viewModel.onConfirmDelete()
                assertIs<DeleteAccountEvent.Deleted>(awaitItem())
                cancelAndConsumeRemainingEvents()
            }
            assertEquals(listOf<String?>(null), authService.deleteAccountCalls)
            assertNull(sessionStorage.get())
        }

    @Test
    fun onConfirmDeleteUnauthorizedMapsToWrongPasswordAndKeepsSession() =
        runTest(testDispatcher) {
            val authService = FakeAuthService(deleteAccountResult = Result.Failure(DataError.Remote.UNAUTHORIZED))
            val sessionStorage = FakeSessionStorage()
            val viewModel = createViewModel(authService = authService, sessionStorage = sessionStorage)
            setPassword(viewModel, "wrong")
            viewModel.onDeleteClick()

            viewModel.events.test {
                viewModel.onConfirmDelete()
                val event = assertIs<DeleteAccountEvent.Error>(awaitItem())
                val message = assertIs<UiText.Resource>(event.message)
                assertEquals(Res.string.delete_account_error_wrong_password, message.id)
                cancelAndConsumeRemainingEvents()
            }
            assertFalse(viewModel.state.value.isSubmitting)
            assertNotNull(sessionStorage.get())
        }

    @Test
    fun onConfirmDeleteOtherErrorUsesGenericToUiText() =
        runTest(testDispatcher) {
            val error = DataError.Remote.SERVER_ERROR
            val authService = FakeAuthService(deleteAccountResult = Result.Failure(error))
            val viewModel = createViewModel(authService = authService)
            setPassword(viewModel, "password")
            viewModel.onDeleteClick()

            viewModel.events.test {
                viewModel.onConfirmDelete()
                val event = assertIs<DeleteAccountEvent.Error>(awaitItem())
                val message = assertIs<UiText.Resource>(event.message)
                val expected = assertIs<UiText.Resource>(error.toUiText())
                assertEquals(expected.id, message.id)
                cancelAndConsumeRemainingEvents()
            }
            assertFalse(viewModel.state.value.isSubmitting)
        }

    @Test
    fun onDismissDialogHidesConfirmDialog() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            setPassword(viewModel, "password")
            viewModel.onDeleteClick()
            assertTrue(viewModel.state.value.showConfirmDialog)

            viewModel.onDismissDialog()
            assertFalse(viewModel.state.value.showConfirmDialog)
        }

    @Test
    fun onTogglePasswordVisibilityFlipsState() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            assertFalse(viewModel.state.value.isPasswordVisible)

            viewModel.onTogglePasswordVisibility()
            assertTrue(viewModel.state.value.isPasswordVisible)

            viewModel.onTogglePasswordVisibility()
            assertFalse(viewModel.state.value.isPasswordVisible)
        }

    private fun createViewModel(
        authService: FakeAuthService = FakeAuthService(),
        sessionStorage: SessionStorage = FakeSessionStorage(),
    ): DeleteAccountViewModel {
        return DeleteAccountViewModel(
            authService = authService,
            sessionStorage = sessionStorage,
        )
    }

    private fun anonymousSessionStorage(): FakeSessionStorage {
        return FakeSessionStorage(
            initial =
                AuthInfo(
                    accessToken = "access",
                    refreshToken = "refresh",
                    user =
                        User(
                            id = "guest-1",
                            email = "",
                            username = "guest",
                            hasVerifiedEmail = false,
                            userType = UserType.ANONYMOUS,
                        ),
                ),
        )
    }

    private fun TestScope.setPassword(
        viewModel: DeleteAccountViewModel,
        password: String,
    ) {
        viewModel.passwordState.edit { replace(0, length, password) }
        Snapshot.sendApplyNotifications()
        advanceUntilIdle()
    }
}
