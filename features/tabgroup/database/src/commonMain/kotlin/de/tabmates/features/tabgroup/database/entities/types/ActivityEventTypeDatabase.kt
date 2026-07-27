package de.tabmates.features.tabgroup.database.entities.types

/**
 * What a mirrored activity event describes. Names match the server's `ActivityType` exactly, since
 * rows are matched by name across the wire.
 *
 * [UNKNOWN] is the landing spot for a type this build does not recognise: the server may add one, and
 * an unmapped row must still render as a generic line rather than disappear from the feed.
 */
enum class ActivityEventTypeDatabase {
    ENTRY_CREATED,
    ENTRY_UPDATED,
    ENTRY_DELETED,
    MEMBER_JOINED,
    MEMBER_LEFT,

    /** Defined server-side but never emitted — there is no remove-another-member endpoint yet. */
    MEMBER_REMOVED,
    GROUP_CREATED,
    GROUP_UPDATED,
    UNKNOWN,
}
