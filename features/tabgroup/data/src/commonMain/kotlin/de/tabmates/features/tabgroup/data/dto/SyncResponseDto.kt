package de.tabmates.features.tabgroup.data.dto

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class SyncResponseDto(
    val serverTime: Instant,
    val groups: List<GroupDto>,
    val activeGroupIds: List<String>,
    val tabEntries: List<TabEntryDto>,
    /**
     * Schedules created or changed since the cursor, active and ended alike. The entries they
     * produce need no separate treatment — they are ordinary rows in [tabEntries]. Defaulted so a
     * server predating the feature still parses.
     */
    val recurringSeries: List<RecurringSeriesDto> = emptyList(),
)
