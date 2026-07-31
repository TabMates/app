package de.tabmates.core.domain.sync

import kotlinx.coroutines.flow.Flow

/**
 * How many locally-made writes have not reached the server yet — i.e. exactly what is lost by
 * signing out or switching accounts. Used to put a number on those warnings.
 */
interface PendingWrites {
    fun observeCount(): Flow<Int>
}
