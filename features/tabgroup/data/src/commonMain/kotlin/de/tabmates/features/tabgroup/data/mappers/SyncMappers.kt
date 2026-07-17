package de.tabmates.features.tabgroup.data.mappers

import de.tabmates.features.tabgroup.data.dto.SyncResponseDto
import de.tabmates.features.tabgroup.domain.models.SyncSnapshot

fun SyncResponseDto.toDomain(): SyncSnapshot =
    SyncSnapshot(
        serverTime = serverTime,
        groups = groups.map { it.toDomain() },
        activeGroupIds = activeGroupIds,
        tabEntries = tabEntries.map { it.toDomain() },
        referencedParticipants =
            tabEntries
                .flatMap { it.referencedParticipants() }
                .distinctBy { it.userId }
                .map { it.toDomain() },
    )
