package de.tabmates.features.authentication.data

import de.tabmates.core.data.networking.post
import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.EmptyResult
import de.tabmates.features.authentication.data.dto.requests.RegisterRequest
import de.tabmates.features.authentication.domain.AuthService
import io.ktor.client.HttpClient

class KtorAuthService(
    private val httpClient: HttpClient,
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
}
