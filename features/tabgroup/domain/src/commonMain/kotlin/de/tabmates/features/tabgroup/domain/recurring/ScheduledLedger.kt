package de.tabmates.features.tabgroup.domain.recurring

import de.tabmates.features.tabgroup.domain.models.TabEntry
import kotlinx.coroutines.flow.Flow

/**
 * A group's entries as its ledger currently reads: what the server has written, plus the
 * occurrences its schedules already owe but nobody has written yet.
 *
 * This exists so there is exactly one answer to "what is this group's balance". Every screen that
 * shows a number for a group reads it from here — the group screen, the home summary, the group
 * list, the per-person breakdown. Projecting in some of them and not others is how the same group
 * ends up owing two different amounts on two different screens.
 *
 * The one deliberate exception is settling up, which builds real settlements out of these numbers
 * and must only ever act on entries that actually exist. It reads
 * [de.tabmates.features.tabgroup.domain.tabentry.TabEntryRepository] directly.
 */
interface ScheduledLedger {
    /**
     * Every entry of [groupId], followed by a placeholder for each occurrence that is due and
     * unwritten.
     *
     * A soft-deleted entry is not in the result: persistence removes the row outright rather than
     * keeping it flagged. Its slot stays claimed regardless — the server never releases one — and an
     * implementation has to honour those claims when it projects, or a deliberately deleted
     * occurrence comes back as a placeholder on every projection.
     */
    fun observeEntriesForGroup(groupId: String): Flow<List<TabEntry>>
}
