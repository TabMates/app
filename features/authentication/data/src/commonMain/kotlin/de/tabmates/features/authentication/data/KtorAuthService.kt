package de.tabmates.features.authentication.data

import de.tabmates.core.data.dto.AuthInfoSerializable
import de.tabmates.core.data.dto.UserSerializable
import de.tabmates.core.data.dto.UserWithPendingEmailSerializable
import de.tabmates.core.data.dto.requests.RefreshRequest
import de.tabmates.core.data.mappers.toDomain
import de.tabmates.core.data.networking.delete
import de.tabmates.core.data.networking.get
import de.tabmates.core.data.networking.post
import de.tabmates.core.domain.auth.AuthInfo
import de.tabmates.core.domain.auth.SessionStorage
import de.tabmates.core.domain.auth.UserWithPendingEmail
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
import de.tabmates.features.authentication.data.dto.requests.LoginRequest
import de.tabmates.features.authentication.data.dto.requests.MigrateToRegisteredRequest
import de.tabmates.features.authentication.data.dto.requests.RegisterAnonymousRequest
import de.tabmates.features.authentication.data.dto.requests.RegisterRequest
import de.tabmates.features.authentication.data.dto.requests.ResetPasswordRequest
import de.tabmates.features.authentication.data.dto.turnstileErrorOrNull
import de.tabmates.features.authentication.domain.AuthService
import de.tabmates.features.authentication.domain.TurnstileTokenProvider
import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.clearAuthTokens
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import org.koin.core.annotation.Single

@Single(binds = [AuthService::class])
class KtorAuthService(
    private val httpClient: HttpClient,
    private val sessionStorage: SessionStorage,
    // Web-only Cloudflare Turnstile token; no-op (null) on native, so the header is omitted there.
    private val turnstileTokenProvider: TurnstileTokenProvider,
) : AuthService {
    override suspend fun register(
        email: String,
        username: String,
        password: String,
    ): EmptyResult<DataError.Remote> {
        val turnstileToken = turnstileTokenProvider.getToken()
        return httpClient.post(
            route = "/api/auth/register",
            body =
                RegisterRequest(
                    email = email,
                    username = username,
                    password = password,
                ),
            mapKnownError = { it.turnstileErrorOrNull() },
            builder = { turnstileToken?.let { token -> header("cf-turnstile-response", token) } },
        )
    }

    override suspend fun registerAnonymous(username: String): Result<AuthInfo, DataError.Remote> {
        val turnstileToken = turnstileTokenProvider.getToken()
        return httpClient
            .post<RegisterAnonymousRequest, AuthInfoSerializable>(
                route = "/api/auth/register-anonymous",
                body =
                    RegisterAnonymousRequest(
                        username = username,
                    ),
                mapKnownError = { it.turnstileErrorOrNull() },
                builder = { turnstileToken?.let { token -> header("cf-turnstile-response", token) } },
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
        val turnstileToken = turnstileTokenProvider.getToken()
        return httpClient
            .post<LoginRequest, AuthInfoSerializable>(
                route = "/api/auth/login",
                body =
                    LoginRequest(
                        email = email,
                        password = password,
                    ),
                mapKnownError = { it.turnstileErrorOrNull() },
                builder = { turnstileToken?.let { token -> header("cf-turnstile-response", token) } },
            ).map { authInfoSerializable ->
                authInfoSerializable.toDomain()
            }.onSuccess { authInfo ->
                sessionStorage.set(authInfo)
            }
    }

    override suspend fun migrateToRegistered(
        email: String,
        password: String,
    ): EmptyResult<DataError.Remote> {
        // Deliberately does not touch the session: the server only records the request until the
        // emailed link is redeemed, so the account is still anonymous when this returns.
        // The endpoint authenticates with the anonymous session's own token and is not
        // Turnstile-protected, so no challenge token is attached here.
        return httpClient.post(
            route = "/api/auth/migrate-to-registered",
            body =
                MigrateToRegisteredRequest(
                    email = email,
                    password = password,
                ),
        )
    }

    override suspend fun refreshAccount(): Result<UserWithPendingEmail, DataError.Remote> {
        return httpClient
            .get<UserWithPendingEmailSerializable>(route = "/api/auth/account")
            .map { serializable ->
                serializable.toDomain()
            }.onSuccess { userWithPendingEmail ->
                // Keep the cached session in sync so a migration confirmed on another device shows
                // up everywhere `userType` is read. A failure leaves the session as it was: this is
                // a background refresh, and a flaky network must not look like a signed-out user.
                sessionStorage.get()?.let { current ->
                    sessionStorage.set(current.copy(user = userWithPendingEmail.user))
                }
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
        val turnstileToken = turnstileTokenProvider.getToken()
        return httpClient.post<EmailRequest, Unit>(
            route = "/api/auth/forgot-password",
            body = EmailRequest(email),
            mapKnownError = { it.turnstileErrorOrNull() },
            builder = { turnstileToken?.let { token -> header("cf-turnstile-response", token) } },
        )
    }

    override fun clearCachedTokens() {
        httpClient.clearAuthTokens()
    }

    override suspend fun logout(refreshToken: String): EmptyResult<DataError.Remote> {
        // Cleared after the call, and regardless of how it went: the revoke is allowed to fail
        // (offline sign-out still signs out), but the cached token must not survive it. Ktor only
        // reloads tokens from storage once its cache is empty, so a token left behind here keeps
        // authenticating as the account that just left — the next account's first sync would then
        // pull that account's groups into the freshly wiped database. Clearing when the request
        // failed costs nothing: while the session still exists the very next call reloads it.
        val result =
            httpClient.post<RefreshRequest, Unit>(
                route = "/api/auth/logout",
                body = RefreshRequest(refreshToken = refreshToken),
            )
        clearCachedTokens()
        return result
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
        val result =
            httpClient.delete<Unit>(
                route = "/api/auth/account",
                // Registered users confirm with their password; anonymous users send no body.
                builder = { password?.let { setBody(DeleteAccountRequest(password = it)) } },
            )
        // Same reasoning as in `logout`.
        clearCachedTokens()
        return result
    }
}
