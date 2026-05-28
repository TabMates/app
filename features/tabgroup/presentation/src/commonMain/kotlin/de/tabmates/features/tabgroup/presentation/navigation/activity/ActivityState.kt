package de.tabmates.features.tabgroup.presentation.navigation.activity

data class ActivityState(
    val isLoading: Boolean = true,
    val sections: List<ActivitySection> = emptyList(),
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

data class ActivityItem(
    val id: String,
    val groupId: String,
    val initials: String,
    val colorSeed: String,
    val actor: String,
    val actorIsYou: Boolean,
    val kind: ActivityKind,
    val subtitle: String,
    val isDeleted: Boolean,
)

/** What happened. The actor is rendered bold; [target] (expense/group name) is bold too when present. */
sealed interface ActivityKind {
    data class Added(val target: String) : ActivityKind

    data class Edited(val target: String) : ActivityKind

    data class Deleted(val target: String) : ActivityKind

    data class Created(val target: String) : ActivityKind

    data object SettledWithYou : ActivityKind

    data class SettledWith(val other: String) : ActivityKind
}
