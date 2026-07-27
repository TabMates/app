package de.tabmates.features.tabgroup.domain.activity

import kotlin.time.Instant

/**
 * A row in the activity feed: either an event the server has confirmed, or a local write still
 * waiting in the outbox.
 *
 * [Pending] rows are synthesized from the outbox so an offline write shows up immediately with the
 * same "Not synced" marker the group details use, and are retired as soon as the server's own event
 * for that write arrives.
 */
sealed interface ActivityFeedItem {
    val groupId: String?
    val occurredAt: Instant
    val type: ActivityEventType

    data class Persisted(
        val event: ActivityEvent,
    ) : ActivityFeedItem {
        override val groupId: String get() = event.groupId
        override val occurredAt: Instant get() = event.occurredAt
        override val type: ActivityEventType get() = event.type
    }

    /**
     * [groupId] is nullable only for delete rows written by a build that predates the outbox
     * snapshot; those render in the account feed and drain within one connection.
     */
    data class Pending(
        val outboxId: String,
        val tabEntryId: String,
        override val type: ActivityEventType,
        override val groupId: String?,
        override val occurredAt: Instant,
        val entryType: ActivityEntryType? = null,
        val entryTitle: String? = null,
        val amount: Double? = null,
        val currencyCode: String? = null,
    ) : ActivityFeedItem
}
