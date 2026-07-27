package de.tabmates.features.tabgroup.domain.activity

import kotlin.time.Instant

/**
 * One entry in the server's append-only activity log.
 *
 * Every field describing the subject ([entryTitle], [amount], [targetUsername], …) is a snapshot as
 * of the event, never a lookup — that is what lets an `ENTRY_DELETED` row still say what was deleted,
 * and stops a later rename from rewriting history. The exception is [ActivityFieldChange] values for
 * [ActivityField.PAID_BY] / [ActivityField.RECEIVED_BY], which carry raw user ids resolved at render
 * time, so a diff always names the person as they are known now.
 */
data class ActivityEvent(
    val id: String,
    /** Server-assigned cursor. The feed orders by this, never by [occurredAt]. */
    val seq: Long,
    val groupId: String,
    /** Display only — [seq] is the only value guaranteed monotonic. */
    val occurredAt: Instant,
    val actorUserId: String,
    val type: ActivityEventType,
    val tabEntryId: String? = null,
    val entryType: ActivityEntryType? = null,
    val entryTitle: String? = null,
    val amount: Double? = null,
    val currencyCode: String? = null,
    val targetUserId: String? = null,
    val targetUsername: String? = null,
    /** The entry version this event produced — 0 for a create, incrementing per edit. */
    val entryVersion: Int? = null,
    val changes: List<ActivityFieldChange> = emptyList(),
)

data class ActivityFieldChange(
    val field: ActivityField,
    val oldValue: String? = null,
    val newValue: String? = null,
)
