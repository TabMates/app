package de.tabmates.features.tabgroup.data.dto

import kotlinx.datetime.LocalDate
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlin.time.Instant

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("entryType")
sealed class TabEntryDto {
    abstract val id: String
    abstract val groupId: String
    abstract val creator: GroupParticipantDto
    abstract val paidBy: GroupParticipantDto
    abstract val title: String
    abstract val description: String
    abstract val amount: Double
    abstract val currency: String

    /**
     * Rate locked in at creation (group default currency per 1 unit of [currency]); null = no
     * snapshot. Defaults to null so responses from a server that predates the field still parse
     * (ignoreUnknownKeys only covers extra fields, not missing ones).
     */
    abstract val exchangeRate: Double?
    abstract val entryDate: LocalDate
    abstract val createdAt: Instant
    abstract val lastModifiedAt: Instant
    abstract val lastModifiedBy: GroupParticipantDto
    abstract val version: Int
    abstract val deletedAt: Instant?
    abstract val deletedBy: GroupParticipantDto?

    /**
     * The recurring series that produced this entry, and the slot it filled. Both null for a
     * hand-created entry, both set for a generated one; defaulted so a server predating the feature
     * still parses.
     *
     * The slot is [recurringOccurrenceDate], not [entryDate] — the latter stays editable once the
     * entry exists, and the slot must not move with it.
     */
    abstract val recurringSeriesId: String?
    abstract val recurringOccurrenceDate: LocalDate?

    @Serializable
    @SerialName("EXPENSE")
    data class Expense(
        override val id: String,
        override val groupId: String,
        override val creator: GroupParticipantDto,
        override val paidBy: GroupParticipantDto,
        override val title: String,
        override val description: String,
        override val amount: Double,
        override val currency: String,
        override val exchangeRate: Double? = null,
        val splits: List<TabEntrySplitDto>,
        override val entryDate: LocalDate,
        override val createdAt: Instant,
        override val lastModifiedAt: Instant,
        override val lastModifiedBy: GroupParticipantDto,
        override val version: Int,
        override val deletedAt: Instant?,
        override val deletedBy: GroupParticipantDto?,
        override val recurringSeriesId: String? = null,
        override val recurringOccurrenceDate: LocalDate? = null,
    ) : TabEntryDto()

    @Serializable
    @SerialName("INCOME")
    data class Income(
        override val id: String,
        override val groupId: String,
        override val creator: GroupParticipantDto,
        override val paidBy: GroupParticipantDto,
        override val title: String,
        override val description: String,
        override val amount: Double,
        override val currency: String,
        override val exchangeRate: Double? = null,
        val splits: List<TabEntrySplitDto>,
        override val entryDate: LocalDate,
        override val createdAt: Instant,
        override val lastModifiedAt: Instant,
        override val lastModifiedBy: GroupParticipantDto,
        override val version: Int,
        override val deletedAt: Instant?,
        override val deletedBy: GroupParticipantDto?,
        override val recurringSeriesId: String? = null,
        override val recurringOccurrenceDate: LocalDate? = null,
    ) : TabEntryDto()

    @Serializable
    @SerialName("SETTLEMENT")
    data class Settlement(
        override val id: String,
        override val groupId: String,
        override val creator: GroupParticipantDto,
        override val paidBy: GroupParticipantDto,
        override val title: String,
        override val description: String,
        override val amount: Double,
        override val currency: String,
        override val exchangeRate: Double? = null,
        val receivedBy: GroupParticipantDto,
        override val entryDate: LocalDate,
        override val createdAt: Instant,
        override val lastModifiedAt: Instant,
        override val lastModifiedBy: GroupParticipantDto,
        override val version: Int,
        override val deletedAt: Instant?,
        override val deletedBy: GroupParticipantDto?,
        override val recurringSeriesId: String? = null,
        override val recurringOccurrenceDate: LocalDate? = null,
    ) : TabEntryDto()
}
