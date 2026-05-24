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
)
