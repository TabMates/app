package de.tabmates.features.authentication.presentation.fakes

import de.tabmates.core.domain.auth.AuthInfo
import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.EmptyResult
import de.tabmates.core.domain.util.Result
import de.tabmates.features.authentication.domain.AuthService

internal class FakeAuthService(
    var registerResult: EmptyResult<DataError.Remote> = Result.Success(Unit),
) : AuthService {
    var registerCalls: Int = 0
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
    ): Result<AuthInfo, DataError.Remote> = Result.Failure(DataError.Remote.UNKNOWN)

    override suspend fun loginAnonymous(
        userId: String,
        password: String,
    ): Result<AuthInfo, DataError.Remote> = Result.Failure(DataError.Remote.UNKNOWN)

    override suspend fun resendVerificationEmail(email: String): EmptyResult<DataError.Remote> =
        Result.Success(Unit)

    override suspend fun verifyEmail(token: String): EmptyResult<DataError.Remote> = Result.Success(Unit)

    override suspend fun forgotPassword(email: String): EmptyResult<DataError.Remote> = Result.Success(Unit)
}

