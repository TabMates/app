package de.tabmates.features.authentication.presentation.emailverification

import de.tabmates.core.domain.auth.AuthInfo
import de.tabmates.core.domain.auth.SessionInvalidationReason
import de.tabmates.core.domain.auth.User
import de.tabmates.core.domain.auth.UserType
import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.EmptyResult
import de.tabmates.core.domain.util.Result
import de.tabmates.features.authentication.testing.FakeAuthService
import de.tabmates.features.authentication.testing.FakeSessionInvalidator
import de.tabmates.features.authentication.testing.FakeSessionStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
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
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class EmailVerificationViewModelTest {
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
    fun `successful email verification sets isVerified to true`() =
        runTest(testDispatcher) {
            val authService = FakeAuthService(verifyEmailResult = Result.Success(Unit))
            val viewModel = createViewModel(authService = authService)

            assertEquals(true, viewModel.state.value.isVerified)
            assertEquals(false, viewModel.state.value.isVerifying)
            assertEquals(1, authService.verifyEmailCalls)
        }

    @Test
    fun `failed email verification sets isVerified to false`() =
        runTest(testDispatcher) {
            val authService =
                FakeAuthService(
                    verifyEmailResult = Result.Failure(DataError.Remote.SERVER_ERROR),
                )
            val viewModel = createViewModel(authService = authService)

            assertEquals(false, viewModel.state.value.isVerified)
            assertEquals(false, viewModel.state.value.isVerifying)
            assertEquals(1, authService.verifyEmailCalls)
        }

    @Test
    fun `token is forwarded to auth service`() =
        runTest(testDispatcher) {
            var capturedToken: String? = null
            val authService =
                object : FakeAuthService() {
                    override suspend fun verifyEmail(token: String): EmptyResult<DataError.Remote> {
                        capturedToken = token
                        return super.verifyEmail(token)
                    }
                }
            createViewModel(authService = authService, token = "my-token-123")

            assertEquals("my-token-123", capturedToken)
        }

    private fun TestScope.activateState(viewModel: EmailVerificationViewModel) {
        backgroundScope.launch {
            viewModel.state.collect()
        }
        advanceUntilIdle()
    }

    @Test
    fun `successful verification clears the cached session as an email change`() =
        runTest(testDispatcher) {
            val sessionStorage = FakeSessionStorage(initial = authInfo())
            val sessionInvalidator = FakeSessionInvalidator(sessionStorage)
            createViewModel(
                authService = FakeAuthService(verifyEmailResult = Result.Success(Unit)),
                sessionStorage = sessionStorage,
                sessionInvalidator = sessionInvalidator,
            )

            assertNull(sessionStorage.get())
            // EMAIL_CHANGED rather than TOKEN_REJECTED: the stored address is now the old one, so
            // the re-auth screen must ask for the new one instead of locking the stale value in.
            assertEquals(listOf(SessionInvalidationReason.EMAIL_CHANGED), sessionInvalidator.reasons)
        }

    @Test
    fun `failed verification keeps the cached session`() =
        runTest(testDispatcher) {
            val sessionStorage = FakeSessionStorage(initial = authInfo())
            createViewModel(
                authService = FakeAuthService(verifyEmailResult = Result.Failure(DataError.Remote.SERVER_ERROR)),
                sessionStorage = sessionStorage,
            )

            assertEquals(authInfo(), sessionStorage.get())
        }

    private fun authInfo(): AuthInfo =
        AuthInfo(
            accessToken = "access",
            refreshToken = "refresh",
            user =
                User(
                    id = "user-1",
                    email = "user@test.com",
                    username = "alice",
                    hasVerifiedEmail = true,
                    userType = UserType.REGISTERED,
                ),
        )

    private fun TestScope.createViewModel(
        authService: FakeAuthService = FakeAuthService(),
        sessionStorage: FakeSessionStorage = FakeSessionStorage(),
        sessionInvalidator: FakeSessionInvalidator = FakeSessionInvalidator(sessionStorage),
        token: String = "test-token",
    ): EmailVerificationViewModel {
        val viewModel =
            EmailVerificationViewModel(
                authService = authService,
                sessionInvalidator = sessionInvalidator,
                token = token,
            )
        activateState(viewModel)
        return viewModel
    }
}
