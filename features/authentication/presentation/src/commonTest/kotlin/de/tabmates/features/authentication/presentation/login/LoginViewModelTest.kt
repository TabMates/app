package de.tabmates.features.authentication.presentation.login

import androidx.compose.runtime.snapshots.Snapshot
import app.cash.turbine.test
import de.tabmates.core.domain.auth.AuthInfo
import de.tabmates.core.domain.auth.User
import de.tabmates.core.domain.auth.UserType
import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.Result
import de.tabmates.core.presentation.util.UiText
import de.tabmates.features.authentication.testing.FakeAuthService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import tabmatesapp.features.authentication.presentation.generated.resources.Res
import tabmatesapp.features.authentication.presentation.generated.resources.error_email_not_verified
import tabmatesapp.features.authentication.presentation.generated.resources.error_invalid_credentials
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
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
    fun initialStateHasCanLoginFalse() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()

            viewModel.state.test {
                val state = awaitItem()
                assertFalse(state.canLogin)
                assertFalse(state.isLoggingIn)
                assertFalse(state.isPasswordVisible)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun canLoginIsTrueWhenEmailAndPasswordAreValid() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            fillValidCredentials(viewModel)

            viewModel.state.test {
                assertTrue(expectMostRecentItem().canLogin)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun canLoginIsFalseWhenEmailIsInvalid() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()

            viewModel.state.value.emailTextFieldState.edit {
                replace(0, length, "invalid-email")
            }
            viewModel.state.value.passwordTextFieldState.edit {
                replace(0, length, "password123")
            }
            Snapshot.sendApplyNotifications()
            advanceUntilIdle()

            viewModel.state.test {
                assertFalse(expectMostRecentItem().canLogin)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun canLoginIsFalseWhenPasswordIsBlank() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()

            viewModel.state.value.emailTextFieldState.edit {
                replace(0, length, "test@example.com")
            }
            Snapshot.sendApplyNotifications()
            advanceUntilIdle()

            viewModel.state.test {
                assertFalse(expectMostRecentItem().canLogin)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun togglePasswordVisibilityFlipsState() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()

            viewModel.state.test {
                val initial = awaitItem()
                assertFalse(initial.isPasswordVisible)

                viewModel.onTogglePasswordVisibility()
                assertTrue(expectMostRecentItem().isPasswordVisible)

                viewModel.onTogglePasswordVisibility()
                assertFalse(expectMostRecentItem().isPasswordVisible)

                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun loginSuccessSendsSuccessEvent() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            fillValidCredentials(viewModel)

            viewModel.events.test {
                viewModel.onLogin()
                assertIs<LoginEvent.LoginSuccess>(awaitItem())
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun loginFailureWithUnauthorizedSendsFailureEvent() =
        runTest(testDispatcher) {
            val authService = FakeAuthService(loginResult = Result.Failure(DataError.Remote.UNAUTHORIZED))
            val viewModel = createViewModel(authService = authService)
            fillValidCredentials(viewModel)

            viewModel.events.test {
                viewModel.onLogin()
                val event = awaitItem()
                assertIs<LoginEvent.LoginFailure>(event)
                val resource = assertIs<UiText.Resource>(event.error)
                assertEquals(Res.string.error_invalid_credentials, resource.id)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun loginFailureWithForbiddenSendsFailureEvent() =
        runTest(testDispatcher) {
            val authService = FakeAuthService(loginResult = Result.Failure(DataError.Remote.FORBIDDEN))
            val viewModel = createViewModel(authService = authService)
            fillValidCredentials(viewModel)

            viewModel.events.test {
                viewModel.onLogin()
                val event = awaitItem()
                assertIs<LoginEvent.LoginFailure>(event)
                val resource = assertIs<UiText.Resource>(event.error)
                assertEquals(Res.string.error_email_not_verified, resource.id)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun loginDoesNothingWhenCanLoginIsFalse() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()

            viewModel.events.test {
                viewModel.onLogin()
                expectNoEvents()
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun isLoggingInIsFalseAfterSuccessfulLogin() =
        runTest(testDispatcher) {
            val authService = FakeAuthService(loginResult = Result.Success(FAKE_AUTH_INFO))
            val viewModel = createViewModel(authService = authService)
            fillValidCredentials(viewModel)

            viewModel.onLogin()
            advanceUntilIdle()

            assertFalse(viewModel.state.value.isLoggingIn)
            assertEquals(1, authService.loginCalls)
        }

    @Test
    fun loginSendsTheEmailTrimmedAndLowercased() =
        runTest(testDispatcher) {
            val authService = FakeAuthService(loginResult = Result.Success(FAKE_AUTH_INFO))
            val viewModel = createViewModel(authService = authService)
            // Written straight to the state, the way autofill or a prefill does: the field's
            // input transformation never runs, so the ViewModel has to normalize it.
            viewModel.state.value.emailTextFieldState.edit {
                replace(0, length, " Test.User@Example.COM ")
            }
            viewModel.state.value.passwordTextFieldState.edit {
                replace(0, length, "password123")
            }
            Snapshot.sendApplyNotifications()
            advanceUntilIdle()

            viewModel.onLogin()
            advanceUntilIdle()

            assertEquals(listOf("test.user@example.com"), authService.loginEmails)
        }

    @Test
    fun isLoggingInIsFalseAfterFailedLogin() =
        runTest(testDispatcher) {
            val authService = FakeAuthService(loginResult = Result.Failure(DataError.Remote.UNKNOWN))
            val viewModel = createViewModel(authService = authService)
            fillValidCredentials(viewModel)

            viewModel.onLogin()
            advanceUntilIdle()

            assertFalse(viewModel.state.value.isLoggingIn)
            assertEquals(1, authService.loginCalls)
        }

    @Test
    fun loginFailureWithForbiddenSetsEmailNotVerified() =
        runTest(testDispatcher) {
            val authService = FakeAuthService(loginResult = Result.Failure(DataError.Remote.FORBIDDEN))
            val viewModel = createViewModel(authService = authService)
            fillValidCredentials(viewModel)

            viewModel.onLogin()
            advanceUntilIdle()

            assertTrue(viewModel.state.value.isEmailNotVerified)
        }

    @Test
    fun retryingLoginResetsEmailNotVerified() =
        runTest(testDispatcher) {
            val authService = FakeAuthService(loginResult = Result.Failure(DataError.Remote.FORBIDDEN))
            val viewModel = createViewModel(authService = authService)
            fillValidCredentials(viewModel)

            viewModel.onLogin()
            advanceUntilIdle()
            assertTrue(viewModel.state.value.isEmailNotVerified)

            authService.loginResult = Result.Failure(DataError.Remote.UNAUTHORIZED)
            viewModel.onLogin()
            advanceUntilIdle()

            assertFalse(viewModel.state.value.isEmailNotVerified)
        }

    @Test
    fun resendVerificationSuccessEmitsEventAndResetsLoading() =
        runTest(testDispatcher) {
            val authService = FakeAuthService(loginResult = Result.Failure(DataError.Remote.FORBIDDEN))
            val viewModel = createViewModel(authService = authService)
            fillValidCredentials(viewModel)

            viewModel.events.test {
                viewModel.resendVerification()
                assertIs<LoginEvent.ResendVerificationEmailSuccess>(awaitItem())
                cancelAndConsumeRemainingEvents()
            }

            assertEquals(1, authService.resendVerificationEmailCalls)
            assertFalse(viewModel.state.value.isResendingVerificationEmail)
        }

    @Test
    fun resendVerificationFailureEmitsErrorAndSetsErrorState() =
        runTest(testDispatcher) {
            val authService =
                FakeAuthService(
                    loginResult = Result.Failure(DataError.Remote.FORBIDDEN),
                    resendVerificationEmailResult = Result.Failure(DataError.Remote.SERVER_ERROR),
                )
            val viewModel = createViewModel(authService = authService)
            fillValidCredentials(viewModel)

            viewModel.events.test {
                viewModel.resendVerification()
                assertIs<LoginEvent.ResendVerificationEmailError>(awaitItem())
                cancelAndConsumeRemainingEvents()
            }

            assertEquals(1, authService.resendVerificationEmailCalls)
            assertFalse(viewModel.state.value.isResendingVerificationEmail)
            assertIs<UiText.Resource>(viewModel.state.value.resendVerificationError)
        }

    @Test
    fun resendVerificationEntersCooldownAndBlocksFurtherSendsUntilElapsed() =
        runTest(testDispatcher) {
            val authService = FakeAuthService()
            val viewModel = createViewModel(authService = authService)

            viewModel.resendVerification()
            assertEquals(1, authService.resendVerificationEmailCalls)
            assertEquals(180, viewModel.state.value.resendCooldownSeconds)

            // Blocked while the cooldown is active.
            viewModel.resendVerification()
            assertEquals(1, authService.resendVerificationEmailCalls)

            // Cooldown counts down to zero.
            advanceUntilIdle()
            assertEquals(0, viewModel.state.value.resendCooldownSeconds)

            // Allowed again once the cooldown has elapsed.
            viewModel.resendVerification()
            assertEquals(2, authService.resendVerificationEmailCalls)
        }

    @Test
    fun resendVerificationIgnoresDuplicateCallsWhileInProgress() =
        runTest(testDispatcher) {
            val authService = FakeAuthService(resendVerificationEmailDelayMillis = 1_000L)
            val viewModel = createViewModel(authService = authService)

            viewModel.resendVerification()
            viewModel.resendVerification()

            assertEquals(1, authService.resendVerificationEmailCalls)

            advanceUntilIdle()
            assertFalse(viewModel.state.value.isResendingVerificationEmail)
        }

    private fun TestScope.activateState(viewModel: LoginViewModel) {
        backgroundScope.launch {
            viewModel.state.collect { }
        }
        advanceUntilIdle()
    }

    private fun TestScope.createViewModel(
        authService: FakeAuthService = FakeAuthService(loginResult = Result.Success(FAKE_AUTH_INFO)),
    ): LoginViewModel {
        val viewModel = LoginViewModel(authService = authService)
        activateState(viewModel)
        return viewModel
    }

    private fun TestScope.fillValidCredentials(viewModel: LoginViewModel) {
        viewModel.state.value.emailTextFieldState.edit {
            replace(0, length, "test@example.com")
        }
        viewModel.state.value.passwordTextFieldState.edit {
            replace(0, length, "password123")
        }
        Snapshot.sendApplyNotifications()
        advanceUntilIdle()
    }

    private companion object {
        private val FAKE_AUTH_INFO =
            AuthInfo(
                accessToken = "token",
                refreshToken = "refresh",
                user =
                    User(
                        id = "1",
                        email = "test@test.com",
                        username = "test",
                        hasVerifiedEmail = true,
                        userType = UserType.REGISTERED,
                    ),
            )
    }
}
