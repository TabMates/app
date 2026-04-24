package de.tabmates.features.tabgroup.data.mappers

import de.tabmates.features.tabgroup.data.dto.GroupParticipantDto
import de.tabmates.features.tabgroup.data.dto.ParticipantTypeDto
import de.tabmates.features.tabgroup.database.entities.GroupParticipantEntity
import de.tabmates.features.tabgroup.database.entities.types.ParticipantTypeDatabase
import de.tabmates.features.tabgroup.domain.models.GroupParticipant
import de.tabmates.features.tabgroup.domain.models.ParticipantType

fun GroupParticipantDto.toDomain(): GroupParticipant {
    return GroupParticipant(
        userId = userId,
        username = username,
        participantType = participantType.toDomain(),
    )
}

fun ParticipantTypeDto.toDomain(): ParticipantType {
    return when (this) {
        ParticipantTypeDto.REGISTERED -> ParticipantType.REGISTERED
        ParticipantTypeDto.ANONYMOUS -> ParticipantType.ANONYMOUS
        ParticipantTypeDto.PLACEHOLDER -> ParticipantType.PLACEHOLDER
    }
}

fun GroupParticipantEntity.toDomain(): GroupParticipant {
    return GroupParticipant(
        userId = userId,
        username = username,
        participantType = participantType.toDomain(),
    )
}

fun ParticipantTypeDatabase.toDomain(): ParticipantType {
    return when (this) {
        ParticipantTypeDatabase.REGISTERED -> ParticipantType.REGISTERED
        ParticipantTypeDatabase.ANONYMOUS -> ParticipantType.ANONYMOUS
        ParticipantTypeDatabase.PLACEHOLDER -> ParticipantType.PLACEHOLDER
    }
}
