package de.tabmates.core.data.sync

import de.tabmates.core.domain.sync.ActivityCursorStore
import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.invoke
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

/**
 * [ActivityCursorStore] backed by the plaintext `prefs` KSafe (the cursor is not a secret). Stored
 * as a raw `Long`; `0` represents "no cursor yet", which is unambiguous because the server's `seq`
 * is a `BIGSERIAL` starting at 1.
 */
@Single(binds = [ActivityCursorStore::class])
class KSafeActivityCursorStore(
    @Named("prefs") prefs: KSafe,
) : ActivityCursorStore {
    private var stored: Long by prefs(0L, key = KEY_ACTIVITY_CURSOR)

    override fun get(): Long? = stored.takeIf { it > 0L }

    override fun set(cursor: Long) {
        stored = cursor
    }

    override fun clear() {
        stored = 0L
    }

    private companion object {
        private const val KEY_ACTIVITY_CURSOR = "activitySyncCursor"
    }
}
