package de.tabmates.features.authentication.testing

import de.tabmates.core.domain.auth.AuthInfo
import de.tabmates.core.domain.auth.UserWithPendingEmail
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
    var registerAnonymousResult: Result<AuthInfo, DataError.Remote> =
        Result.Failure(DataError.Remote.UNKNOWN),
    var logoutResult: EmptyResult<DataError.Remote> = Result.Success(Unit),
    var changeUsernameResult: EmptyResult<DataError.Remote> = Result.Success(Unit),
    var changePasswordResult: EmptyResult<DataError.Remote> = Result.Success(Unit),
    var changeEmailResult: EmptyResult<DataError.Remote> = Result.Success(Unit),
    var deleteAccountResult: EmptyResult<DataError.Remote> = Result.Success(Unit),
    var migrateToRegisteredResult: EmptyResult<DataError.Remote> = Result.Success(Unit),
    var migrateToRegisteredDelayMillis: Long = 0L,
    var refreshAccountResult: Result<UserWithPendingEmail, DataError.Remote> =
        Result.Failure(DataError.Remote.UNKNOWN),
) : AuthService {
    val logoutCalls: MutableList<String> = mutableListOf()
    val migrateToRegisteredCalls: MutableList<Pair<String, String>> = mutableListOf()
    val changeUsernameCalls: MutableList<String> = mutableListOf()
    val changePasswordCalls: MutableList<Pair<String, String>> = mutableListOf()
    val changeEmailCalls: MutableList<Pair<String, String>> = mutableListOf()
    val deleteAccountCalls: MutableList<String?> = mutableListOf()
    var registerCalls: Int = 0
        private set

    var registerAnonymousCalls: Int = 0
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

    var clearCachedTokensCalls: Int = 0
        private set

    var refreshAccountCalls: Int = 0
        private set

    override suspend fun register(
        email: String,
        username: String,
        password: String,
    ): EmptyResult<DataError.Remote> {
        registerCalls += 1
        return registerResult
    }

    override suspend fun registerAnonymous(username: String): Result<AuthInfo, DataError.Remote> {
        registerAnonymousCalls += 1
        return registerAnonymousResult
    }

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

    override suspend fun migrateToRegistered(
        email: String,
        password: String,
    ): EmptyResult<DataError.Remote> {
        migrateToRegisteredCalls += email to password
        if (migrateToRegisteredDelayMillis > 0L) {
            delay(migrateToRegisteredDelayMillis)
        }
        return migrateToRegisteredResult
    }

    override suspend fun refreshAccount(): Result<UserWithPendingEmail, DataError.Remote> {
        refreshAccountCalls += 1
        return refreshAccountResult
    }

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

    override fun clearCachedTokens() {
        clearCachedTokensCalls += 1
    }

    override suspend fun logout(refreshToken: String): EmptyResult<DataError.Remote> {
        logoutCalls += refreshToken
        clearCachedTokens()
        return logoutResult
    }

    override suspend fun resetPassword(
        newPassword: String,
        token: String,
    ): EmptyResult<DataError.Remote> {
        resetPasswordCalls += 1
        return resetPasswordResult
    }

    override suspend fun changeUsername(newUsername: String): EmptyResult<DataError.Remote> {
        changeUsernameCalls += newUsername
        return changeUsernameResult
    }

    override suspend fun changePassword(
        oldPassword: String,
        newPassword: String,
    ): EmptyResult<DataError.Remote> {
        changePasswordCalls += (oldPassword to newPassword)
        return changePasswordResult
    }

    override suspend fun changeEmail(
        newEmail: String,
        password: String,
    ): EmptyResult<DataError.Remote> {
        changeEmailCalls += (newEmail to password)
        return changeEmailResult
    }

    override suspend fun deleteAccount(password: String?): EmptyResult<DataError.Remote> {
        deleteAccountCalls += password
        return deleteAccountResult
    }
}
