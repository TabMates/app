package de.tabmates.features.tabgroup.data.mappers

import de.tabmates.features.tabgroup.data.dto.GroupParticipantDto
import de.tabmates.features.tabgroup.data.dto.TabEntryDto
import de.tabmates.features.tabgroup.database.entities.LastTabEntryWithSplits
import de.tabmates.features.tabgroup.database.entities.RecurringSlotClaimEntity
import de.tabmates.features.tabgroup.database.entities.TabEntryEntity
import de.tabmates.features.tabgroup.database.entities.TabEntrySplitEntity
import de.tabmates.features.tabgroup.database.entities.TabEntryWithSplits
import de.tabmates.features.tabgroup.database.entities.types.TabEntryTypeDatabase
import de.tabmates.features.tabgroup.domain.models.TabEntry
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

fun TabEntryDto.toDomain(): TabEntry =
    when (this) {
        is TabEntryDto.Expense -> {
            TabEntry.Expense(
                tabEntryId = id,
                groupId = groupId,
                title = title,
                description = description,
                amount = amount,
                currencyCode = currency,
                exchangeRate = exchangeRate,
                creatorId = creator.userId,
                paidByUserId = paidBy.userId,
                entryDate = entryDate,
                createdAt = createdAt,
                lastModifiedAt = lastModifiedAt,
                lastModifiedByUserId = lastModifiedBy.userId,
                version = version,
                deletedAt = deletedAt,
                deletedByUserId = deletedBy?.userId,
                splits = splits.map { it.toDomain(tabEntryId = id) },
                recurringSeriesId = recurringSeriesId,
                recurringOccurrenceDate = recurringOccurrenceDate,
            )
        }

        is TabEntryDto.Income -> {
            TabEntry.Income(
                tabEntryId = id,
                groupId = groupId,
                title = title,
                description = description,
                amount = amount,
                currencyCode = currency,
                exchangeRate = exchangeRate,
                creatorId = creator.userId,
                paidByUserId = paidBy.userId,
                entryDate = entryDate,
                createdAt = createdAt,
                lastModifiedAt = lastModifiedAt,
                lastModifiedByUserId = lastModifiedBy.userId,
                version = version,
                deletedAt = deletedAt,
                deletedByUserId = deletedBy?.userId,
                splits = splits.map { it.toDomain(tabEntryId = id) },
                recurringSeriesId = recurringSeriesId,
                recurringOccurrenceDate = recurringOccurrenceDate,
            )
        }

        is TabEntryDto.Settlement -> {
            TabEntry.Settlement(
                tabEntryId = id,
                groupId = groupId,
                title = title,
                description = description,
                amount = amount,
                currencyCode = currency,
                exchangeRate = exchangeRate,
                creatorId = creator.userId,
                paidByUserId = paidBy.userId,
                entryDate = entryDate,
                createdAt = createdAt,
                lastModifiedAt = lastModifiedAt,
                lastModifiedByUserId = lastModifiedBy.userId,
                version = version,
                deletedAt = deletedAt,
                deletedByUserId = deletedBy?.userId,
                receivedByUserId = receivedBy.userId,
                recurringSeriesId = recurringSeriesId,
                recurringOccurrenceDate = recurringOccurrenceDate,
            )
        }
    }

/**
 * The recurring slot this entry filled, or null for a hand-created one.
 *
 * Recorded separately from the entry itself and never removed: a soft-deleted entry is dropped from
 * the local table outright, but the server keeps its slot claimed forever, so the claim is the only
 * thing that stops a deliberately deleted occurrence being projected as a placeholder again.
 */
fun TabEntryDto.recurringSlotClaim(): RecurringSlotClaimEntity? {
    val seriesId = recurringSeriesId ?: return null
    val occurrenceDate = recurringOccurrenceDate ?: return null
    return RecurringSlotClaimEntity(
        seriesId = seriesId,
        occurrenceDate = occurrenceDate.toString(),
        groupId = groupId,
    )
}

/**
 * Every participant this entry references. These may include users who are no longer group
 * members (left, removed, or deleted account) and are therefore absent from the group's
 * participant list — their rows must still exist locally to satisfy the split table's FK.
 */
fun TabEntryDto.referencedParticipants(): List<GroupParticipantDto> =
    buildList {
        add(creator)
        add(paidBy)
        add(lastModifiedBy)
        deletedBy?.let { add(it) }
        when (this@referencedParticipants) {
            is TabEntryDto.Expense -> splits.mapNotNullTo(this) { it.participant }
            is TabEntryDto.Income -> splits.mapNotNullTo(this) { it.participant }
            is TabEntryDto.Settlement -> add(receivedBy)
        }
    }

