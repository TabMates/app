package de.tabmates.features.tabgroup.database.entities

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import de.tabmates.features.tabgroup.database.entities.types.ParticipantTypeDatabase

@Entity
data class GroupParticipantEntity(
    @PrimaryKey
    val userId: String,
    val username: String,
    val participantType: ParticipantTypeDatabase,
)
