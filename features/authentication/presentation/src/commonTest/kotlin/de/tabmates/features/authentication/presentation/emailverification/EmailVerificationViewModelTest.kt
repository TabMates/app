package de.tabmates.features.authentication.presentation.emailverification

import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.EmptyResult
import de.tabmates.core.domain.util.Result
import de.tabmates.features.authentication.presentation.fakes.FakeAuthService
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

    private fun TestScope.createViewModel(
        authService: FakeAuthService = FakeAuthService(),
        token: String = "test-token",
    ): EmailVerificationViewModel {
        val viewModel = EmailVerificationViewModel(authService = authService, token = token)
        activateState(viewModel)
        return viewModel
    }
}
