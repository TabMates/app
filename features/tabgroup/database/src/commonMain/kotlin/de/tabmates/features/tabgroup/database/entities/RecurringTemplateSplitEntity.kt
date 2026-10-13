package de.tabmates.features.tabgroup.database.entities

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import de.tabmates.features.tabgroup.database.entities.types.SplitTypeDatabase

/** A participant's share in a recurring template, copied verbatim into every occurrence. */
@Entity(
    foreignKeys = [
        ForeignKey(
            entity = RecurringSeriesEntity::class,
            parentColumns = ["seriesId"],
            childColumns = ["seriesId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = GroupParticipantEntity::class,
            parentColumns = ["userId"],
            childColumns = ["participantId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("seriesId"),
        Index("participantId"),
    ],
)
data class RecurringTemplateSplitEntity(
    @PrimaryKey
    val splitId: String,
    val seriesId: String,
    val participantId: String,
    val splitType: SplitTypeDatabase,
    val value: Double,
    val resolvedAmount: Double,
)
