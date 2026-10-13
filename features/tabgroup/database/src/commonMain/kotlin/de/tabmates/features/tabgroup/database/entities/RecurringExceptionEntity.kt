package de.tabmates.features.tabgroup.database.entities

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index

/**
 * One future occurrence a member chose to skip.
 *
 * A skipped slot is still consumed, so this list is what keeps a `COUNT`-limited schedule from
 * running a period longer to make up for the skip — and what stops the skipped date being rendered
 * as a placeholder that never resolves.
 */
@Entity(
    primaryKeys = ["seriesId", "occurrenceDate"],
    foreignKeys = [
        ForeignKey(
            entity = RecurringSeriesEntity::class,
            parentColumns = ["seriesId"],
            childColumns = ["seriesId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("seriesId")],
)
data class RecurringExceptionEntity(
    val seriesId: String,
    /** ISO "YYYY-MM-DD". */
    val occurrenceDate: String,
)
