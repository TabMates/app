package de.tabmates.features.tabgroup.database.entities

import androidx.room3.Embedded
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import androidx.room3.RoomWarnings

@Suppress(RoomWarnings.PRIMARY_KEY_FROM_EMBEDDED_IS_DROPPED)
@Entity
data class GroupEntity(
    @PrimaryKey
    val groupId: String,
    val title: String,
    val description: String?,
    val defaultCurrencyCode: String,
    @Embedded(prefix = "creator_")
    val creator: GroupParticipantEntity,
    val createdAt: Long,
    val lastModifiedAt: Long,
)
