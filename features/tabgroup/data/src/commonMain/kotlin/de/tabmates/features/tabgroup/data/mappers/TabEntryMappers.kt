package de.tabmates.features.tabgroup.data.mappers

import de.tabmates.features.tabgroup.data.dto.TabEntryDto
import de.tabmates.features.tabgroup.database.entities.LastTabEntryWithSplits
import de.tabmates.features.tabgroup.database.entities.TabEntryEntity
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
            )
        }
    }

fun TabEntry.toEntity(pendingSync: Boolean = false): TabEntryEntity =
    TabEntryEntity(
        tabEntryId = tabEntryId,
        title = title,
        description = description,
        amount = amount,
        currencyCode = currencyCode,
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
    )

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
