package de.tabmates.features.tabgroup.database.entities

import androidx.room3.Embedded
import androidx.room3.Relation
import de.tabmates.features.tabgroup.database.view.LastTabEntryView

data class LastTabEntryWithSplits(
    @Embedded val lastTabEntry: LastTabEntryView,
    @Relation(
        parentColumns = ["tabEntryId"],
        entityColumns = ["tabEntryId"],
    )
    val splits: List<TabEntrySplitEntity>,
)
