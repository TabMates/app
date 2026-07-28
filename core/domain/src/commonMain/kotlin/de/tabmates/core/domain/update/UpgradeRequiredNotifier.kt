package de.tabmates.core.domain.update

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Set once the backend has refused this build for being too old (HTTP 426).
 *
 * The app-update check only runs at startup, so a gate that turns on mid-session would otherwise
 * leave the user staring at generic error messages on every action. The network layer flips this
 * on the first 426 and the update gate turns it into the forced-update prompt.
 *
 * One-way on purpose: no request can make an outdated build current again, so there is nothing to
 * reset. It stays set until the process restarts with a new build.
 */
class UpgradeRequiredNotifier {
    private val _isUpgradeRequired = MutableStateFlow(false)
    val isUpgradeRequired: StateFlow<Boolean> = _isUpgradeRequired.asStateFlow()

    fun notifyUpgradeRequired() {
        _isUpgradeRequired.value = true
    }
}
