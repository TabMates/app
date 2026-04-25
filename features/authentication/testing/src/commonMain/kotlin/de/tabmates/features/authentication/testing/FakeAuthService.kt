package de.tabmates.features.authentication.testing

import de.tabmates.core.domain.auth.AuthInfo
import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.EmptyResult
import de.tabmates.core.domain.util.Result
import de.tabmates.features.authentication.domain.AuthService
import kotlinx.coroutines.delay

open class FakeAuthService(
    var registerResult: EmptyResult<DataError.Remote> = Result.Success(Unit),
    var loginResult: Result<AuthInfo, DataError.Remote> = Result.Failure(DataError.Remote.UNKNOWN),
    var loginDelayMillis: Long = 0L,
    var resendVerificationEmailResult: EmptyResult<DataError.Remote> = Result.Success(Unit),
    var resendVerificationEmailDelayMillis: Long = 0L,
    var verifyEmailResult: EmptyResult<DataError.Remote> = Result.Success(Unit),
    var forgotPasswordResult: EmptyResult<DataError.Remote> = Result.Success(Unit),
    var resetPasswordResult: EmptyResult<DataError.Remote> = Result.Success(Unit),
) : AuthService {
    var registerCalls: Int = 0
        private set

    var loginCalls: Int = 0
        private set

    var resendVerificationEmailCalls: Int = 0
        private set

    var verifyEmailCalls: Int = 0
        private set

    var forgotPasswordCalls: Int = 0
        private set

    var resetPasswordCalls: Int = 0
        private set

    override suspend fun register(
        email: String,
        username: String,
        password: String,
    ): EmptyResult<DataError.Remote> {
        registerCalls += 1
        return registerResult
    }

    override suspend fun registerAnonymous(
        username: String,
        password: String,
    ): EmptyResult<DataError.Remote> = Result.Success(Unit)

    override suspend fun login(
        email: String,
        password: String,
    ): Result<AuthInfo, DataError.Remote> {
        loginCalls += 1
        if (loginDelayMillis > 0L) {
            delay(loginDelayMillis)
        }
        return loginResult
    }

    override suspend fun loginAnonymous(
        userId: String,
        password: String,
    ): Result<AuthInfo, DataError.Remote> = Result.Failure(DataError.Remote.UNKNOWN)

    override suspend fun resendVerificationEmail(email: String): EmptyResult<DataError.Remote> {
        resendVerificationEmailCalls += 1
        if (resendVerificationEmailDelayMillis > 0L) {
            delay(resendVerificationEmailDelayMillis)
        }
        return resendVerificationEmailResult
    }

    override suspend fun verifyEmail(token: String): EmptyResult<DataError.Remote> {
        verifyEmailCalls += 1
        return verifyEmailResult
    }

    override suspend fun forgotPassword(email: String): EmptyResult<DataError.Remote> {
        forgotPasswordCalls += 1
        return forgotPasswordResult
    }

    override suspend fun logout(refreshToken: String): EmptyResult<DataError.Remote> = Result.Success(Unit)

    override suspend fun resetPassword(
        newPassword: String,
        token: String,
    ): EmptyResult<DataError.Remote> {
        resetPasswordCalls += 1
        return resetPasswordResult
    }
}
