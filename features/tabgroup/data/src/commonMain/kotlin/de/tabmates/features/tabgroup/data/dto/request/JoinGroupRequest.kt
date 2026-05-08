package de.tabmates.features.tabgroup.data.dto.request

import kotlinx.serialization.Serializable

@Serializable
data class JoinGroupRequest(
    val token: String,
)
