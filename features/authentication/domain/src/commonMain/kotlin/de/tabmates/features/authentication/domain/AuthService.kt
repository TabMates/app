package de.tabmates.features.authentication.domain

import de.tabmates.core.domain.auth.AuthInfo
import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.EmptyResult
import de.tabmates.core.domain.util.Result

interface AuthService {
    suspend fun register(
        email: String,
        username: String,
        password: String,
    ): EmptyResult<DataError.Remote>

    suspend fun registerAnonymous(username: String): Result<AuthInfo, DataError.Remote>

    suspend fun login(
        email: String,
        password: String,
    ): Result<AuthInfo, DataError.Remote>

    suspend fun loginAnonymous(
        userId: String,
        password: String,
    ): Result<AuthInfo, DataError.Remote>

    suspend fun resendVerificationEmail(email: String): EmptyResult<DataError.Remote>

    suspend fun verifyEmail(token: String): EmptyResult<DataError.Remote>

    suspend fun forgotPassword(email: String): EmptyResult<DataError.Remote>

    suspend fun logout(refreshToken: String): EmptyResult<DataError.Remote>

    suspend fun resetPassword(
        newPassword: String,
        token: String,
    ): EmptyResult<DataError.Remote>
}
