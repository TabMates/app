package de.tabmates.features.tabgroup.presentation.testing

import de.tabmates.features.tabgroup.domain.models.Group
import de.tabmates.features.tabgroup.domain.models.GroupParticipant
import de.tabmates.features.tabgroup.domain.models.ParticipantType
import de.tabmates.features.tabgroup.domain.models.SplitType
import de.tabmates.features.tabgroup.domain.models.TabEntry
import de.tabmates.features.tabgroup.domain.models.TabEntrySplit
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

object Fixtures {
    fun participant(
        id: String = "user-1",
        name: String = "Alice",
    ): GroupParticipant =
        GroupParticipant(
            userId = id,
            username = name,
            participantType = ParticipantType.REGISTERED,
        )

    fun group(
        id: String = "g1",
        title: String = "Trip",
        participants: Set<GroupParticipant> = setOf(participant()),
        currency: String = "EUR",
        activityEpochMs: Long = 0L,
    ): Group =
        Group(
            id = id,
            title = title,
            description = null,
            defaultCurrencyCode = currency,
            participants = participants,
            creator = participants.first(),
            inviteToken = "",
            lastActivityAt = Instant.fromEpochMilliseconds(activityEpochMs),
            lastTabEntry = null,
            createdAt = Instant.fromEpochMilliseconds(0),
        )

    fun expense(
        id: String = "e1",
        groupId: String = "g1",
        amount: Double = 100.0,
        paidByUserId: String = "user-1",
        splits: List<TabEntrySplit> = emptyList(),
        entryDate: LocalDate = LocalDate.parse("1970-01-01"),
        isPendingSync: Boolean = false,
    ): TabEntry.Expense =
        TabEntry.Expense(
            tabEntryId = id,
            groupId = groupId,
            title = "Expense",
            description = "",
            amount = amount,
            currencyCode = "EUR",
            creatorId = paidByUserId,
            paidByUserId = paidByUserId,
            entryDate = entryDate,
            createdAt = Instant.fromEpochMilliseconds(0),
            lastModifiedAt = Instant.fromEpochMilliseconds(0),
            lastModifiedByUserId = paidByUserId,
            version = 1,
            deletedAt = null,
            deletedByUserId = null,
            splits = splits,
            isPendingSync = isPendingSync,
        )

    fun income(
        id: String = "i1",
        groupId: String = "g1",
        amount: Double = 100.0,
        paidByUserId: String = "user-1",
        splits: List<TabEntrySplit> = emptyList(),
        entryDate: LocalDate = LocalDate.parse("1970-01-01"),
        isPendingSync: Boolean = false,
    ): TabEntry.Income =
        TabEntry.Income(
            tabEntryId = id,
            groupId = groupId,
            title = "Income",
            description = "",
            amount = amount,
            currencyCode = "EUR",
            creatorId = paidByUserId,
            paidByUserId = paidByUserId,
            entryDate = entryDate,
            createdAt = Instant.fromEpochMilliseconds(0),
            lastModifiedAt = Instant.fromEpochMilliseconds(0),
            lastModifiedByUserId = paidByUserId,
            version = 1,
            deletedAt = null,
            deletedByUserId = null,
            splits = splits,
            isPendingSync = isPendingSync,
        )

    fun settlement(
        id: String = "s1",
        groupId: String = "g1",
        amount: Double = 100.0,
        paidByUserId: String = "user-1",
        receivedByUserId: String = "user-2",
        entryDate: LocalDate = LocalDate.parse("1970-01-01"),
        isPendingSync: Boolean = false,
    ): TabEntry.Settlement =
        TabEntry.Settlement(
            tabEntryId = id,
            groupId = groupId,
            title = "Settlement",
            description = "",
            amount = amount,
            currencyCode = "EUR",
            creatorId = paidByUserId,
            paidByUserId = paidByUserId,
            entryDate = entryDate,
            createdAt = Instant.fromEpochMilliseconds(0),
            lastModifiedAt = Instant.fromEpochMilliseconds(0),
            lastModifiedByUserId = paidByUserId,
            version = 1,
            deletedAt = null,
            deletedByUserId = null,
            receivedByUserId = receivedByUserId,
            isPendingSync = isPendingSync,
        )

    fun split(
        tabEntryId: String = "e1",
        participantId: String = "user-1",
        resolvedAmount: Double = 50.0,
    ): TabEntrySplit =
        TabEntrySplit(
            splitId = "$tabEntryId-$participantId",
            tabEntryId = tabEntryId,
            participantId = participantId,
            splitType = SplitType.EQUAL,
            value = resolvedAmount,
            resolvedAmount = resolvedAmount,
        )
}
