package de.tabmates.features.tabgroup.presentation.testing

import de.tabmates.features.tabgroup.domain.sync.ConnectionStatusRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Instant

/** Connected by default — the offline path is the exception a test opts into. */
class FakeConnectionStatusRepository(
    connected: Boolean = true,
    lastContactAt: Instant? = null,
) : ConnectionStatusRepository {
    private val connectedFlow = MutableStateFlow(connected)

    override val isConnected: StateFlow<Boolean> = connectedFlow
    override val lastServerContactAt: StateFlow<Instant?> = MutableStateFlow(lastContactAt)

    fun setConnected(value: Boolean) {
        connectedFlow.value = value
    }
}
