package de.tabmates.features.tabgroup.database.entities

import androidx.room3.Embedded
import androidx.room3.Junction
import androidx.room3.Relation
import de.tabmates.features.tabgroup.database.view.LastTabEntryView

data class GroupWithParticipants(
    @Embedded
    val group: GroupEntity,
    @Relation(
        parentColumn = "groupId",
        entityColumn = "userId",
        associateBy = Junction(GroupParticipantCrossRef::class),
    )
    val participants: List<GroupParticipantEntity>,
    @Relation(
        parentColumn = "groupId",
        entityColumn = "groupId",
        entity = LastTabEntryView::class,
    )
    val lastTabEntry: LastTabEntryWithSplits?,
)

data class GroupInfoEntity(
    @Embedded
    val group: GroupEntity,
    @Relation(
        parentColumn = "groupId",
        entityColumn = "userId",
        associateBy = Junction(GroupParticipantCrossRef::class),
    )
    val participants: List<GroupParticipantEntity>,
    @Relation(
        parentColumn = "groupId",
        entityColumn = "groupId",
        entity = TabEntryEntity::class,
    )
    val tabEntriesWithCreator: List<TabEntryWithCreator>,
)
