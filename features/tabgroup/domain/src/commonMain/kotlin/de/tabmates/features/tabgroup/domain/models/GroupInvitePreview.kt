package de.tabmates.features.tabgroup.domain.models

data class GroupInvitePreview(
    val groupId: String,
    val title: String,
    val inviterUsername: String,
    val memberCount: Int,
    val totalSpent: Double,
    val defaultCurrencyCode: String,
    val members: List<GroupParticipant>,
    val placeholders: List<GroupParticipant>,
)
