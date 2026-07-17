package de.tabmates.features.tabgroup.domain.models

import kotlin.time.Instant

/**
 * Result of a `/api/sync` call. On an initial sync ([since][de.tabmates.features.tabgroup.domain.sync.SyncService]
 * called with `null`) [groups] and [tabEntries] hold the full account state; on a delta sync they
 * hold only what changed since the cursor.
 *
 * @property serverTime the new cursor to persist and echo back as `since` on the next sync.
 * @property groups groups created or changed (metadata or membership) since the cursor.
 * @property activeGroupIds the complete current set of the user's group ids (always full, never a
 *   delta) — used to prune groups the user no longer belongs to.
 * @property tabEntries entries changed since the cursor across all the user's groups, including
 *   soft-deleted ones (non-null [TabEntry.deletedAt]).
 * @property referencedParticipants every participant referenced by [tabEntries] (creator, payer,
 *   split members, …), deduplicated. May include users who are no longer members of any group
 *   (left, removed, or deleted account) and thus absent from [groups]' participant lists — they
 *   must still be persisted locally so entry/split rows referencing them stay valid.
 */
data class SyncSnapshot(
    val serverTime: Instant,
    val groups: List<Group>,
    val activeGroupIds: List<String>,
    val tabEntries: List<TabEntry>,
    val referencedParticipants: List<GroupParticipant> = emptyList(),
)
