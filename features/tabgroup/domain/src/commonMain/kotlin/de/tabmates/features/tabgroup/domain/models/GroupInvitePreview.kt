package de.tabmates.features.tabgroup.domain.models

data class GroupInvitePreview(
    val title: String,
    val inviterUsername: String,
    val memberCount: Int,
    val placeholders: List<GroupParticipant>,
)
