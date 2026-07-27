package de.tabmates.features.tabgroup.data.dto

import de.tabmates.features.tabgroup.domain.activity.ActivityEntryType
import de.tabmates.features.tabgroup.domain.activity.ActivityEventType
import de.tabmates.features.tabgroup.domain.activity.ActivityField
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * One event from `GET /api/activity`, and — byte for byte — the payload of an `ACTIVITY_EVENT`
 * WebSocket frame, so one mapper serves both paths.
 *
 * The enum properties go through [UnknownTolerantEnumSerializer] rather than the generated enum
 * serializer: a constant this build does not know must land on `UNKNOWN` and still render, not throw
 * and take the whole page — and with it the sync cursor — down with it.
 */
@Serializable
data class ActivityEventDto(
    val id: String,
    val seq: Long,
    val groupId: String,
    val occurredAt: Instant,
    val actorUserId: String,
    @Serializable(with = ActivityEventTypeSerializer::class)
    val type: ActivityEventType,
    val tabEntryId: String? = null,
    @Serializable(with = ActivityEntryTypeSerializer::class)
    val entryType: ActivityEntryType? = null,
    val entryTitle: String? = null,
    val amount: Double? = null,
    val currencyCode: String? = null,
    val targetUserId: String? = null,
    val targetUsername: String? = null,
    val entryVersion: Int? = null,
    val changes: List<ActivityChangeDto> = emptyList(),
)

@Serializable
data class ActivityChangeDto(
    @Serializable(with = ActivityFieldSerializer::class)
    val field: ActivityField,
    val oldValue: String? = null,
    val newValue: String? = null,
)

@Serializable
data class ActivityFeedResponseDto(
    val events: List<ActivityEventDto>,
    val nextCursor: Long? = null,
    val hasMore: Boolean = false,
)
