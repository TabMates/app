package de.tabmates.features.tabgroup.data.dto.request

import kotlinx.serialization.Serializable

@Serializable
data class CreateGroupRequest(
    val title: String,
    val description: String?,
    val defaultCurrencyCode: String,
    val otherParticipantUserIds: List<String>,
)
