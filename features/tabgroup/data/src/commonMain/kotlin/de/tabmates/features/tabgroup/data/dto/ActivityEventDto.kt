package de.tabmates.features.tabgroup.data.dto

import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * One event from `GET /api/activity`, and — byte for byte — the payload of an `ACTIVITY_EVENT`
 * WebSocket frame, so one mapper serves both paths.
 *
 * [type] and [ActivityChangeDto.field] are `String`, not enums, on purpose: `ignoreUnknownKeys`
 * guards unknown *fields*, not unknown *enum values*, so a real enum would hard-crash the whole page
 * the day the server adds a ninth type. They are widened to domain enums with an `UNKNOWN` member
 * instead.
 */
@Serializable
data class ActivityEventDto(
    val id: String,
    val seq: Long,
    val groupId: String,
    val occurredAt: Instant,
    val actorUserId: String,
    val type: String,
    val tabEntryId: String? = null,
    val entryType: String? = null,
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
    val field: String,
    val oldValue: String? = null,
    val newValue: String? = null,
)

@Serializable
data class ActivityFeedResponseDto(
    val events: List<ActivityEventDto>,
    val nextCursor: Long? = null,
    val hasMore: Boolean = false,
)
