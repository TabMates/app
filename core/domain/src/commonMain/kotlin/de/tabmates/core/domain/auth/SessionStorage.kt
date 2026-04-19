package de.tabmates.core.domain.auth

import kotlinx.coroutines.flow.StateFlow

/**
 * Abstraction for reading and writing the persisted [AuthInfo].
 *
 * [authState] emits the current session reactively so observers
 * (e.g. the main UI) can respond to login/logout immediately.
 */
interface SessionStorage {
    val authState: StateFlow<AuthInfo?>

    fun get(): AuthInfo?

    fun set(info: AuthInfo?)
}
