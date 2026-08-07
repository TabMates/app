package de.tabmates.features.tabgroup.domain.activity

/**
 * What an activity event describes. Names match the server's `ActivityType`.
 *
 * [UNKNOWN] exists because the wire format is a plain string: a server that grows a ninth type must
 * leave older clients rendering a generic line, not crashing or dropping the row.
 */
enum class ActivityEventType {
    ENTRY_CREATED,
    ENTRY_UPDATED,
    ENTRY_DELETED,
    MEMBER_JOINED,
    MEMBER_LEFT,

    /** One member removed another. Always names two different people, unlike [MEMBER_LEFT]. */
    MEMBER_REMOVED,
    GROUP_CREATED,
    GROUP_UPDATED,
    UNKNOWN,
    ;

    val isEntryEvent: Boolean
        get() = this == ENTRY_CREATED || this == ENTRY_UPDATED || this == ENTRY_DELETED
}
