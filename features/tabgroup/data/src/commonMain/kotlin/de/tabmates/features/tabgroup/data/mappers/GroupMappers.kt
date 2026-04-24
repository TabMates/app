package de.tabmates.features.tabgroup.data.mappers

import de.tabmates.features.tabgroup.data.dto.GroupDto
import de.tabmates.features.tabgroup.database.entities.GroupWithParticipants
import de.tabmates.features.tabgroup.domain.models.Group
import kotlin.time.Instant

fun GroupDto.toDomain(): Group {
    return Group(
        id = id,
        title = title,
        description = description,
        defaultCurrencyCode = defaultCurrencyCode,
        participants = participants.map { it.toDomain() }.toSet(),
        creator = creator.toDomain(),
        lastActivityAt = lastActivityAt,
        createdAt = createdAt,
    )
}

fun GroupWithParticipants.toDomain(): Group {
    return Group(
        id = group.groupId,
        title = group.title,
        description = group.description,
        defaultCurrencyCode = group.defaultCurrencyCode,
        participants = participants.map { it.toDomain() }.toSet(),
        creator = group.creator.toDomain(),
        lastActivityAt = Instant.fromEpochMilliseconds(group.lastModifiedAt),
        createdAt = Instant.fromEpochMilliseconds(group.createdAt),
    )
}
