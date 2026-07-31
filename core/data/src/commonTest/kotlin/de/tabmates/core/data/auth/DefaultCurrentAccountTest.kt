package de.tabmates.core.data.auth

import de.tabmates.core.domain.auth.AuthInfo
import de.tabmates.core.domain.auth.SessionStorage
import de.tabmates.core.domain.auth.StaleSession
import de.tabmates.core.domain.auth.StaleSessionStore
import de.tabmates.core.domain.auth.User
import de.tabmates.core.domain.auth.UserType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DefaultCurrentAccountTest {
    @Test
    fun `resolves the signed-in account`() {
        val currentAccount =
            DefaultCurrentAccount(
                sessionStorage = FakeSessionStorage(authInfo("user-1")),
                staleSessionStore = FakeStaleSessionStore(),
            )

        assertEquals("user-1", currentAccount.userId())
    }

    @Test
    fun `falls back to the expired account so balances stay owned`() {
        // The regression: with only the session to go on, an expired session made every balance
        // compute against an empty id — so every group read as settled and every expense as
        // "not involved", as if a stranger had picked up the device.
        val currentAccount =
            DefaultCurrentAccount(
                sessionStorage = FakeSessionStorage(null),
                staleSessionStore = FakeStaleSessionStore(staleSession("user-1")),
            )

        assertEquals("user-1", currentAccount.userId())
    }

    @Test
    fun `prefers the live session over a leftover record`() {
        val currentAccount =
            DefaultCurrentAccount(
                sessionStorage = FakeSessionStorage(authInfo("user-2")),
                staleSessionStore = FakeStaleSessionStore(staleSession("user-1")),
            )

        assertEquals("user-2", currentAccount.userId())
    }

    @Test
    fun `is null when the device holds no account`() {
        val currentAccount =
            DefaultCurrentAccount(
                sessionStorage = FakeSessionStorage(null),
                staleSessionStore = FakeStaleSessionStore(),
            )

        assertNull(currentAccount.userId())
    }

    private fun authInfo(userId: String) =
        AuthInfo(
            accessToken = "access",
            refreshToken = "refresh",
            user =
                User(
                    id = userId,
                    email = "user@test.com",
                    username = "alice",
                    hasVerifiedEmail = true,
                    userType = UserType.REGISTERED,
                ),
        )

    private fun staleSession(userId: String) =
        StaleSession(
            userId = userId,
            email = "user@test.com",
            username = "alice",
            userType = UserType.REGISTERED,
        )

    private class FakeSessionStorage(
        private val initial: AuthInfo?,
    ) : SessionStorage {
        override val authState: StateFlow<AuthInfo?> = MutableStateFlow(initial)

        override fun get(): AuthInfo? = initial

        override fun set(info: AuthInfo?) = Unit
    }

    private class FakeStaleSessionStore(
        private val initial: StaleSession? = null,
    ) : StaleSessionStore {
        override val state: StateFlow<StaleSession?> = MutableStateFlow(initial)

        override fun get(): StaleSession? = initial

        override fun set(session: StaleSession?) = Unit

        override fun clear() = Unit
    }
}
