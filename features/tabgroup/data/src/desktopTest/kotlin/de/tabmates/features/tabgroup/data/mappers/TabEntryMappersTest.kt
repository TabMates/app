package de.tabmates.features.tabgroup.data.mappers

import de.tabmates.features.tabgroup.data.dto.GroupParticipantDto
import de.tabmates.features.tabgroup.data.dto.ParticipantTypeDto
import de.tabmates.features.tabgroup.data.dto.TabEntryDto
import de.tabmates.features.tabgroup.data.dto.TabEntrySplitDto
import de.tabmates.features.tabgroup.data.network.dto.WsSplitDto
import de.tabmates.features.tabgroup.database.entities.types.ParticipantTypeDatabase
import de.tabmates.features.tabgroup.domain.models.ParticipantType
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class TabEntryMappersTest {
    private fun participantDto(
        id: String,
        type: ParticipantTypeDto = ParticipantTypeDto.REGISTERED,
    ): GroupParticipantDto = GroupParticipantDto(userId = id, username = "user-$id", userType = type)

    private fun splitDto(
        id: String,
        participantId: String,
        participant: GroupParticipantDto? = participantDto(participantId),
    ): TabEntrySplitDto =
        TabEntrySplitDto(
            id = id,
            participantId = participantId,
            participant = participant,
            split = WsSplitDto.Equal,
            resolvedAmount = 5.0,
        )

    private fun expenseDto(
        creator: GroupParticipantDto,
        paidBy: GroupParticipantDto = creator,
        lastModifiedBy: GroupParticipantDto = creator,
        deletedBy: GroupParticipantDto? = null,
        splits: List<TabEntrySplitDto> = emptyList(),
    ): TabEntryDto.Expense =
        TabEntryDto.Expense(
            id = "e1",
            groupId = "g1",
            creator = creator,
            paidBy = paidBy,
            title = "t",
            description = "",
            amount = 10.0,
            currency = "EUR",
            splits = splits,
            entryDate = LocalDate(2026, 1, 1),
            createdAt = Instant.fromEpochMilliseconds(0),
            lastModifiedAt = Instant.fromEpochMilliseconds(0),
            lastModifiedBy = lastModifiedBy,
            version = 0,
            deletedAt = null,
            deletedBy = deletedBy,
        )

    @Test
    fun referencedParticipantsCollectsAllRolesAndSplitParticipants() {
        val dto =
            expenseDto(
                creator = participantDto("creator"),
                paidBy = participantDto("payer"),
                lastModifiedBy = participantDto("editor"),
                deletedBy = participantDto("deleter"),
                splits =
                    listOf(
                        splitDto("s1", participantId = "member"),
                        // Server couldn't resolve this participant: no object attached.
                        splitDto("s2", participantId = "unresolved", participant = null),
                    ),
            )

        val ids = dto.referencedParticipants().map { it.userId }

        assertEquals(listOf("creator", "payer", "editor", "deleter", "member"), ids)
    }

    @Test
    fun referencedParticipantsIncludesSettlementReceiver() {
        val dto =
            TabEntryDto.Settlement(
                id = "e1",
                groupId = "g1",
                creator = participantDto("creator"),
                paidBy = participantDto("creator"),
                title = "t",
                description = "",
                amount = 10.0,
                currency = "EUR",
                receivedBy = participantDto("receiver"),
                entryDate = LocalDate(2026, 1, 1),
                createdAt = Instant.fromEpochMilliseconds(0),
                lastModifiedAt = Instant.fromEpochMilliseconds(0),
                lastModifiedBy = participantDto("creator"),
                version = 0,
                deletedAt = null,
                deletedBy = null,
            )

        val ids = dto.referencedParticipants().map { it.userId }

        assertEquals(listOf("creator", "creator", "creator", "receiver"), ids)
    }

    @Test
    fun deletedParticipantTypeDecodesAndMapsThroughAllLayers() {
        val dto =
            Json.decodeFromString(
                GroupParticipantDto.serializer(),
                """{"userId":"u1","username":"Max (deleted account)","userType":"DELETED"}""",
            )

        val domain = dto.toDomain()

        assertEquals(ParticipantType.DELETED, domain.participantType)
        assertEquals(ParticipantTypeDatabase.DELETED, domain.toEntity().participantType)
    }
}
