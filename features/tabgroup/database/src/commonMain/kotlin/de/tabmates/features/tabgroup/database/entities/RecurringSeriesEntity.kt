package de.tabmates.features.tabgroup.database.entities

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import de.tabmates.features.tabgroup.database.entities.types.RecurrenceFrequencyDatabase
import de.tabmates.features.tabgroup.database.entities.types.RecurringEndTypeDatabase
import de.tabmates.features.tabgroup.database.entities.types.TabEntryTypeDatabase

/**
 * A recurring schedule, mirrored from the server.
 *
 * The server keeps an append-only chain of template revisions but only ever ships the newest one,
 * so the current rule is flattened into this row rather than given a table of its own — there is no
 * local history to reconcile, and every read wants the series and its template together.
 */
@Entity(
    foreignKeys = [
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["groupId"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = GroupParticipantEntity::class,
            parentColumns = ["userId"],
            childColumns = ["createdByUserId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("groupId"),
        Index("createdByUserId"),
    ],
)
data class RecurringSeriesEntity(
    @PrimaryKey
    val seriesId: String,
    val groupId: String,
    val entryType: TabEntryTypeDatabase,
    val isActive: Boolean,
    /** The template names a former member; the server generates nothing until someone repairs it. */
    val needsAttention: Boolean,
    val createdAt: Long,
    val createdByUserId: String,
    val updatedAt: Long,
    // --- current rule revision ---
    val ruleId: String,
    val title: String,
    val description: String,
    val amount: Double,
    val currencyCode: String,
    /** Fallback only — the server resolves a live rate per occurrence when it writes one. */
    val exchangeRate: Double?,
    val paidByUserId: String,
    /** Set for SETTLEMENT series only. */
    val receivedByUserId: String?,
    val frequency: RecurrenceFrequencyDatabase,
    val intervalCount: Int,
    /** ISO "YYYY-MM-DD". The first occurrence, and the anchor every later date is computed from. */
    val startDate: String,
    val endType: RecurringEndTypeDatabase,
    /** Set only when [endType] is `UNTIL`. ISO "YYYY-MM-DD", inclusive. */
    val endUntilDate: String?,
    /** Set only when [endType] is `COUNT`. Counts occurrences a skip left empty. */
    val endCount: Int?,
)