fun TabEntry.toEntity(pendingSync: Boolean = false): TabEntryEntity =
    TabEntryEntity(
        tabEntryId = tabEntryId,
        title = title,
        description = description,
        amount = amount,
        currencyCode = currencyCode,
        exchangeRate = exchangeRate,
        entryType = toDatabaseType(),
        groupId = groupId,
        creatorId = creatorId,
        paidByUserId = paidByUserId,
        receivedByUserId = (this as? TabEntry.Settlement)?.receivedByUserId,
        entryDate = entryDate.toString(),
        createdAt = createdAt.toEpochMilliseconds(),
        lastModifiedAt = lastModifiedAt.toEpochMilliseconds(),
        lastModifiedByUserId = lastModifiedByUserId,
        version = version,
        deletedAt = deletedAt?.toEpochMilliseconds(),
        deletedByUserId = deletedByUserId,
        pendingSync = pendingSync,
        recurringSeriesId = recurringSeriesId,
        recurringOccurrenceDate = recurringOccurrenceDate?.toString(),
    )

fun TabEntry.toSplitEntities(): List<TabEntrySplitEntity> =
    when (this) {
        is TabEntry.Expense -> splits.map { it.toEntity() }
        is TabEntry.Income -> splits.map { it.toEntity() }
        is TabEntry.Settlement -> emptyList()
    }

fun TabEntryWithSplits.toDomain(): TabEntry =
    when (tabEntry.entryType) {
        TabEntryTypeDatabase.EXPENSE -> {
            TabEntry.Expense(
                tabEntryId = tabEntry.tabEntryId,
                groupId = tabEntry.groupId,
                title = tabEntry.title,
                description = tabEntry.description,
                amount = tabEntry.amount,
                currencyCode = tabEntry.currencyCode,
                exchangeRate = tabEntry.exchangeRate,
                creatorId = tabEntry.creatorId,
                paidByUserId = tabEntry.paidByUserId,
                entryDate = LocalDate.parse(tabEntry.entryDate),
                createdAt = Instant.fromEpochMilliseconds(tabEntry.createdAt),
                lastModifiedAt = Instant.fromEpochMilliseconds(tabEntry.lastModifiedAt),
                lastModifiedByUserId = tabEntry.lastModifiedByUserId,
                version = tabEntry.version,
                deletedAt = tabEntry.deletedAt?.let { Instant.fromEpochMilliseconds(it) },
                deletedByUserId = tabEntry.deletedByUserId,
                splits = splits.map { it.toDomain() },
                isPendingSync = tabEntry.pendingSync,
                recurringSeriesId = tabEntry.recurringSeriesId,
                recurringOccurrenceDate = tabEntry.recurringOccurrenceDate?.let(LocalDate::parse),
            )
        }

        TabEntryTypeDatabase.INCOME -> {
            TabEntry.Income(
                tabEntryId = tabEntry.tabEntryId,
                groupId = tabEntry.groupId,
                title = tabEntry.title,
                description = tabEntry.description,
                amount = tabEntry.amount,
                currencyCode = tabEntry.currencyCode,
                exchangeRate = tabEntry.exchangeRate,
                creatorId = tabEntry.creatorId,
                paidByUserId = tabEntry.paidByUserId,
                entryDate = LocalDate.parse(tabEntry.entryDate),
                createdAt = Instant.fromEpochMilliseconds(tabEntry.createdAt),
                lastModifiedAt = Instant.fromEpochMilliseconds(tabEntry.lastModifiedAt),
                lastModifiedByUserId = tabEntry.lastModifiedByUserId,
                version = tabEntry.version,
                deletedAt = tabEntry.deletedAt?.let { Instant.fromEpochMilliseconds(it) },
                deletedByUserId = tabEntry.deletedByUserId,
                splits = splits.map { it.toDomain() },
                isPendingSync = tabEntry.pendingSync,
                recurringSeriesId = tabEntry.recurringSeriesId,
                recurringOccurrenceDate = tabEntry.recurringOccurrenceDate?.let(LocalDate::parse),
            )
        }

        TabEntryTypeDatabase.SETTLEMENT -> {
            TabEntry.Settlement(
                tabEntryId = tabEntry.tabEntryId,
                groupId = tabEntry.groupId,
                title = tabEntry.title,
                description = tabEntry.description,
                amount = tabEntry.amount,
                currencyCode = tabEntry.currencyCode,
                exchangeRate = tabEntry.exchangeRate,
                creatorId = tabEntry.creatorId,
                paidByUserId = tabEntry.paidByUserId,
                entryDate = LocalDate.parse(tabEntry.entryDate),
                createdAt = Instant.fromEpochMilliseconds(tabEntry.createdAt),
                lastModifiedAt = Instant.fromEpochMilliseconds(tabEntry.lastModifiedAt),
                lastModifiedByUserId = tabEntry.lastModifiedByUserId,
                version = tabEntry.version,
                deletedAt = tabEntry.deletedAt?.let { Instant.fromEpochMilliseconds(it) },
                deletedByUserId = tabEntry.deletedByUserId,
                receivedByUserId =
                    requireNotNull(tabEntry.receivedByUserId) {
                        "Settlement TabEntry ${tabEntry.tabEntryId} has null receivedByUserId"
                    },
                isPendingSync = tabEntry.pendingSync,
                recurringSeriesId = tabEntry.recurringSeriesId,
                recurringOccurrenceDate = tabEntry.recurringOccurrenceDate?.let(LocalDate::parse),
            )
        }
    }

