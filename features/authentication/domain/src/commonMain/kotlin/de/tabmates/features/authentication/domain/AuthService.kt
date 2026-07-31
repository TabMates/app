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

    /**
     * Drops the access token the HTTP client is holding in memory.
     *
     * The client only reads the session back from storage once its own cache is empty, so a token
     * that is not dropped keeps authenticating requests as the account it was issued to — even
     * after that session is gone. [logout] and [deleteAccount] already do this; call it directly
     * on any other path that ends a session without going through them.
     */
    fun clearCachedTokens()

    suspend fun resetPassword(
        newPassword: String,
        token: String,
    ): EmptyResult<DataError.Remote>

    suspend fun changeUsername(newUsername: String): EmptyResult<DataError.Remote>

    suspend fun changePassword(
        oldPassword: String,
        newPassword: String,
    ): EmptyResult<DataError.Remote>

    suspend fun changeEmail(
        newEmail: String,
        password: String,
    ): EmptyResult<DataError.Remote>

    /**
     * Deletes the currently authenticated account.
     *
     * Registered users must supply their current [password]; anonymous users have none, so it may
     * be `null`. On success the caller is responsible for clearing the local session.
     */
    suspend fun deleteAccount(password: String?): EmptyResult<DataError.Remote>
}
