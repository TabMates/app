package de.tabmates.core.data.sync

import de.tabmates.core.data.di.APPLICATION_SCOPE
import de.tabmates.core.domain.sync.LastServerContactStore
import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.KSafeWriteMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * [LastServerContactStore] backed by the plaintext `prefs` KSafe (the timestamp is not a secret).
 * Stored as an ISO-8601 string; an empty string represents "never synced".
 */
@Single(binds = [LastServerContactStore::class])
class KSafeLastServerContactStore(
    @Named("prefs") private val prefs: KSafe,
    @Named(APPLICATION_SCOPE) applicationScope: CoroutineScope,
) : LastServerContactStore {
    override val lastContactAt: StateFlow<Instant?> =
        prefs
            .getFlow(KEY_LAST_SERVER_CONTACT, "")
            .map { it.toInstantOrNull() }
            .stateIn(
                applicationScope,
                SharingStarted.Eagerly,
                prefs.getDirect(KEY_LAST_SERVER_CONTACT, "").toInstantOrNull(),
            )

    override fun recordContactNow() {
        prefs.putDirect(KEY_LAST_SERVER_CONTACT, Clock.System.now().toString(), KSafeWriteMode.Plain)
    }

    override fun clear() {
        prefs.putDirect(KEY_LAST_SERVER_CONTACT, "", KSafeWriteMode.Plain)
    }

    private fun String.toInstantOrNull(): Instant? = takeIf { it.isNotEmpty() }?.let { Instant.parse(it) }

    private companion object {
        private const val KEY_LAST_SERVER_CONTACT = "lastServerContactAt"
    }
}
