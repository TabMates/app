package de.tabmates.features.tabgroup.data.dto

import de.tabmates.features.tabgroup.data.network.dto.WsSplitDto
import kotlinx.serialization.Serializable

@Serializable
data class TabEntrySplitDto(
    val id: String,
    val participantId: String,
    val participant: GroupParticipantDto? = null,
    val split: WsSplitDto,
    val resolvedAmount: Double,
)
