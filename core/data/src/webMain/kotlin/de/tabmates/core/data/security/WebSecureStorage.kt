package de.tabmates.core.data.security

import co.touchlab.kermit.Logger
import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.awaitCacheReady
import kotlinx.coroutines.CancellationException

/**
 * The web KSafe instances, created eagerly so [awaitSecureStorageReady] warms the exact
 * instances Koin later injects (a KSafe cache is per-instance, so warming a throwaway
 * instance would not help).
 */
internal object WebKSafeInstances {
    val prefs: KSafe = KSafe(fileName = "prefs")
    val vault: KSafe = KSafe(fileName = "vault")
}

/**
 * Suspends until both KSafe caches are loaded from localStorage and decrypted.
 *
 * On web, KSafe decrypts via WebCrypto, which is async-only: a synchronous read that races
 * the background preload returns the property's default value — e.g. a `null` session in
 * `KSafeSessionStorage`, logging the user out on every cold start. Call this from the web
 * entry point before starting the UI so the first read is deterministic.
 *
 * A preload failure degrades to the pre-warm behavior (cold defaults) instead of blocking
 * startup forever.
 */
suspend fun awaitSecureStorageReady() {
    try {
        WebKSafeInstances.prefs.awaitCacheReady()
        WebKSafeInstances.vault.awaitCacheReady()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // Runs before Koin starts, so the DI-provided TabMatesLogger isn't available yet.
        Logger.w(tag = "WebSecureStorage", throwable = e) { "KSafe cache preload failed, starting with defaults" }
    }
}
