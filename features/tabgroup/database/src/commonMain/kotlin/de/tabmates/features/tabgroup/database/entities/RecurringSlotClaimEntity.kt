package de.tabmates.features.tabgroup.database.entities

import androidx.room3.Entity
import androidx.room3.Index

/**
 * A slot the server has written an entry into at some point.
 *
 * Deliberately **not** derived from the entries table, and deliberately never deleted when an entry
 * is. The server's uniqueness guarantee on `(series, occurrence date)` is not filtered by deletion:
 * a slot stays claimed forever, so an occurrence a member deleted on purpose is not regenerated.
 * Locally, though, a soft-deleted entry is dropped from the table outright — so without this record
 * the projector would see a free slot and render the deleted occurrence as a placeholder again, on
 * every projection, with no sync able to clear it.
 *
 * No foreign key to the series on purpose: a generated entry can reach this device before the
 * schedule that produced it does — a websocket broadcast arrives without waiting for a sync, and a
 * sync applies groups and entries in one pass. An FK would reject exactly the claim that matters
 * most. [groupId] carries the ownership instead, so claims are pruned when their group is.
 */
@Entity(
    primaryKeys = ["seriesId", "occurrenceDate"],
    indices = [
        Index("seriesId"),
        Index("groupId"),
    ],
)
data class RecurringSlotClaimEntity(
    val seriesId: String,
    /** ISO "YYYY-MM-DD". */
    val occurrenceDate: String,
    val groupId: String,
)
