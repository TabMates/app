package de.tabmates.features.tabgroup.database.entities

import androidx.room3.Embedded
import androidx.room3.Relation

data class TabEntryWithSplits(
    @Embedded
    val tabEntry: TabEntryEntity,
    @Relation(
        parentColumn = "tabEntryId",
        entityColumn = "tabEntryId",
    )
    val splits: List<TabEntrySplitEntity>,
)
