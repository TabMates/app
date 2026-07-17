package de.tabmates.core.data.sync

import de.tabmates.core.domain.sync.PendingTabEntryBackfillStore
import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.invoke
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

/**
 * [PendingTabEntryBackfillStore] backed by the plaintext `prefs` KSafe (group ids are not
 * secrets), mirroring [KSafeSyncCursorStore].
 */
@Single(binds = [PendingTabEntryBackfillStore::class])
class KSafePendingTabEntryBackfillStore(
    @Named("prefs") prefs: KSafe,
) : PendingTabEntryBackfillStore {
    private var stored: Set<String> by prefs(emptySet(), key = KEY_PENDING_BACKFILL_GROUP_IDS)

    override fun getAll(): Set<String> = stored

    override fun add(groupId: String) {
        stored = stored + groupId
    }

    override fun remove(groupId: String) {
        stored = stored - groupId
    }

    override fun retainAll(activeGroupIds: Set<String>) {
        stored = stored intersect activeGroupIds
    }

    override fun clearAll() {
        stored = emptySet()
    }

    private companion object {
        private const val KEY_PENDING_BACKFILL_GROUP_IDS = "pendingTabEntryBackfillGroupIds"
    }
}
