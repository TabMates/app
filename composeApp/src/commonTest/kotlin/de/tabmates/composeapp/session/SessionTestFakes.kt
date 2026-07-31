package de.tabmates.composeapp.session

import de.tabmates.core.domain.auth.StaleSession
import de.tabmates.core.domain.auth.StaleSessionStore
import de.tabmates.core.domain.auth.User
import de.tabmates.core.domain.auth.UserType
import de.tabmates.core.domain.sync.LocalDataResetter
import de.tabmates.core.domain.sync.PendingWrites
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeStaleSessionStore(
    initial: StaleSession? = null,
) : StaleSessionStore {
    private val internalState = MutableStateFlow(initial)

    override val state: StateFlow<StaleSession?> = internalState

    override fun get(): StaleSession? = internalState.value

    override fun set(session: StaleSession?) {
        internalState.value = session
    }

    override fun clear() = set(null)
}

class FakeLocalDataResetter : LocalDataResetter {
    var resetCalls: Int = 0
        private set

    override suspend fun resetLocalData() {
        resetCalls += 1
    }
}

class FakePendingWrites(
    initial: Int = 0,
) : PendingWrites {
    private val count = MutableStateFlow(initial)

    override fun observeCount(): Flow<Int> = count
}

fun staleSession(
    userId: String = "user-1",
    email: String? = "lena@example.com",
    userType: UserType = UserType.REGISTERED,
) = StaleSession(
    userId = userId,
    email = email,
    username = "Lena",
    userType = userType,
)

fun user(
    id: String = "user-1",
    userType: UserType = UserType.REGISTERED,
) = User(
    id = id,
    email = "lena@example.com",
    username = "Lena",
    hasVerifiedEmail = true,
    userType = userType,
)
