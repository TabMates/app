package de.tabmates.features.tabgroup.presentation.navigation.profile

import androidx.compose.runtime.snapshots.Snapshot
import app.cash.turbine.test
import de.tabmates.core.domain.auth.UserType
import de.tabmates.core.domain.auth.UserWithPendingEmail
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
import tabmatesapp.features.tabgroup.presentation.generated.resources.upgrade_account_email_invalid
import tabmatesapp.features.tabgroup.presentation.generated.resources.upgrade_account_error_email_in_use
import tabmatesapp.features.tabgroup.presentation.generated.resources.upgrade_account_password_mismatch
import tabmatesapp.features.tabgroup.presentation.generated.resources.upgrade_account_password_requirements
import tabmatesapp.features.tabgroup.presentation.generated.resources.upgrade_account_resend_needs_details
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class UpgradeAccountViewModelTest {
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
    fun submitWithInvalidEmailShowsFieldErrorAndDoesNotCallService() =
        runTest(testDispatcher) {
            val authService = FakeAuthService()
            val viewModel = createViewModel(authService)
            fillFields(viewModel, email = "not-an-email")

            viewModel.onSubmit()
            advanceUntilIdle()

            val error = assertIs<UiText.Resource>(viewModel.state.value.emailError)
            assertEquals(Res.string.upgrade_account_email_invalid, error.id)
            assertTrue(authService.migrateToRegisteredCalls.isEmpty())
            assertNull(viewModel.state.value.pendingEmail)
        }

    @Test
    fun submitWithWeakPasswordShowsRequirementsError() =
        runTest(testDispatcher) {
            val authService = FakeAuthService()
            val viewModel = createViewModel(authService)
            fillFields(viewModel, password = "short", confirmPassword = "short")

            viewModel.onSubmit()
            advanceUntilIdle()

            val error = assertIs<UiText.Resource>(viewModel.state.value.passwordError)
            assertEquals(Res.string.upgrade_account_password_requirements, error.id)
            assertTrue(authService.migrateToRegisteredCalls.isEmpty())
        }

    @Test
    fun submitWithMismatchedConfirmationShowsMismatchError() =
        runTest(testDispatcher) {
            val authService = FakeAuthService()
            val viewModel = createViewModel(authService)
            fillFields(viewModel, confirmPassword = "something-else1")

            viewModel.onSubmit()
            advanceUntilIdle()

            val error = assertIs<UiText.Resource>(viewModel.state.value.confirmPasswordError)
            assertEquals(Res.string.upgrade_account_password_mismatch, error.id)
            assertTrue(authService.migrateToRegisteredCalls.isEmpty())
        }

    @Test
    fun successfulSubmitMovesToPendingPhaseWithTrimmedEmail() =
        runTest(testDispatcher) {
            val authService = FakeAuthService(migrateToRegisteredResult = Result.Success(Unit))
            val viewModel = createViewModel(authService)
            fillFields(viewModel, email = "  new@test.com  ")

            viewModel.events.test {
                viewModel.onSubmit()
                advanceUntilIdle()
                val event = assertIs<UpgradeAccountEvent.VerificationSent>(awaitItem())
                assertEquals("new@test.com", event.email)
                cancelAndConsumeRemainingEvents()
            }

            assertEquals(listOf("new@test.com" to "password1"), authService.migrateToRegisteredCalls)
            assertEquals("new@test.com", viewModel.state.value.pendingEmail)
            assertFalse(viewModel.state.value.isSubmitting)
        }

    @Test
    fun conflictMapsToEmailInUseAndStaysOnTheForm() =
        runTest(testDispatcher) {
            val authService =
                FakeAuthService(migrateToRegisteredResult = Result.Failure(DataError.Remote.CONFLICT))
            val viewModel = createViewModel(authService)
            fillFields(viewModel)

            viewModel.events.test {
                viewModel.onSubmit()
                advanceUntilIdle()
                val event = assertIs<UpgradeAccountEvent.Error>(awaitItem())
                val message = assertIs<UiText.Resource>(event.message)
                assertEquals(Res.string.upgrade_account_error_email_in_use, message.id)
                cancelAndConsumeRemainingEvents()
            }

            // Staying on the form is the point: the address has to be corrected before a link
            // can be sent, and the anonymous session is untouched either way.
            assertNull(viewModel.state.value.pendingEmail)
            assertFalse(viewModel.state.value.isSubmitting)
        }

    @Test
    fun otherFailuresUseTheGenericMapping() =
        runTest(testDispatcher) {
            val error = DataError.Remote.SERVER_ERROR
            val viewModel = createViewModel(FakeAuthService(migrateToRegisteredResult = Result.Failure(error)))
            fillFields(viewModel)

            viewModel.events.test {
                viewModel.onSubmit()
                advanceUntilIdle()
                val event = assertIs<UpgradeAccountEvent.Error>(awaitItem())
                val message = assertIs<UiText.Resource>(event.message)
                assertEquals(assertIs<UiText.Resource>(error.toUiText()).id, message.id)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun resendRepeatsTheRequestWithTheSameCredentials() =
        runTest(testDispatcher) {
            val authService = FakeAuthService(migrateToRegisteredResult = Result.Success(Unit))
            val viewModel = createViewModel(authService)
            fillFields(viewModel)

            viewModel.onSubmit()
            advanceUntilIdle()
            viewModel.onResend()
            advanceUntilIdle()

            assertEquals(
                listOf("new@test.com" to "password1", "new@test.com" to "password1"),
                authService.migrateToRegisteredCalls,
            )
            assertEquals("new@test.com", viewModel.state.value.pendingEmail)
        }

    @Test
    fun resendWithoutCredentialsFallsBackToTheForm() =
        runTest(testDispatcher) {
            // Reproduces a process death: the server still has the request, but the password only
            // ever lived in the text field, so there is nothing left to repeat.
            val authService =
                FakeAuthService(
                    refreshAccountResult = Result.Success(anonymousUser(pendingEmail = "new@test.com")),
                )
            val viewModel = createViewModel(authService)
            advanceUntilIdle()
            assertEquals("new@test.com", viewModel.state.value.pendingEmail)

            viewModel.events.test {
                viewModel.onResend()
                advanceUntilIdle()
                val event = assertIs<UpgradeAccountEvent.Error>(awaitItem())
                val message = assertIs<UiText.Resource>(event.message)
                assertEquals(Res.string.upgrade_account_resend_needs_details, message.id)
                cancelAndConsumeRemainingEvents()
            }

            assertNull(viewModel.state.value.pendingEmail)
            assertTrue(authService.migrateToRegisteredCalls.isEmpty())
        }

    @Test
    fun initRestoresAPendingRequestFromTheServer() =
        runTest(testDispatcher) {
            val authService =
                FakeAuthService(
                    refreshAccountResult = Result.Success(anonymousUser(pendingEmail = "waiting@test.com")),
                )
            val viewModel = createViewModel(authService)
            advanceUntilIdle()

            assertEquals("waiting@test.com", viewModel.state.value.pendingEmail)
            assertEquals(1, authService.refreshAccountCalls)
        }

    @Test
    fun initLeavesTheScreenWhenTheAccountIsAlreadyRegistered() =
        runTest(testDispatcher) {
            // The link was redeemed elsewhere while this screen was on its way up.
            val userWithPendingEmail =
                UserWithPendingEmail(
                    user = FakeSessionStorage.DEFAULT_USER.copy(userType = UserType.REGISTERED),
                    pendingEmail = null,
                )
            val viewModel =
                createViewModel(FakeAuthService(refreshAccountResult = Result.Success(userWithPendingEmail)))

            viewModel.events.test {
                advanceUntilIdle()
                assertIs<UpgradeAccountEvent.AlreadyRegistered>(awaitItem())
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun useDifferentEmailReturnsToTheForm() =
        runTest(testDispatcher) {
            val viewModel = createViewModel(FakeAuthService(migrateToRegisteredResult = Result.Success(Unit)))
            fillFields(viewModel)

            viewModel.onSubmit()
            advanceUntilIdle()
            assertEquals("new@test.com", viewModel.state.value.pendingEmail)

            viewModel.onUseDifferentEmail()

            assertNull(viewModel.state.value.pendingEmail)
        }

    @Test
    fun blurValidationIgnoresEmptyFields() =
        runTest(testDispatcher) {
            val viewModel = createViewModel(FakeAuthService())

            viewModel.validateEmailOnBlur()
            viewModel.validatePasswordOnBlur()
            viewModel.validateConfirmPasswordOnBlur()

            assertNull(viewModel.state.value.emailError)
            assertNull(viewModel.state.value.passwordError)
            assertNull(viewModel.state.value.confirmPasswordError)
        }

    private fun anonymousUser(pendingEmail: String?): UserWithPendingEmail =
        UserWithPendingEmail(
            user = FakeSessionStorage.DEFAULT_USER.copy(email = "", userType = UserType.ANONYMOUS),
            pendingEmail = pendingEmail,
        )

    private fun createViewModel(authService: FakeAuthService = FakeAuthService()): UpgradeAccountViewModel =
        UpgradeAccountViewModel(authService = authService)

    private fun TestScope.fillFields(
        viewModel: UpgradeAccountViewModel,
        email: String = "new@test.com",
        password: String = "password1",
        confirmPassword: String = password,
    ) {
        viewModel.emailState.edit { replace(0, length, email) }
        viewModel.passwordState.edit { replace(0, length, password) }
        viewModel.confirmPasswordState.edit { replace(0, length, confirmPassword) }
        Snapshot.sendApplyNotifications()
        advanceUntilIdle()
    }
}
