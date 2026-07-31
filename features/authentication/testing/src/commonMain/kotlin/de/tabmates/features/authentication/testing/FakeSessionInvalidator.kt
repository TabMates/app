package de.tabmates.features.authentication.testing

import de.tabmates.core.domain.auth.SessionInvalidationReason
import de.tabmates.core.domain.auth.SessionInvalidator

/**
 * Mirrors the real invalidator's observable effect — the session goes away — and records why, so
 * tests can tell an expiry apart from a deliberate sign-out.
 */
class FakeSessionInvalidator(
    private val sessionStorage: FakeSessionStorage = FakeSessionStorage(),
) : SessionInvalidator {
    val reasons: MutableList<SessionInvalidationReason> = mutableListOf()

    override fun invalidate(reason: SessionInvalidationReason) {
        reasons += reason
        sessionStorage.set(null)
    }
}
