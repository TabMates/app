package de.tabmates.features.tabgroup.data.mappers

import de.tabmates.features.tabgroup.data.dto.GroupParticipantDto
import de.tabmates.features.tabgroup.data.dto.NewRecurringTemplateSplitDto
import de.tabmates.features.tabgroup.data.dto.RecurrenceFrequencyDto
import de.tabmates.features.tabgroup.data.dto.RecurringEndDto
import de.tabmates.features.tabgroup.data.dto.RecurringEntryTypeDto
import de.tabmates.features.tabgroup.data.dto.RecurringSeriesDto
import de.tabmates.features.tabgroup.data.dto.RecurringTemplateDto
import de.tabmates.features.tabgroup.database.entities.RecurringExceptionEntity
import de.tabmates.features.tabgroup.database.entities.RecurringSeriesEntity
import de.tabmates.features.tabgroup.database.entities.RecurringSeriesWithDetails
import de.tabmates.features.tabgroup.database.entities.RecurringTemplateSplitEntity
import de.tabmates.features.tabgroup.database.entities.types.RecurrenceFrequencyDatabase
import de.tabmates.features.tabgroup.database.entities.types.RecurringEndTypeDatabase
import de.tabmates.features.tabgroup.database.entities.types.TabEntryTypeDatabase
import de.tabmates.features.tabgroup.domain.models.GroupParticipant
import de.tabmates.features.tabgroup.domain.models.ParticipantType
import de.tabmates.features.tabgroup.domain.recurring.NewRecurringTemplateSplit
import de.tabmates.features.tabgroup.domain.recurring.RecurrenceFrequency
import de.tabmates.features.tabgroup.domain.recurring.RecurringEnd
import de.tabmates.features.tabgroup.domain.recurring.RecurringEntryType
import de.tabmates.features.tabgroup.domain.recurring.RecurringRule
import de.tabmates.features.tabgroup.domain.recurring.RecurringSeries
import de.tabmates.features.tabgroup.domain.recurring.RecurringTemplate
import de.tabmates.features.tabgroup.domain.recurring.RecurringTemplateSplit
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

// region wire -> domain

fun RecurringSeriesDto.toDomain(): RecurringSeries =
    RecurringSeries(
        seriesId = id,
        groupId = groupId,
        entryType = entryType.toDomain(),
        isActive = isActive,
        needsAttention = needsAttention,
        createdAt = createdAt,
        createdBy = createdBy.toDomain(),
        updatedAt = updatedAt,
        rule =
            RecurringRule(
                ruleId = rule.id,
                title = rule.title,
                description = rule.description,
                amount = rule.amount,
                currencyCode = rule.currency,
                exchangeRate = rule.exchangeRate,
                paidByUserId = rule.paidBy.userId,
                receivedByUserId = rule.receivedBy?.userId,
                splits =
                    rule.splits.map { split ->
                        val (type, value) = split.split.toSplitTypeAndValue()
                        RecurringTemplateSplit(
                            splitId = split.id,
                            participantId = split.participantId,
                            splitType = type,
                            value = value,
                            resolvedAmount = split.resolvedAmount,
                        )
                    },
                frequency = rule.frequency.toDomain(),
                interval = rule.interval,
                startDate = rule.startDate,
                end = rule.end.toDomain(),
            ),
        skippedOccurrenceDates = skippedOccurrenceDates.toSet(),
    )

/**
 * Every participant the series names, for the same foreign-key reason entries have one: a template
 * can outlive the membership of the people in it, so the payload's own participant lists are not
 * enough to keep the split and creator references valid.
 */
fun RecurringSeriesDto.referencedParticipants(): List<GroupParticipantDto> =
    buildList {
        add(createdBy)
        add(rule.paidBy)
        rule.receivedBy?.let(::add)
        rule.splits.forEach { split -> split.participant?.let(::add) }
    }

fun RecurringEntryTypeDto.toDomain(): RecurringEntryType =
    when (this) {
        RecurringEntryTypeDto.EXPENSE -> RecurringEntryType.EXPENSE
        RecurringEntryTypeDto.INCOME -> RecurringEntryType.INCOME
        RecurringEntryTypeDto.SETTLEMENT -> RecurringEntryType.SETTLEMENT
    }

fun RecurrenceFrequencyDto.toDomain(): RecurrenceFrequency =
    when (this) {
        RecurrenceFrequencyDto.DAILY -> RecurrenceFrequency.DAILY
        RecurrenceFrequencyDto.WEEKLY -> RecurrenceFrequency.WEEKLY
        RecurrenceFrequencyDto.MONTHLY -> RecurrenceFrequency.MONTHLY
        RecurrenceFrequencyDto.YEARLY -> RecurrenceFrequency.YEARLY
    }

