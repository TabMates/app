package de.tabmates.features.tabgroup.presentation.util

import de.tabmates.features.tabgroup.domain.models.GroupParticipant

/**
 * Display order for people lists: the current user first, then A–Z by name.
 *
 * Names go through the platform collator (see [platformNameComparator]), so case and accents are
 * ignored and every script sorts the way the device locale expects. The raw name and then the id
 * break ties, so the order is stable.
 */
fun List<GroupParticipant>.sortedForDisplay(currentUserId: String?): List<GroupParticipant> {
    val names = platformNameComparator()
    return sortedWith(
        compareByDescending<GroupParticipant> { currentUserId != null && it.userId == currentUserId }
            .thenBy(names) { it.username }
            .thenBy { it.username }
            .thenBy { it.userId },
    )
}