fun LastTabEntryWithSplits.toDomain(): TabEntry =
    when (lastTabEntry.entryType) {
        TabEntryTypeDatabase.EXPENSE -> {
            TabEntry.Expense(
                tabEntryId = lastTabEntry.tabEntryId,
                groupId = lastTabEntry.groupId,
                title = lastTabEntry.title,
                description = lastTabEntry.description,
                amount = lastTabEntry.amount,
                currencyCode = lastTabEntry.currencyCode,
                exchangeRate = lastTabEntry.exchangeRate,
                creatorId = lastTabEntry.creatorId,
                paidByUserId = lastTabEntry.paidByUserId,
                entryDate = LocalDate.parse(lastTabEntry.entryDate),
                createdAt = Instant.fromEpochMilliseconds(lastTabEntry.createdAt),
                lastModifiedAt = Instant.fromEpochMilliseconds(lastTabEntry.lastModifiedAt),
                lastModifiedByUserId = lastTabEntry.lastModifiedByUserId,
                version = lastTabEntry.version,
                deletedAt = lastTabEntry.deletedAt?.let { Instant.fromEpochMilliseconds(it) },
                deletedByUserId = lastTabEntry.deletedByUserId,
                splits = splits.map { it.toDomain() },
            )
        }

        TabEntryTypeDatabase.INCOME -> {
            TabEntry.Income(
                tabEntryId = lastTabEntry.tabEntryId,
                groupId = lastTabEntry.groupId,
                title = lastTabEntry.title,
                description = lastTabEntry.description,
                amount = lastTabEntry.amount,
                currencyCode = lastTabEntry.currencyCode,
                exchangeRate = lastTabEntry.exchangeRate,
                creatorId = lastTabEntry.creatorId,
                paidByUserId = lastTabEntry.paidByUserId,
                entryDate = LocalDate.parse(lastTabEntry.entryDate),
                createdAt = Instant.fromEpochMilliseconds(lastTabEntry.createdAt),
                lastModifiedAt = Instant.fromEpochMilliseconds(lastTabEntry.lastModifiedAt),
                lastModifiedByUserId = lastTabEntry.lastModifiedByUserId,
                version = lastTabEntry.version,
                deletedAt = lastTabEntry.deletedAt?.let { Instant.fromEpochMilliseconds(it) },
                deletedByUserId = lastTabEntry.deletedByUserId,
                splits = splits.map { it.toDomain() },
            )
        }

        TabEntryTypeDatabase.SETTLEMENT -> {
            TabEntry.Settlement(
                tabEntryId = lastTabEntry.tabEntryId,
                groupId = lastTabEntry.groupId,
                title = lastTabEntry.title,
                description = lastTabEntry.description,
                amount = lastTabEntry.amount,
                currencyCode = lastTabEntry.currencyCode,
                exchangeRate = lastTabEntry.exchangeRate,
                creatorId = lastTabEntry.creatorId,
                paidByUserId = lastTabEntry.paidByUserId,
                entryDate = LocalDate.parse(lastTabEntry.entryDate),
                createdAt = Instant.fromEpochMilliseconds(lastTabEntry.createdAt),
                lastModifiedAt = Instant.fromEpochMilliseconds(lastTabEntry.lastModifiedAt),
                lastModifiedByUserId = lastTabEntry.lastModifiedByUserId,
                version = lastTabEntry.version,
                deletedAt = lastTabEntry.deletedAt?.let { Instant.fromEpochMilliseconds(it) },
                deletedByUserId = lastTabEntry.deletedByUserId,
                receivedByUserId =
                    requireNotNull(lastTabEntry.receivedByUserId) {
                        "Settlement TabEntry ${lastTabEntry.tabEntryId} has null receivedByUserId"
                    },
            )
        }
    }

private fun TabEntry.toDatabaseType(): TabEntryTypeDatabase =
    when (this) {
        is TabEntry.Expense -> TabEntryTypeDatabase.EXPENSE
        is TabEntry.Income -> TabEntryTypeDatabase.INCOME
        is TabEntry.Settlement -> TabEntryTypeDatabase.SETTLEMENT
    }
