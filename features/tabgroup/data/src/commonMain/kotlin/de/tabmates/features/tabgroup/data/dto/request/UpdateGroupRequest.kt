package de.tabmates.features.tabgroup.data.dto.request

import kotlinx.serialization.Serializable

@Serializable
data class UpdateGroupRequest(
    val title: String,
    val description: String? = null,
    val defaultCurrencyCode: String,
)
