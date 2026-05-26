package de.tabmates.features.tabgroup.data.dto.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JoinGroupRequest(
    val token: String,
    // Server expects `claimPlaceholderUserId` (JoinGroupWithInviteTokenDto); keep the wire name in sync.
    @SerialName("claimPlaceholderUserId") val claimPlaceholderId: String? = null,
)
