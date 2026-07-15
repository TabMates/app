package de.tabmates.features.authentication.data

import de.tabmates.core.data.dto.AuthInfoSerializable
import de.tabmates.core.data.dto.UserSerializable
import de.tabmates.core.data.dto.requests.RefreshRequest
import de.tabmates.core.data.mappers.toDomain
import de.tabmates.core.data.networking.delete
import de.tabmates.core.data.networking.get
import de.tabmates.core.data.networking.post
import de.tabmates.core.domain.auth.AuthInfo
import de.tabmates.core.domain.auth.SessionStorage
import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.EmptyResult
import de.tabmates.core.domain.util.Result
import de.tabmates.core.domain.util.map
import de.tabmates.core.domain.util.onSuccess
import de.tabmates.features.authentication.data.dto.requests.ChangeEmailRequest
import de.tabmates.features.authentication.data.dto.requests.ChangePasswordRequest
import de.tabmates.features.authentication.data.dto.requests.ChangeUsernameRequest
import de.tabmates.features.authentication.data.dto.requests.DeleteAccountRequest
import de.tabmates.features.authentication.data.dto.requests.EmailRequest
import de.tabmates.features.authentication.data.dto.requests.LoginAnonymousRequest
import de.tabmates.features.authentication.data.dto.requests.LoginRequest
import de.tabmates.features.authentication.data.dto.requests.RegisterAnonymousRequest
import de.tabmates.features.authentication.data.dto.requests.RegisterRequest
import de.tabmates.features.authentication.data.dto.requests.ResetPasswordRequest
import de.tabmates.features.authentication.domain.AuthService
import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.clearAuthTokens
import io.ktor.client.request.setBody
import org.koin.core.annotation.Single

@Single(binds = [AuthService::class])
class KtorAuthService(
    private val httpClient: HttpClient,
    private val sessionStorage: SessionStorage,
) : AuthService {
    override suspend fun register(
        email: String,
        username: String,
        password: String,
    ): EmptyResult<DataError.Remote> {
        return httpClient.post(
            route = "/api/auth/register",
            body =
                RegisterRequest(
                    email = email,
                    username = username,
                    password = password,
                ),
        )
    }

    override suspend fun registerAnonymous(username: String): Result<AuthInfo, DataError.Remote> {
        return httpClient
            .post<RegisterAnonymousRequest, AuthInfoSerializable>(
                route = "/api/auth/register-anonymous",
                body =
                    RegisterAnonymousRequest(
                        username = username,
                    ),
            ).map { authInfoSerializable ->
                authInfoSerializable.toDomain()
            }.onSuccess { authInfo ->
                sessionStorage.set(authInfo)
            }
    }

    override suspend fun login(
        email: String,
        password: String,
    ): Result<AuthInfo, DataError.Remote> {
        return httpClient
            .post<LoginRequest, AuthInfoSerializable>(
                route = "/api/auth/login",
                body =
                    LoginRequest(
                        email = email,
                        password = password,
                    ),
            ).map { authInfoSerializable ->
                authInfoSerializable.toDomain()
            }.onSuccess { authInfo ->
                sessionStorage.set(authInfo)
            }
    }

    override suspend fun loginAnonymous(
        userId: String,
        password: String,
    ): Result<AuthInfo, DataError.Remote> {
        return httpClient
            .post<LoginAnonymousRequest, AuthInfoSerializable>(
                route = "/api/auth/login-anonymous",
                body =
                    LoginAnonymousRequest(
                        userId = userId,
                        password = password,
                    ),
            ).map { authInfoSerializable ->
                authInfoSerializable.toDomain()
            }.onSuccess { authInfo ->
                sessionStorage.set(authInfo)
            }
    }

    override suspend fun resendVerificationEmail(email: String): EmptyResult<DataError.Remote> {
        return httpClient.post(
            route = "/api/auth/resend-verification",
            body = EmailRequest(email),
        )
    }

    override suspend fun verifyEmail(token: String): EmptyResult<DataError.Remote> {
        return httpClient.get(
            route = "/api/auth/verify",
            queryParams = mapOf("token" to token),
        )
    }

    override suspend fun forgotPassword(email: String): EmptyResult<DataError.Remote> {
        return httpClient.post<EmailRequest, Unit>(
            route = "/api/auth/forgot-password",
            body = EmailRequest(email),
        )
    }

    override suspend fun logout(refreshToken: String): EmptyResult<DataError.Remote> {
        return httpClient
            .post<RefreshRequest, Unit>(
                route = "/api/auth/logout",
                body = RefreshRequest(refreshToken = refreshToken),
            ).onSuccess {
                httpClient.clearAuthTokens()
            }
    }

    override suspend fun resetPassword(
        newPassword: String,
        token: String,
    ): EmptyResult<DataError.Remote> {
        return httpClient.post(
            route = "/api/auth/reset-password",
            body =
                ResetPasswordRequest(
                    newPassword = newPassword,
                    token = token,
                ),
        )
    }

    override suspend fun changeUsername(newUsername: String): EmptyResult<DataError.Remote> {
        return httpClient
            .post<ChangeUsernameRequest, UserSerializable>(
                route = "/api/auth/change-username",
                body = ChangeUsernameRequest(newUsername = newUsername),
            ).onSuccess { user ->
                // Keep the cached session in sync so the new username shows everywhere immediately.
                sessionStorage.get()?.let { current ->
                    sessionStorage.set(current.copy(user = user.toDomain()))
                }
            }.map { }
    }

    override suspend fun changePassword(
        oldPassword: String,
        newPassword: String,
    ): EmptyResult<DataError.Remote> {
        return httpClient.post(
            route = "/api/auth/change-password",
            body =
                ChangePasswordRequest(
                    oldPassword = oldPassword,
                    newPassword = newPassword,
                ),
        )
    }

    override suspend fun changeEmail(
        newEmail: String,
        password: String,
    ): EmptyResult<DataError.Remote> {
        // The server only stores a pending email and sends a verification link;
        // the cached session keeps the old address until the user confirms.
        return httpClient.post(
            route = "/api/auth/change-email",
            body =
                ChangeEmailRequest(
                    newEmail = newEmail,
                    password = password,
                ),
        )
    }

    override suspend fun deleteAccount(password: String?): EmptyResult<DataError.Remote> {
        return httpClient
            .delete<Unit>(
                route = "/api/auth/account",
                // Registered users confirm with their password; anonymous users send no body.
                builder = { password?.let { setBody(DeleteAccountRequest(password = it)) } },
            ).onSuccess {
                httpClient.clearAuthTokens()
            }
    }
}
