package de.tabmates.features.tabgroup.domain.models

/**
 * Result of fetching a group's complete tab-entry history for backfill.
 *
 * @property entries every entry in the group, including soft-deleted ones (non-null [TabEntry.deletedAt]).
 * @property referencedParticipants every participant referenced by [entries] (creator, payer, split
 *   members, …), deduplicated. May include users who are no longer members of the group (left,
 *   removed, or deleted account) and thus absent from the group's current participant list — they
 *   must still be persisted locally so entry/split rows referencing them stay valid.
 */
data class GroupTabEntryHistory(
    val entries: List<TabEntry>,
    val referencedParticipants: List<GroupParticipant> = emptyList(),
)
