package de.tabmates.features.tabgroup.database.entities

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import de.tabmates.features.tabgroup.database.entities.types.SplitTypeDatabase

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = TabEntryEntity::class,
            parentColumns = ["tabEntryId"],
            childColumns = ["tabEntryId"],
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
        Index("tabEntryId"),
        Index("participantId"),
    ],
)
data class TabEntrySplitEntity(
    @PrimaryKey
    val splitId: String,
    val tabEntryId: String,
    val participantId: String,
    val splitType: SplitTypeDatabase,
    val value: Double,
    val resolvedAmount: Double,
)
