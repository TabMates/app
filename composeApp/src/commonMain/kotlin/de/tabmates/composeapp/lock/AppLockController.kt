package de.tabmates.composeapp.lock

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.Single
import kotlin.time.Duration
import kotlin.time.TimeSource

/**
 * App-scoped holder for the biometric-lock runtime state.
 *
 * Lives as a singleton (outliving the per-composition [AppLockViewModel]) so the "unlocked for this
 * session" flag and the background timestamp survive ViewModel recreation on configuration change.
 * [unlocked] is `false` on a cold start, so an enabled lock engages before any content is shown.
 */
@Single
class AppLockController {
    private val _unlocked = MutableStateFlow(false)

    /** True once the user has authenticated (or logged in) this session; reset when re-locked. */
    val unlocked: StateFlow<Boolean> = _unlocked.asStateFlow()

    private var backgroundMark: TimeSource.Monotonic.ValueTimeMark? = null

    // Set while the OS prompt (incl. the device-credential screen, which backgrounds our activity)
    // is up, so returning to the foreground mid-auth doesn't spuriously re-lock.
    private var authInProgress = false

    /** Mark the app unlocked for this session (successful biometric auth, or a fresh sign-in). */
    fun markUnlocked() {
        _unlocked.value = true
        backgroundMark = null
    }

    fun markAuthStarted() {
        authInProgress = true
    }

    fun markAuthEnded() {
        authInProgress = false
    }

    /** Record when the app left the foreground, to measure the grace period on return. */
    fun onEnteredBackground() {
        if (authInProgress) return
        backgroundMark = TimeSource.Monotonic.markNow()
    }

    /** On return to the foreground, re-lock if we were backgrounded longer than [gracePeriod]. */
    fun onEnteredForeground(gracePeriod: Duration) {
        val mark = backgroundMark
        backgroundMark = null
        if (authInProgress) return
        if (mark != null && mark.elapsedNow() >= gracePeriod) {
            _unlocked.value = false
        }
    }
}
