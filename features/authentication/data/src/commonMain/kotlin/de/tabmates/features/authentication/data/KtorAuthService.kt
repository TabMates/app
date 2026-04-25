package de.tabmates.features.authentication.data

import de.tabmates.core.data.dto.AuthInfoSerializable
import de.tabmates.core.data.dto.requests.RefreshRequest
import de.tabmates.core.data.mappers.toDomain
import de.tabmates.core.data.networking.get
import de.tabmates.core.data.networking.post
import de.tabmates.core.domain.auth.AuthInfo
import de.tabmates.core.domain.auth.SessionStorage
import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.EmptyResult
import de.tabmates.core.domain.util.Result
import de.tabmates.core.domain.util.map
import de.tabmates.core.domain.util.onSuccess
import de.tabmates.features.authentication.data.dto.requests.EmailRequest
import de.tabmates.features.authentication.data.dto.requests.LoginAnonymousRequest
import de.tabmates.features.authentication.data.dto.requests.LoginRequest
import de.tabmates.features.authentication.data.dto.requests.RegisterAnonymousRequest
import de.tabmates.features.authentication.data.dto.requests.RegisterRequest
import de.tabmates.features.authentication.data.dto.requests.ResetPasswordRequest
import de.tabmates.features.authentication.domain.AuthService
import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.clearAuthTokens

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

    override suspend fun registerAnonymous(
        username: String,
        password: String,
    ): EmptyResult<DataError.Remote> {
        return httpClient.post(
            route = "/api/auth/register-anonymous",
            body =
                RegisterAnonymousRequest(
                    username = username,
                    password = password,
                ),
        )
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
}
