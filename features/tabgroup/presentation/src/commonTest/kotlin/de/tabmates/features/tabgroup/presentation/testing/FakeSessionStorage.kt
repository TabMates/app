package de.tabmates.features.tabgroup.presentation.testing

import de.tabmates.core.domain.auth.AuthInfo
import de.tabmates.core.domain.auth.SessionStorage
import de.tabmates.core.domain.auth.User
import de.tabmates.core.domain.auth.UserType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeSessionStorage(
    initial: AuthInfo? = DEFAULT_AUTH_INFO,
) : SessionStorage {
    private val state = MutableStateFlow(initial)

    override val authState: StateFlow<AuthInfo?> = state

    override fun get(): AuthInfo? = state.value

    override fun set(info: AuthInfo?) {
        state.value = info
    }

    companion object {
        val DEFAULT_USER =
            User(
                id = "user-1",
                email = "user@test.com",
                username = "alice",
                hasVerifiedEmail = true,
                userType = UserType.REGISTERED,
            )
        val DEFAULT_AUTH_INFO =
            AuthInfo(
                accessToken = "access",
                refreshToken = "refresh",
                user = DEFAULT_USER,
            )
    }
}
