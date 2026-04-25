package de.tabmates.features.authentication.presentation.resetpassword

import androidx.compose.runtime.snapshots.Snapshot
import app.cash.turbine.test
import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.Result
import de.tabmates.core.presentation.util.UiText
import de.tabmates.core.presentation.util.toUiText
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
import tabmatesapp.features.authentication.presentation.generated.resources.error_reset_password_token_invalid
import tabmatesapp.features.authentication.presentation.generated.resources.error_same_password
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ResetPasswordViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testToken = "test-token"

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialStateIsCorrect() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()

            viewModel.state.test {
                val state = awaitItem()
                assertFalse(state.canSubmit)
                assertFalse(state.isLoading)
                assertFalse(state.isPasswordVisible)
                assertFalse(state.isResetSuccessful)
                assertNull(state.errorText)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun canSubmitIsTrueWhenPasswordIsValid() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()

            viewModel.state.value.passwordTextState.edit {
                replace(0, length, "Password123!")
            }
            Snapshot.sendApplyNotifications()
            advanceUntilIdle()

            viewModel.state.test {
                assertTrue(expectMostRecentItem().canSubmit)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun canSubmitIsFalseWhenPasswordIsTooShort() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()

            viewModel.state.value.passwordTextState.edit {
                replace(0, length, "Short1!")
            }
            Snapshot.sendApplyNotifications()
            advanceUntilIdle()

            viewModel.state.test {
                assertFalse(expectMostRecentItem().canSubmit)
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

                viewModel.onTogglePasswordVisibilityClick()
                assertTrue(expectMostRecentItem().isPasswordVisible)

                viewModel.onTogglePasswordVisibilityClick()
                assertFalse(expectMostRecentItem().isPasswordVisible)

                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun submitResetPasswordRequestSuccessSetsStateCorrectly() =
        runTest(testDispatcher) {
            val authService = FakeAuthService(resetPasswordResult = Result.Success(Unit))
            val viewModel = createViewModel(authService = authService)
            fillValidPassword(viewModel)

            viewModel.submitResetPasswordRequest()
            advanceUntilIdle()

            viewModel.state.test {
                val state = expectMostRecentItem()
                assertTrue(state.isResetSuccessful)
                assertFalse(state.isLoading)
                assertNull(state.errorText)
                assertEquals(1, authService.resetPasswordCalls)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun submitResetPasswordRequestFailureWithUnauthorizedSetsTokenInvalidError() =
        runTest(testDispatcher) {
            val authService = FakeAuthService(resetPasswordResult = Result.Failure(DataError.Remote.UNAUTHORIZED))
            val viewModel = createViewModel(authService = authService)
            fillValidPassword(viewModel)

            viewModel.submitResetPasswordRequest()
            advanceUntilIdle()

            viewModel.state.test {
                val state = expectMostRecentItem()
                assertFalse(state.isResetSuccessful)
                assertFalse(state.isLoading)
                val errorText = assertIs<UiText.Resource>(state.errorText)
                assertEquals(Res.string.error_reset_password_token_invalid, errorText.id)
                assertEquals(1, authService.resetPasswordCalls)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun submitResetPasswordRequestFailureWithConflictSetsSamePasswordError() =
        runTest(testDispatcher) {
            val authService = FakeAuthService(resetPasswordResult = Result.Failure(DataError.Remote.CONFLICT))
            val viewModel = createViewModel(authService = authService)
            fillValidPassword(viewModel)

            viewModel.submitResetPasswordRequest()
            advanceUntilIdle()

            viewModel.state.test {
                val state = expectMostRecentItem()
                assertFalse(state.isResetSuccessful)
                assertFalse(state.isLoading)
                val errorText = assertIs<UiText.Resource>(state.errorText)
                assertEquals(Res.string.error_same_password, errorText.id)
                assertEquals(1, authService.resetPasswordCalls)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun submitResetPasswordRequestOtherFailureSetsGenericError() =
        runTest(testDispatcher) {
            val error = DataError.Remote.SERVER_ERROR
            val authService = FakeAuthService(resetPasswordResult = Result.Failure(error))
            val viewModel = createViewModel(authService = authService)
            fillValidPassword(viewModel)

            viewModel.submitResetPasswordRequest()
            advanceUntilIdle()

            viewModel.state.test {
                val state = expectMostRecentItem()
                assertFalse(state.isResetSuccessful)
                assertFalse(state.isLoading)
                val errorText = assertIs<UiText.Resource>(state.errorText)
                val expectedErrorText = assertIs<UiText.Resource>(error.toUiText())
                assertEquals(expectedErrorText.id, errorText.id)
                assertEquals(1, authService.resetPasswordCalls)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun submitResetPasswordRequestDoesNothingWhenCanSubmitIsFalse() =
        runTest(testDispatcher) {
            val authService = FakeAuthService()
            val viewModel = createViewModel(authService = authService)

            viewModel.submitResetPasswordRequest()
            advanceUntilIdle()

            assertEquals(0, authService.resetPasswordCalls)
            assertFalse(viewModel.state.value.isLoading)
        }

    private fun TestScope.activateState(viewModel: ResetPasswordViewModel) {
        backgroundScope.launch {
            viewModel.state.collect { }
        }
        advanceUntilIdle()
    }

    private fun TestScope.createViewModel(
        authService: FakeAuthService = FakeAuthService(),
    ): ResetPasswordViewModel {
        val viewModel =
            ResetPasswordViewModel(
                authService = authService,
                token = testToken,
            )
        activateState(viewModel)
        return viewModel
    }

    private fun TestScope.fillValidPassword(viewModel: ResetPasswordViewModel) {
        viewModel.state.value.passwordTextState.edit {
            replace(0, length, "Password123!")
        }
        Snapshot.sendApplyNotifications()
        advanceUntilIdle()
    }
}
