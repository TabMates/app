package de.tabmates.features.tabgroup.data.dto

import kotlinx.serialization.Serializable

enum class ParticipantTypeDto {
    REGISTERED,
    ANONYMOUS,
    PLACEHOLDER,
}

@Serializable
data class GroupParticipantDto(
    val userId: String,
    val username: String,
    val email: String?,
    val userType: ParticipantTypeDto,
)