fun RecurringEndDto.toDomain(): RecurringEnd =
    when (this) {
        RecurringEndDto.Never -> RecurringEnd.Never
        is RecurringEndDto.Until -> RecurringEnd.Until(date)
        is RecurringEndDto.Count -> RecurringEnd.Count(count)
    }

// endregion

// region domain -> wire

fun RecurringTemplate.toDto(): RecurringTemplateDto =
    when (entryType) {
        RecurringEntryType.EXPENSE -> {
            RecurringTemplateDto.Expense(
                paidByUserId = paidByUserId,
                title = title,
                description = description,
                amount = amount,
                currency = currencyCode,
                exchangeRate = exchangeRate,
                frequency = frequency.toDto(),
                interval = interval,
                startDate = startDate,
                end = end.toDto(),
                splits = splits.map { it.toDto() },
            )
        }

        RecurringEntryType.INCOME -> {
            RecurringTemplateDto.Income(
                paidByUserId = paidByUserId,
                title = title,
                description = description,
                amount = amount,
                currency = currencyCode,
                exchangeRate = exchangeRate,
                frequency = frequency.toDto(),
                interval = interval,
                startDate = startDate,
                end = end.toDto(),
                splits = splits.map { it.toDto() },
            )
        }

        RecurringEntryType.SETTLEMENT -> {
            RecurringTemplateDto.Settlement(
                paidByUserId = paidByUserId,
                title = title,
                description = description,
                amount = amount,
                currency = currencyCode,
                exchangeRate = exchangeRate,
                frequency = frequency.toDto(),
                interval = interval,
                startDate = startDate,
                end = end.toDto(),
                // A settlement template without a receiver is not something the server will store,
                // and the form cannot produce one; failing loudly beats posting a request that
                // comes back as an opaque 400.
                receivedByUserId =
                    requireNotNull(receivedByUserId) {
                        "a SETTLEMENT recurring template requires receivedByUserId"
                    },
            )
        }
    }

private fun NewRecurringTemplateSplit.toDto() =
    NewRecurringTemplateSplitDto(
        participantId = participantId,
        split = toWsSplit(splitType, value),
        resolvedAmount = resolvedAmount,
    )

fun RecurrenceFrequency.toDto(): RecurrenceFrequencyDto =
    when (this) {
        RecurrenceFrequency.DAILY -> RecurrenceFrequencyDto.DAILY
        RecurrenceFrequency.WEEKLY -> RecurrenceFrequencyDto.WEEKLY
        RecurrenceFrequency.MONTHLY -> RecurrenceFrequencyDto.MONTHLY
        RecurrenceFrequency.YEARLY -> RecurrenceFrequencyDto.YEARLY
    }

fun RecurringEnd.toDto(): RecurringEndDto =
    when (this) {
        RecurringEnd.Never -> RecurringEndDto.Never
        is RecurringEnd.Until -> RecurringEndDto.Until(date)
        is RecurringEnd.Count -> RecurringEndDto.Count(count)
    }

// endregion

// region domain -> entity

fun RecurringSeries.toEntity(): RecurringSeriesEntity =
    RecurringSeriesEntity(
        seriesId = seriesId,
        groupId = groupId,
        entryType = entryType.toDatabase(),
        isActive = isActive,
        needsAttention = needsAttention,
        createdAt = createdAt.toEpochMilliseconds(),
        createdByUserId = createdBy.userId,
        updatedAt = updatedAt.toEpochMilliseconds(),
        ruleId = rule.ruleId,
        title = rule.title,
        description = rule.description,
        amount = rule.amount,
        currencyCode = rule.currencyCode,
        exchangeRate = rule.exchangeRate,
        paidByUserId = rule.paidByUserId,
        receivedByUserId = rule.receivedByUserId,
        frequency = rule.frequency.toDatabase(),
        intervalCount = rule.interval,
        startDate = rule.startDate.toString(),
        endType = rule.end.toDatabaseType(),
        endUntilDate = (rule.end as? RecurringEnd.Until)?.date?.toString(),
        endCount = (rule.end as? RecurringEnd.Count)?.count,
    )

fun RecurringSeries.toSplitEntities(): List<RecurringTemplateSplitEntity> =
    rule.splits.map { split ->
        RecurringTemplateSplitEntity(
            // The server omits an id on a template split it has not persisted separately; deriving
            // one from the slot keeps the local primary key stable across re-syncs of the same rule.
            splitId = split.splitId ?: "${rule.ruleId}:${split.participantId}",
            seriesId = seriesId,
            participantId = split.participantId,
            splitType = split.splitType.toDatabase(),
            value = split.value,
            resolvedAmount = split.resolvedAmount,
        )
    }

fun RecurringSeries.toExceptionEntities(): List<RecurringExceptionEntity> =
    skippedOccurrenceDates.map { date ->
        RecurringExceptionEntity(seriesId = seriesId, occurrenceDate = date.toString())
    }

