package de.tabmates.core.domain.auth

import kotlinx.coroutines.flow.StateFlow

/**
 * Durable record of a [StaleSession].
 *
 * A set record is what distinguishes an *expired* session from a deliberate sign-out: both leave
 * [SessionStorage] empty, but only the former must keep the local database intact. It outlives the
 * process so the distinction survives an app kill.
 */
interface StaleSessionStore {
    val state: StateFlow<StaleSession?>

    fun get(): StaleSession?

    fun set(session: StaleSession?)

    fun clear()
}
