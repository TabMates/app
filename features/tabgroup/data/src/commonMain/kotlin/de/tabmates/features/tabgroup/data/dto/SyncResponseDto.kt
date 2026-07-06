package de.tabmates.features.tabgroup.data.dto

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class SyncResponseDto(
    val serverTime: Instant,
    val groups: List<GroupDto>,
    val activeGroupIds: List<String>,
    val tabEntries: List<TabEntryDto>,
)
