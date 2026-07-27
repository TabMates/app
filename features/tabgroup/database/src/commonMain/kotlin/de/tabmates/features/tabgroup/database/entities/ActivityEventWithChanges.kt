package de.tabmates.features.tabgroup.database.entities

import androidx.room3.Embedded
import androidx.room3.Relation

data class ActivityEventWithChanges(
    @Embedded
    val event: ActivityEventEntity,
    @Relation(
        parentColumns = ["id"],
        entityColumns = ["activityEventId"],
    )
    val changes: List<ActivityFieldChangeEntity>,
)
