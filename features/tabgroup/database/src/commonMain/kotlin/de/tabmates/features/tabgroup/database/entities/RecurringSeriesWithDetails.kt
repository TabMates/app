package de.tabmates.features.tabgroup.database.entities

import androidx.room3.Embedded
import androidx.room3.Relation

/** A recurring series with everything needed to render it and project its occurrences. */
data class RecurringSeriesWithDetails(
    @Embedded
    val series: RecurringSeriesEntity,
    @Relation(
        parentColumns = ["seriesId"],
        entityColumns = ["seriesId"],
    )
    val splits: List<RecurringTemplateSplitEntity>,
    @Relation(
        parentColumns = ["seriesId"],
        entityColumns = ["seriesId"],
    )
    val exceptions: List<RecurringExceptionEntity>,
    @Relation(
        parentColumns = ["createdByUserId"],
        entityColumns = ["userId"],
    )
    val createdBy: GroupParticipantEntity?,
)
