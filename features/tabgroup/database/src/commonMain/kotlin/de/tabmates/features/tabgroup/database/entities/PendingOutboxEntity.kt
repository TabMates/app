package de.tabmates.features.tabgroup.database.entities

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * Persisted outbound write that hasn't been confirmed by the server yet.
 *
 * `id` is the deterministic key of the write (e.g. the local tab-entry id, or the entry id for
 * a deletion) so re-enqueuing the same write is idempotent.
 * `type` selects how the worker serializes/dispatches the payload.
 * `payload` holds the JSON body for the outgoing message.
 */
@Entity
data class PendingOutboxEntity(
    @PrimaryKey
    val id: String,
    val type: String,
    val payload: String,
    val createdAt: Long,
    val attemptCount: Int = 0,
    val lastAttemptAt: Long? = null,
    val lastError: String? = null,
    /**
     * The `TabEntryEntity.version` this write will produce once the server applies it — 0 for a
     * create, the current version + 1 for an update, null for a delete (which is terminal, so no
     * version math is needed).
     *
     * Exists so the activity feed can retire the synthesized "Not synced" row for this write the
     * moment the server's own event for it arrives, rather than showing the change twice.
     */
    val expectedVersion: Int? = null,
    /**
     * The correlation id the server acknowledges this write by. Stable for every retry of the
     * payload currently in [payload], and replaced whenever that payload is.
     *
     * Deliberately not [id]: update rows are keyed `update:<tabEntryId>` and upserted in place, so
     * reusing the row key would make a second edit of the same entry carry the first edit's id.
     * The server's replay cache would answer it with the first edit's ack and never apply the
     * second — a silently lost write.
     *
     * Nullable only for rows written before this column existed; the outbox mints one before their
     * first dispatch.
     */
    val requestId: String? = null,
)
