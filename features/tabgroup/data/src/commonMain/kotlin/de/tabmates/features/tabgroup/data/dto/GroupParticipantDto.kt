package de.tabmates.features.tabgroup.data.dto

import kotlinx.serialization.Serializable

enum class ParticipantTypeDto {
    REGISTERED,
    ANONYMOUS,
    PLACEHOLDER,
    DELETED,
}

@Serializable
data class GroupParticipantDto(
    val userId: String,
    val username: String,
    val userType: ParticipantTypeDto,
)
