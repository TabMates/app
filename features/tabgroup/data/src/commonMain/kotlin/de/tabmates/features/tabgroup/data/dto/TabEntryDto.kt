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
    abstract val entryDate: LocalDate
    abstract val createdAt: Instant
    abstract val lastModifiedAt: Instant
    abstract val lastModifiedBy: GroupParticipantDto
    abstract val version: Int
    abstract val deletedAt: Instant?
    abstract val deletedBy: GroupParticipantDto?

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
        val splits: List<TabEntrySplitDto>,
        override val entryDate: LocalDate,
        override val createdAt: Instant,
        override val lastModifiedAt: Instant,
        override val lastModifiedBy: GroupParticipantDto,
        override val version: Int,
        override val deletedAt: Instant?,
        override val deletedBy: GroupParticipantDto?,
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
        val splits: List<TabEntrySplitDto>,
        override val entryDate: LocalDate,
        override val createdAt: Instant,
        override val lastModifiedAt: Instant,
        override val lastModifiedBy: GroupParticipantDto,
        override val version: Int,
        override val deletedAt: Instant?,
        override val deletedBy: GroupParticipantDto?,
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
        val receivedBy: GroupParticipantDto,
        override val entryDate: LocalDate,
        override val createdAt: Instant,
        override val lastModifiedAt: Instant,
        override val lastModifiedBy: GroupParticipantDto,
        override val version: Int,
        override val deletedAt: Instant?,
        override val deletedBy: GroupParticipantDto?,
    ) : TabEntryDto()
}
