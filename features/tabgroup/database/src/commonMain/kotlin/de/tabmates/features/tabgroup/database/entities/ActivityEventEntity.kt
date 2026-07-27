package de.tabmates.features.tabgroup.database.entities

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import de.tabmates.features.tabgroup.database.entities.types.ActivityEventTypeDatabase
import de.tabmates.features.tabgroup.database.entities.types.TabEntryTypeDatabase

/**
 * One mirrored event from the server's append-only activity log (`GET /api/activity`).
 *
 * The whole log is mirrored and kept, so the feed reads offline all the way back to a group's
 * creation. The one exception is deliberate: the foreign key onto [GroupEntity] cascades, so leaving a
 * group (or it dropping out of `activeGroupIds`, or logging out) takes its activity with it. Keeping
 * those rows would leave a feed nothing can resolve names or titles for.
 *
 * Titles, amounts and [targetUsername] are snapshots as of the event, not lookups — that is what lets
 * an `ENTRY_DELETED` row still name what was deleted, and what stops a later rename from rewriting
 * history. The user id columns carry no foreign key for the same reason the server's do not: the
 * actor may since have left the group.
 */
@Entity(
    foreignKeys = [
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["groupId"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("groupId"),
        Index(value = ["seq"], unique = true),
        Index("tabEntryId"),
    ],
)
data class ActivityEventEntity(
    @PrimaryKey
    val id: String,
    /** Server-assigned cursor. The feed orders by this, never by [occurredAt]. */
    val seq: Long,
    val groupId: String,
    /** Display only. Ordering uses [seq], which is the only value guaranteed monotonic. */
    val occurredAt: Long,
    val actorUserId: String,
    val type: ActivityEventTypeDatabase,
    val tabEntryId: String? = null,
    val entryType: TabEntryTypeDatabase? = null,
    val entryTitle: String? = null,
    val amount: Double? = null,
    val currencyCode: String? = null,
    val targetUserId: String? = null,
    /** The target's name as it was at event time, so the row survives their removal. */
    val targetUsername: String? = null,
    /**
     * The `TabEntryEntity.version` this event produced — 0 for a create, incrementing per edit. Used
     * to retire a pending outbox row once the server's own event for that write has landed.
     */
    val entryVersion: Int? = null,
)
