package de.tabmates.features.tabgroup.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class GroupInvitePreviewDto(
    val groupId: String,
    val title: String,
    val inviterUsername: String,
    val memberCount: Int,
    val totalSpent: Double,
    val defaultCurrencyCode: String,
    val members: List<GroupParticipantDto> = emptyList(),
    val placeholders: List<GroupParticipantDto> = emptyList(),
)
