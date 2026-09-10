package de.tabmates.features.tabgroup.presentation.testing

import de.tabmates.features.tabgroup.domain.models.GroupParticipant
import de.tabmates.features.tabgroup.domain.models.SplitType
import de.tabmates.features.tabgroup.domain.recurring.RecurrenceFrequency
import de.tabmates.features.tabgroup.domain.recurring.RecurringEnd
import de.tabmates.features.tabgroup.domain.recurring.RecurringEntryType
import de.tabmates.features.tabgroup.domain.recurring.RecurringRule
import de.tabmates.features.tabgroup.domain.recurring.RecurringSeries
import de.tabmates.features.tabgroup.domain.recurring.RecurringTemplateSplit
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/** Builders for recurring schedules, kept next to [Fixtures] and used the same way. */
object RecurringFixtures {
    fun series(
        seriesId: String = "series-1",
        groupId: String = "g1",
        entryType: RecurringEntryType = RecurringEntryType.EXPENSE,
        isActive: Boolean = true,
        needsAttention: Boolean = false,
        amount: Double = 100.0,
        title: String = "Rent",
        paidByUserId: String = "user-1",
        receivedByUserId: String? = null,
        splits: List<RecurringTemplateSplit> = emptyList(),
        frequency: RecurrenceFrequency = RecurrenceFrequency.MONTHLY,
        interval: Int = 1,
        startDate: LocalDate = LocalDate.parse("2026-01-15"),
        end: RecurringEnd = RecurringEnd.Never,
        skipped: Set<LocalDate> = emptySet(),
        createdBy: GroupParticipant = Fixtures.participant(),
    ): RecurringSeries =
        RecurringSeries(
            seriesId = seriesId,
            groupId = groupId,
            entryType = entryType,
            isActive = isActive,
            needsAttention = needsAttention,
            createdAt = Instant.fromEpochMilliseconds(0),
            createdBy = createdBy,
            updatedAt = Instant.fromEpochMilliseconds(0),
            rule =
                RecurringRule(
                    ruleId = "$seriesId-rule",
                    title = title,
                    description = "",
                    amount = amount,
                    currencyCode = "EUR",
                    exchangeRate = null,
                    paidByUserId = paidByUserId,
                    receivedByUserId = receivedByUserId,
                    splits = splits,
                    frequency = frequency,
                    interval = interval,
                    startDate = startDate,
                    end = end,
                ),
            skippedOccurrenceDates = skipped,
        )

    fun templateSplit(
        participantId: String,
        resolvedAmount: Double,
        splitType: SplitType = SplitType.EQUAL,
        value: Double = 1.0,
    ): RecurringTemplateSplit =
        RecurringTemplateSplit(
            splitId = null,
            participantId = participantId,
            splitType = splitType,
            value = value,
            resolvedAmount = resolvedAmount,
        )
}
