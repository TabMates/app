package de.tabmates.features.tabgroup.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GroupInvitePreviewDto(
    @SerialName("groupName") val title: String,
    @SerialName("creatorUsername") val inviterUsername: String,
    val memberCount: Int,
    @SerialName("claimablePlaceholders") val placeholders: List<GroupParticipantDto> = emptyList(),
)