fun RecurringEntryType.toDatabase(): TabEntryTypeDatabase =
    when (this) {
        RecurringEntryType.EXPENSE -> TabEntryTypeDatabase.EXPENSE
        RecurringEntryType.INCOME -> TabEntryTypeDatabase.INCOME
        RecurringEntryType.SETTLEMENT -> TabEntryTypeDatabase.SETTLEMENT
    }

fun RecurrenceFrequency.toDatabase(): RecurrenceFrequencyDatabase =
    when (this) {
        RecurrenceFrequency.DAILY -> RecurrenceFrequencyDatabase.DAILY
        RecurrenceFrequency.WEEKLY -> RecurrenceFrequencyDatabase.WEEKLY
        RecurrenceFrequency.MONTHLY -> RecurrenceFrequencyDatabase.MONTHLY
        RecurrenceFrequency.YEARLY -> RecurrenceFrequencyDatabase.YEARLY
    }

private fun RecurringEnd.toDatabaseType(): RecurringEndTypeDatabase =
    when (this) {
        RecurringEnd.Never -> RecurringEndTypeDatabase.NEVER
        is RecurringEnd.Until -> RecurringEndTypeDatabase.UNTIL
        is RecurringEnd.Count -> RecurringEndTypeDatabase.COUNT
    }

// endregion

// region entity -> domain

fun RecurringSeriesWithDetails.toDomain(): RecurringSeries =
    RecurringSeries(
        seriesId = series.seriesId,
        groupId = series.groupId,
        entryType = series.entryType.toRecurringEntryType(),
        isActive = series.isActive,
        needsAttention = series.needsAttention,
        createdAt = Instant.fromEpochMilliseconds(series.createdAt),
        createdBy =
            createdBy?.toDomain()
                // A series always names its creator, but the participant row can be missing on a
                // device that has not synced them yet. A stand-in keeps the schedule renderable
                // rather than dropping it from the list entirely.
                ?: GroupParticipant(series.createdByUserId, "", ParticipantType.PLACEHOLDER),
        updatedAt = Instant.fromEpochMilliseconds(series.updatedAt),
        rule =
            RecurringRule(
                ruleId = series.ruleId,
                title = series.title,
                description = series.description,
                amount = series.amount,
                currencyCode = series.currencyCode,
                exchangeRate = series.exchangeRate,
                paidByUserId = series.paidByUserId,
                receivedByUserId = series.receivedByUserId,
                splits =
                    splits.map { split ->
                        RecurringTemplateSplit(
                            splitId = split.splitId,
                            participantId = split.participantId,
                            splitType = split.splitType.toDomain(),
                            value = split.value,
                            resolvedAmount = split.resolvedAmount,
                        )
                    },
                frequency = series.frequency.toDomain(),
                interval = series.intervalCount,
                startDate = LocalDate.parse(series.startDate),
                end =
                    when (series.endType) {
                        RecurringEndTypeDatabase.NEVER -> {
                            RecurringEnd.Never
                        }

                        RecurringEndTypeDatabase.UNTIL -> {
                            series.endUntilDate
                                ?.let { RecurringEnd.Until(LocalDate.parse(it)) }
                                ?: STOP_GENERATING
                        }

                        RecurringEndTypeDatabase.COUNT -> {
                            series.endCount
                                ?.let { RecurringEnd.Count(it) }
                                ?: STOP_GENERATING
                        }
                    },
            ),
        skippedOccurrenceDates = exceptions.mapTo(mutableSetOf()) { LocalDate.parse(it.occurrenceDate) },
    )

/**
 * The end rule an `UNTIL`/`COUNT` row missing its bound falls back to.
 *
 * [toEntity] always writes the bound alongside the type, so only a corrupted row reaches this. The
 * direction of the failure is what matters: [RecurringEnd.Never] would turn a schedule that should
 * have stopped into one that projects placeholders forever and moves everybody's balance, while a
 * zero count consumes no slots and produces nothing until the row is re-synced from the server.
 */
private val STOP_GENERATING = RecurringEnd.Count(0)

fun TabEntryTypeDatabase.toRecurringEntryType(): RecurringEntryType =
    when (this) {
        TabEntryTypeDatabase.EXPENSE -> RecurringEntryType.EXPENSE
        TabEntryTypeDatabase.INCOME -> RecurringEntryType.INCOME
        TabEntryTypeDatabase.SETTLEMENT -> RecurringEntryType.SETTLEMENT
    }

fun RecurrenceFrequencyDatabase.toDomain(): RecurrenceFrequency =
    when (this) {
        RecurrenceFrequencyDatabase.DAILY -> RecurrenceFrequency.DAILY
        RecurrenceFrequencyDatabase.WEEKLY -> RecurrenceFrequency.WEEKLY
        RecurrenceFrequencyDatabase.MONTHLY -> RecurrenceFrequency.MONTHLY
        RecurrenceFrequencyDatabase.YEARLY -> RecurrenceFrequency.YEARLY
    }

// endregion
