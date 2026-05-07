package de.tabmates.features.tabgroup.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class WebSocketMessageDto(
    val type: String,
    val payload: String,
)
