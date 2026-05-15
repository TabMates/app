package de.tabmates.features.tabgroup.data.dto.request

import kotlinx.serialization.Serializable

@Serializable
data class AddNewParticipantToGroupRequest(
    val usernames: List<String>,
)
