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
)
