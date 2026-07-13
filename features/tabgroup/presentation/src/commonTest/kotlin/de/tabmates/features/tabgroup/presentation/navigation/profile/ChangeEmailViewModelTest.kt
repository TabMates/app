package de.tabmates.features.tabgroup.presentation.navigation.profile

import androidx.compose.runtime.snapshots.Snapshot
import app.cash.turbine.test
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
import tabmatesapp.features.tabgroup.presentation.generated.resources.change_email_error_in_use
import tabmatesapp.features.tabgroup.presentation.generated.resources.change_email_error_invalid
import tabmatesapp.features.tabgroup.presentation.generated.resources.change_email_error_password_required
import tabmatesapp.features.tabgroup.presentation.generated.resources.change_email_error_same
import tabmatesapp.features.tabgroup.presentation.generated.resources.change_email_error_wrong_password
import tabmatesapp.features.tabgroup.presentation.generated.resources.change_email_verification_sent
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ChangeEmailViewModelTest {
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
    fun onSaveWithInvalidEmailSendsInvalidErrorEvent() =
        runTest(testDispatcher) {
            val authService = FakeAuthService()
            val viewModel = createViewModel(authService = authService)
            fillFields(viewModel, email = "not-an-email")

            viewModel.events.test {
                viewModel.onSave()
                val event = assertIs<ChangeEmailEvent.Error>(awaitItem())
                val message = assertIs<UiText.Resource>(event.message)
                assertEquals(Res.string.change_email_error_invalid, message.id)
                cancelAndConsumeRemainingEvents()
            }
            assertTrue(authService.changeEmailCalls.isEmpty())
        }

    @Test
    fun onSaveWithSameEmailAsCurrentSendsSameErrorEvent() =
        runTest(testDispatcher) {
            val authService = FakeAuthService()
            val viewModel = createViewModel(authService = authService)
            // Current email is FakeSessionStorage.DEFAULT_USER.email; check is case-insensitive.
            fillFields(viewModel, email = "User@Test.com")

            viewModel.events.test {
                viewModel.onSave()
                val event = assertIs<ChangeEmailEvent.Error>(awaitItem())
                val message = assertIs<UiText.Resource>(event.message)
                assertEquals(Res.string.change_email_error_same, message.id)
                cancelAndConsumeRemainingEvents()
            }
            assertTrue(authService.changeEmailCalls.isEmpty())
        }

    @Test
    fun onSaveWithBlankPasswordSendsPasswordRequiredErrorEvent() =
        runTest(testDispatcher) {
            val authService = FakeAuthService()
            val viewModel = createViewModel(authService = authService)
            fillFields(viewModel, password = "")

            viewModel.events.test {
                viewModel.onSave()
                val event = assertIs<ChangeEmailEvent.Error>(awaitItem())
                val message = assertIs<UiText.Resource>(event.message)
                assertEquals(Res.string.change_email_error_password_required, message.id)
                cancelAndConsumeRemainingEvents()
            }
            assertTrue(authService.changeEmailCalls.isEmpty())
        }

    @Test
    fun onSaveSuccessSendsSavedEventWithNewEmail() =
        runTest(testDispatcher) {
            val authService = FakeAuthService(changeEmailResult = Result.Success(Unit))
            val viewModel = createViewModel(authService = authService)
            fillFields(viewModel)

            viewModel.events.test {
                viewModel.onSave()
                val event = assertIs<ChangeEmailEvent.Saved>(awaitItem())
                val message = assertIs<UiText.Resource>(event.message)
                assertEquals(Res.string.change_email_verification_sent, message.id)
                assertTrue(message.args.contains("new@test.com"))
                cancelAndConsumeRemainingEvents()
            }
            assertEquals(listOf("new@test.com" to "password"), authService.changeEmailCalls)
            assertFalse(viewModel.state.value.isSubmitting)
        }

    @Test
    fun onSaveFailureWithConflictMapsToEmailInUseError() =
        runTest(testDispatcher) {
            val viewModel =
                createViewModel(
                    authService = FakeAuthService(changeEmailResult = Result.Failure(DataError.Remote.CONFLICT)),
                )
            fillFields(viewModel)

            viewModel.events.test {
                viewModel.onSave()
                val event = assertIs<ChangeEmailEvent.Error>(awaitItem())
                val message = assertIs<UiText.Resource>(event.message)
                assertEquals(Res.string.change_email_error_in_use, message.id)
                cancelAndConsumeRemainingEvents()
            }
            assertFalse(viewModel.state.value.isSubmitting)
        }

    @Test
    fun onSaveFailureWithUnauthorizedMapsToWrongPasswordError() =
        runTest(testDispatcher) {
            val error = DataError.Remote.UNAUTHORIZED
            val viewModel =
                createViewModel(
                    authService = FakeAuthService(changeEmailResult = Result.Failure(error)),
                )
            fillFields(viewModel)

            viewModel.events.test {
                viewModel.onSave()
                val event = assertIs<ChangeEmailEvent.Error>(awaitItem())
                val message = assertIs<UiText.Resource>(event.message)
                assertEquals(Res.string.change_email_error_wrong_password, message.id)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun onSaveFailureWithOtherErrorUsesGenericToUiText() =
        runTest(testDispatcher) {
            val error = DataError.Remote.SERVER_ERROR
            val viewModel =
                createViewModel(
                    authService = FakeAuthService(changeEmailResult = Result.Failure(error)),
                )
            fillFields(viewModel)

            viewModel.events.test {
                viewModel.onSave()
                val event = assertIs<ChangeEmailEvent.Error>(awaitItem())
                val message = assertIs<UiText.Resource>(event.message)
                val expected = assertIs<UiText.Resource>(error.toUiText())
                assertEquals(expected.id, message.id)
                cancelAndConsumeRemainingEvents()
            }
            assertFalse(viewModel.state.value.isSubmitting)
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

    private fun createViewModel(authService: FakeAuthService = FakeAuthService()): ChangeEmailViewModel {
        return ChangeEmailViewModel(
            authService = authService,
            sessionStorage = FakeSessionStorage(),
        )
    }

    private fun TestScope.fillFields(
        viewModel: ChangeEmailViewModel,
        email: String = "new@test.com",
        password: String = "password",
    ) {
        viewModel.newEmailState.edit { replace(0, length, email) }
        viewModel.passwordState.edit { replace(0, length, password) }
        Snapshot.sendApplyNotifications()
        advanceUntilIdle()
    }
}
