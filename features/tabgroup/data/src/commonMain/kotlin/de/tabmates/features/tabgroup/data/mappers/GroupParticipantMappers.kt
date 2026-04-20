package de.tabmates.features.tabgroup.data.mappers

import de.tabmates.features.tabgroup.data.dto.GroupParticipantDto
import de.tabmates.features.tabgroup.data.dto.ParticipantTypeDto
import de.tabmates.features.tabgroup.database.entities.GroupParticipantEntity
import de.tabmates.features.tabgroup.database.entities.types.UserTypeDatabase
import de.tabmates.features.tabgroup.domain.models.GroupParticipant
import de.tabmates.features.tabgroup.domain.models.ParticipantType

fun GroupParticipantDto.toDomain(): GroupParticipant {
    return GroupParticipant(
        userId = userId,
        username = username,
        userType = participantType.toDomain(),
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
        userType = userType.toDomain(),
    )
}

fun UserTypeDatabase.toDomain(): ParticipantType {
    return when (this) {
        UserTypeDatabase.REGISTERED -> ParticipantType.REGISTERED
        UserTypeDatabase.ANONYMOUS -> ParticipantType.ANONYMOUS
        UserTypeDatabase.PLACEHOLDER -> ParticipantType.PLACEHOLDER
    }
}
