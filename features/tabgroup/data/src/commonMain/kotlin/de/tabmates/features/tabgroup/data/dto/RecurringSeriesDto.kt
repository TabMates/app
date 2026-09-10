package de.tabmates.features.tabgroup.data.dto

import de.tabmates.features.tabgroup.data.network.dto.WsSplitDto
import kotlinx.datetime.LocalDate
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlin.time.Instant

/** A recurring schedule as the server reports it: identity, state, and its current template. */
@Serializable
data class RecurringSeriesDto(
    val id: String,
    val groupId: String,
    val entryType: RecurringEntryTypeDto,
    val isActive: Boolean,
    /**
     * The template names somebody who has left the group, so nothing is being generated until a
     * member repairs it. The only state that needs a human — surface it.
     */
    val needsAttention: Boolean,
    val createdAt: Instant,
    val createdBy: GroupParticipantDto,
    val updatedAt: Instant,
    val rule: RecurringRuleDto,
    /**
     * Future occurrences a member skipped. Defaulted so a server predating the field still parses —
     * at the cost, until it ships, of a skipped date rendering as a placeholder that never resolves.
     */
    val skippedOccurrenceDates: List<LocalDate> = emptyList(),
)

@Serializable
data class RecurringRuleDto(
    val id: String,
    val title: String,
    val description: String,
    val amount: Double,
    val currency: String,
    val exchangeRate: Double? = null,
    val paidBy: GroupParticipantDto,
    /** Set for `SETTLEMENT` schedules only. */
    val receivedBy: GroupParticipantDto? = null,
    /** Empty for `SETTLEMENT` schedules. */
    val splits: List<RecurringTemplateSplitDto> = emptyList(),
    val frequency: RecurrenceFrequencyDto,
    val interval: Int,
    val startDate: LocalDate,
    val end: RecurringEndDto,
)

@Serializable
data class RecurringTemplateSplitDto(
    val id: String? = null,
    val participantId: String,
    /**
     * Null when the participant is not one the payload otherwise names — a template can outlive the
     * membership of the people in it. [participantId] is always present.
     */
    val participant: GroupParticipantDto? = null,
    val split: WsSplitDto,
    val resolvedAmount: Double,
)

@Serializable
enum class RecurringEntryTypeDto {
    EXPENSE,
    INCOME,
    SETTLEMENT,
}

@Serializable
enum class RecurrenceFrequencyDto {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
}

/**
 * How a schedule stops. A sealed shape on the wire, discriminated by `type` — the server writes
 * `{"type":"NEVER"}`, `{"type":"UNTIL","date":...}`, `{"type":"COUNT","count":...}`.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed class RecurringEndDto {
    @Serializable
    @SerialName("NEVER")
    data object Never : RecurringEndDto()

    /** Inclusive: an occurrence landing exactly on [date] is still produced. */
    @Serializable
    @SerialName("UNTIL")
    data class Until(
        val date: LocalDate,
    ) : RecurringEndDto()

    /** Total occurrences, counting ones a skip left empty. */
    @Serializable
    @SerialName("COUNT")
    data class Count(
        val count: Int,
    ) : RecurringEndDto()
}
