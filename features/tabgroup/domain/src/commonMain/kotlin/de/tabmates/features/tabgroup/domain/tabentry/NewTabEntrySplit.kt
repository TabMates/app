package de.tabmates.features.tabgroup.domain.tabentry

import de.tabmates.features.tabgroup.domain.models.SplitType

data class NewTabEntrySplit(
    val participantId: String,
    val splitType: SplitType,
    val value: Double,
)
