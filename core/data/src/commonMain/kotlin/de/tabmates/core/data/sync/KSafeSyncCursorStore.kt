package de.tabmates.core.data.sync

import de.tabmates.core.domain.sync.SyncCursorStore
import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.invoke
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import kotlin.time.Instant

/**
 * [SyncCursorStore] backed by the plaintext `prefs` KSafe (the cursor is not a secret). Stored as an
 * ISO-8601 string; an empty string represents "no cursor yet".
 */
@Single(binds = [SyncCursorStore::class])
class KSafeSyncCursorStore(
    @Named("prefs") prefs: KSafe,
) : SyncCursorStore {
    private var stored: String by prefs("", key = KEY_SYNC_CURSOR)

    override fun get(): Instant? = stored.takeIf { it.isNotEmpty() }?.let { Instant.parse(it) }

    override fun set(cursor: Instant) {
        stored = cursor.toString()
    }

    override fun clear() {
        stored = ""
    }

    private companion object {
        // Bumping the key forces one full sync on the next start, which rebuilds every local entry
        // from the server. V2: placeholder claims used to leave split-only entries out of delta
        // sync, so installs still carried splits naming the deleted placeholder.
        private const val KEY_SYNC_CURSOR = "tabSyncCursorV2"
    }
}
