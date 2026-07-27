package de.tabmates.features.tabgroup.domain.activity

/**
 * Which kind of tab entry an entry event refers to. A flat enum rather than the [TabEntry]
 * [de.tabmates.features.tabgroup.domain.models.TabEntry] hierarchy: an activity row is a snapshot of
 * a past state, so it carries no splits or participants to model.
 */
enum class ActivityEntryType {
    EXPENSE,
    INCOME,
    SETTLEMENT,
    UNKNOWN,
}
