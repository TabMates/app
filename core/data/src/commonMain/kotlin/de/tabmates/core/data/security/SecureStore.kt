package de.tabmates.core.data.security

import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.invoke
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

/**
 * Single entry point for encrypted, persisted values (Android Keystore / iOS Keychain backed,
 * via KSafe). Owns the vault wiring so storage classes (e.g. session, push token) don't each
 * reference `@Named("vault")`; they declare an encrypted property via [invoke]:
 *
 * ```
 * private var token: String? by secureStore(null, key = "pushToken")
 * ```
 */
@Single
class SecureStore(
    @Named("vault") @PublishedApi internal val vault: KSafe,
) {
    /** An encrypted, persisted property delegate for [key], defaulting to [default]. */
    inline operator fun <reified T> invoke(
        default: T,
        key: String,
    ) = vault(default, key)
}
