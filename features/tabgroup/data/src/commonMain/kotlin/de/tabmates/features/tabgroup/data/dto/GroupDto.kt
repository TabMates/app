package de.tabmates.features.tabgroup.data.dto

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class GroupDto(
    val id: String,
    val participants: Set<GroupParticipantDto>,
    val creator: GroupParticipantDto,
    val lastActivityAt: Instant,
    val createdAt: Instant,
)
