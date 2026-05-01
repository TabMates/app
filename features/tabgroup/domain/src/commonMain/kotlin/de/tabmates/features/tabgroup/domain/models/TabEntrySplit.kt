package de.tabmates.features.tabgroup.domain.models

data class TabEntrySplit(
    val splitId: String,
    val tabEntryId: String,
    val participantId: String,
    val splitType: SplitType,
    val value: Double,
    val resolvedAmount: Double,
)
