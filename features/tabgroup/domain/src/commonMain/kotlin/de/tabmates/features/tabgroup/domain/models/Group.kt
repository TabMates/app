package de.tabmates.features.tabgroup.domain.models

import kotlin.time.Instant

data class Group(
    val id: String,
    val title: String,
    val description: String?,
    val defaultCurrencyCode: String,
    val participants: Set<GroupParticipant>,
    val creator: GroupParticipant,
    val lastActivityAt: Instant,
    val lastTabEntry: TabEntry?,
    val createdAt: Instant,
)
