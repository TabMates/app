package de.tabmates.features.tabgroup.domain.models

import kotlin.time.Instant

data class Group(
    val id: String,
    val participants: Set<GroupParticipant>,
    val creator: GroupParticipant,
    val lastActivityAt: Instant,
    val createdAt: Instant,
)
