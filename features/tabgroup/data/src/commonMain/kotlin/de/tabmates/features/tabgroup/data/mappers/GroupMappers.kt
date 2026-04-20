package de.tabmates.features.tabgroup.data.mappers

import de.tabmates.features.tabgroup.data.dto.GroupDto
import de.tabmates.features.tabgroup.domain.models.Group

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
