package de.tabmates.features.tabgroup.data.dto

import de.tabmates.features.tabgroup.data.network.dto.WsSplitDto
import kotlinx.datetime.LocalDate
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

/**
 * The template half of a create or edit.
 *
 * Mirrors the outgoing entry payload's shape and discriminator deliberately: this is the same entry
 * the client would otherwise create by hand, plus when to repeat it.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("entryType")
sealed class RecurringTemplateDto {
    abstract val paidByUserId: String
    abstract val title: String
    abstract val description: String
    abstract val amount: Double
    abstract val currency: String

    /** Fallback rate only; the server resolves each occurrence's own rate when it writes one. */
    abstract val exchangeRate: Double?
    abstract val frequency: RecurrenceFrequencyDto
    abstract val interval: Int
    abstract val startDate: LocalDate
    abstract val end: RecurringEndDto

    @Serializable
    @SerialName("EXPENSE")
    data class Expense(
        override val paidByUserId: String,
        override val title: String,
        override val description: String,
        override val amount: Double,
        override val currency: String,
        override val exchangeRate: Double? = null,
        override val frequency: RecurrenceFrequencyDto,
        override val interval: Int = 1,
        override val startDate: LocalDate,
        override val end: RecurringEndDto = RecurringEndDto.Never,
        val splits: List<NewRecurringTemplateSplitDto>,
    ) : RecurringTemplateDto()

    @Serializable
    @SerialName("INCOME")
    data class Income(
        override val paidByUserId: String,
        override val title: String,
        override val description: String,
        override val amount: Double,
        override val currency: String,
        override val exchangeRate: Double? = null,
        override val frequency: RecurrenceFrequencyDto,
        override val interval: Int = 1,
        override val startDate: LocalDate,
        override val end: RecurringEndDto = RecurringEndDto.Never,
        val splits: List<NewRecurringTemplateSplitDto>,
    ) : RecurringTemplateDto()

    @Serializable
    @SerialName("SETTLEMENT")
    data class Settlement(
        override val paidByUserId: String,
        override val title: String,
        override val description: String,
        override val amount: Double,
        override val currency: String,
        override val exchangeRate: Double? = null,
        override val frequency: RecurrenceFrequencyDto,
        override val interval: Int = 1,
        override val startDate: LocalDate,
        override val end: RecurringEndDto = RecurringEndDto.Never,
        val receivedByUserId: String,
    ) : RecurringTemplateDto()
}

@Serializable
data class NewRecurringTemplateSplitDto(
    val participantId: String,
    val split: WsSplitDto,
    val resolvedAmount: Double,
)

@Serializable
data class CreateRecurringSeriesRequestDto(
    val groupId: String,
    /** Client-generated, so a create retried after a dropped response cannot make a second series. */
    val id: String,
    val template: RecurringTemplateDto,
)

/**
 * Applies a new template from [effectiveFrom] onwards.
 *
 * [effectiveFrom] must be a future date the current schedule actually produces, and must equal the
 * new template's `startDate` — otherwise the server rejects it rather than silently re-anchoring
 * the rhythm to whichever day the edit happened to be made.
 */
@Serializable
data class UpdateRecurringSeriesRequestDto(
    val effectiveFrom: LocalDate,
    val template: RecurringTemplateDto,
)

@Serializable
data class SkipRecurringOccurrenceRequestDto(
    val occurrenceDate: LocalDate,
)

/** Error bodies from the schedule endpoints, which state their refusals by code. */
@Serializable
internal data class RecurringSeriesErrorDto(
    val code: String? = null,
)
