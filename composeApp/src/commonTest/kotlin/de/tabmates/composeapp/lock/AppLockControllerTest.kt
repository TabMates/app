package de.tabmates.composeapp.lock

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

class AppLockControllerTest {
    @Test
    fun startsLocked() {
        assertFalse(AppLockController().unlocked.value)
    }

    @Test
    fun markUnlocked_unlocks() {
        val controller = AppLockController()
        controller.markUnlocked()
        assertTrue(controller.unlocked.value)
    }

    @Test
    fun backgroundBeyondGrace_relocks() {
        val controller = AppLockController()
        controller.markUnlocked()
        controller.onEnteredBackground()
        // Zero grace period -> any elapsed time re-locks on return.
        controller.onEnteredForeground(Duration.ZERO)
        assertFalse(controller.unlocked.value)
    }

    @Test
    fun backgroundWithinGrace_staysUnlocked() {
        val controller = AppLockController()
        controller.markUnlocked()
        controller.onEnteredBackground()
        controller.onEnteredForeground(1.hours)
        assertTrue(controller.unlocked.value)
    }

    @Test
    fun authInProgress_preventsRelockAcrossCredentialScreen() {
        val controller = AppLockController()
        controller.markUnlocked()
        // Simulates the device-credential screen backgrounding then foregrounding the activity.
        controller.markAuthStarted()
        controller.onEnteredBackground()
        controller.onEnteredForeground(Duration.ZERO)
        controller.markAuthEnded()
        assertTrue(controller.unlocked.value)
    }
}
