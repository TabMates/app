package de.tabmates.features.tabgroup.data.dto

import kotlinx.serialization.Serializable

enum class SplitTypeDto {
    EQUAL,
    EXACT_AMOUNT,
    PERCENTAGE,
    SHARES,
}

@Serializable
data class TabEntrySplitDto(
    val splitId: String,
    val participant: GroupParticipantDto,
    val splitType: SplitTypeDto,
    val value: Double,
    val resolvedAmount: Double,
)
