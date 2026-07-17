package de.tabmates.features.tabgroup.domain.sync

import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Instant

/**
 * UI-facing view of the realtime sync link: whether the client currently receives live updates
 * and when local data was last known fresh.
 */
interface ConnectionStatusRepository {
    /** True while the realtime socket is connected and local data is live. */
    val isConnected: StateFlow<Boolean>

    /** The last moment local data was known fresh, or null if no sync has ever completed. */
    val lastServerContactAt: StateFlow<Instant?>
}
