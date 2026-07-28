package de.tabmates.features.tabgroup.presentation.navigation.activity

import de.tabmates.features.tabgroup.domain.activity.ActivityField

data class ActivityState(
    val isLoading: Boolean = true,
    val sections: List<ActivitySection> = emptyList(),
    /** False once the mirror has handed back fewer rows than asked for — the end of the log. */
    val canLoadMore: Boolean = false,
)

data class ActivitySection(
    val bucket: ActivityBucket,
    val items: List<ActivityItem>,
)

/** Date grouping for the feed. [OnDate] carries a 0-based [monthIndex] + day for header formatting. */
sealed interface ActivityBucket {
    data object Today : ActivityBucket

    data object Yesterday : ActivityBucket

    data class OnDate(val monthIndex: Int, val day: Int) : ActivityBucket
}

/**
 * One row. Every version of an entry is its own row, so an entry that was added and edited twice
 * produces three of these — they are not collapsed.
 */
data class ActivityItem(
    val id: String,
    val initials: String,
    val colorSeed: String,
    val actor: String,
    val actorIsYou: Boolean,
    val kind: ActivityKind,
    val subtitle: String,
    /** Field-level diff lines, empty for anything but an update. */
    val diffs: List<ActivityDiff> = emptyList(),
    /** A local write still in the outbox: rendered with the same "Not synced" chip as elsewhere. */
    val isPending: Boolean = false,
    /** A deletion row: struck through, and never clickable since the entry no longer exists. */
    val isDeleted: Boolean = false,
    val clickTarget: ActivityClickTarget = ActivityClickTarget.None,
)

/**
 * One before/after pair. Values arrive already formatted — the ViewModel resolves amounts, dates and
 * user names; the composable only supplies the [field] label.
 */
data class ActivityDiff(
    val field: ActivityField,
    val oldValue: String? = null,
    val newValue: String? = null,
)

/**
 * Where a row navigates. Every row of an entry that has since been deleted resolves to [None] — not
 * just the deletion row — as do unrecognised event types.
 */
sealed interface ActivityClickTarget {
    /** [groupId] travels with the id because entry detail routes are scoped to their group. */
    data class Entry(
        val tabEntryId: String,
        val groupId: String,
        val isSettlement: Boolean,
    ) : ActivityClickTarget

    data class Group(val groupId: String) : ActivityClickTarget

    data object None : ActivityClickTarget
}

/** What happened. The actor is rendered bold; the target (entry/group/member name) is bold too. */
sealed interface ActivityKind {
    data class EntryAdded(val target: String) : ActivityKind

    data class EntryEdited(val target: String) : ActivityKind

    data class EntryDeleted(val target: String) : ActivityKind

    data class GroupCreated(val target: String) : ActivityKind

    data class GroupUpdated(val target: String) : ActivityKind

    data class MemberJoined(val member: String) : ActivityKind

    data class MemberLeft(val member: String) : ActivityKind

    data class MemberRemoved(val member: String) : ActivityKind

    /** An event type this build does not know: renders a bare "{actor} · {group}" line. */
    data object Unknown : ActivityKind
}
