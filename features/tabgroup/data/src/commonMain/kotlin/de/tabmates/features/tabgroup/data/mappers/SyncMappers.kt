package de.tabmates.features.tabgroup.data.mappers

import de.tabmates.features.tabgroup.data.dto.SyncResponseDto
import de.tabmates.features.tabgroup.domain.models.SyncSnapshot

fun SyncResponseDto.toDomain(): SyncSnapshot =
    SyncSnapshot(
        serverTime = serverTime,
        groups = groups.map { it.toDomain() },
        activeGroupIds = activeGroupIds,
        tabEntries = tabEntries.map { it.toDomain() },
        // Series participants join the same bucket as entry participants: both can name people
        // who are no longer members of any group, and both need a local row for their foreign keys.
        referencedParticipants =
            (
                tabEntries.flatMap { it.referencedParticipants() } +
                    recurringSeries.flatMap { it.referencedParticipants() }
            ).distinctBy { it.userId }
                .map { it.toDomain() },
        recurringSeries = recurringSeries.map { it.toDomain() },
    )
