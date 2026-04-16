package de.tabmates.features.tabgroup.database.entities

import androidx.room3.Embedded
import androidx.room3.Relation

data class TabEntryWithCreator(
    @Embedded
    val tabEntry: TabEntryEntity,
    @Relation(
        parentColumn = "creatorId",
        entityColumn = "userId",
    )
    val creator: GroupParticipantEntity,
)
