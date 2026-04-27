package de.tabmates.features.tabgroup.data.mappers

import de.tabmates.features.tabgroup.data.dto.TabEntryDto
import de.tabmates.features.tabgroup.database.entities.TabEntryEntity
import de.tabmates.features.tabgroup.database.entities.types.TabEntryTypeDatabase
import de.tabmates.features.tabgroup.database.view.LastTabEntryView
import de.tabmates.features.tabgroup.domain.models.TabEntry
import de.tabmates.features.tabgroup.domain.models.TabEntryType
import kotlin.time.Instant

fun LastTabEntryView.toDomain(): TabEntry {
    return TabEntry(
        tabEntryId = tabEntryId,
        title = title,
        description = description,
        amount = amount,
        currencyCode = currencyCode,
        entryType = entryType.toDomain(),
        groupId = groupId,
        creatorId = creatorId,
        paidByUserId = paidByUserId,
        receivedByUserId = receivedByUserId,
        createdAt = Instant.fromEpochMilliseconds(createdAt),
        lastModifiedAt = Instant.fromEpochMilliseconds(lastModifiedAt),
        lastModifiedByUserId = lastModifiedByUserId,
        deletedAt = deletedAt?.let { Instant.fromEpochMilliseconds(it) },
        deletedByUserId = deletedByUserId,
    )
}

fun TabEntry.toLastTabEntryView(): LastTabEntryView {
    return LastTabEntryView(
        tabEntryId = tabEntryId,
        title = title,
        description = description,
        amount = amount,
        currencyCode = currencyCode,
        entryType = entryType.toDatabase(),
        groupId = groupId,
        creatorId = creatorId,
        paidByUserId = paidByUserId,
        receivedByUserId = receivedByUserId,
        createdAt = createdAt.toEpochMilliseconds(),
        lastModifiedAt = lastModifiedAt.toEpochMilliseconds(),
        lastModifiedByUserId = lastModifiedByUserId,
        deletedAt = deletedAt?.toEpochMilliseconds(),
        deletedByUserId = deletedByUserId,
    )
}

fun TabEntryDto.toDomain(): TabEntry {
    return TabEntry(
        tabEntryId = id,
        title = title,
        description = description,
        amount = amount,
        currencyCode = currency,
        entryType =
            when (this) {
                is TabEntryDto.Expense -> TabEntryType.EXPENSE
                is TabEntryDto.Income -> TabEntryType.INCOME
                is TabEntryDto.Settlement -> TabEntryType.SETTLEMENT
            },
        groupId = groupId,
        creatorId = creator.userId,
        paidByUserId = paidBy.userId,
        receivedByUserId = if (this is TabEntryDto.Settlement) receivedBy.userId else null,
        createdAt = createdAt,
        lastModifiedAt = lastModifiedAt,
        lastModifiedByUserId = lastModifiedBy.userId,
        deletedAt = deletedAt,
        deletedByUserId = deletedBy?.userId,
    )
}

fun TabEntry.toEntity(): TabEntryEntity {
    return TabEntryEntity(
        tabEntryId = tabEntryId,
        title = title,
        description = description,
        amount = amount,
        currencyCode = currencyCode,
        entryType = entryType.toDatabase(),
        groupId = groupId,
        creatorId = creatorId,
        paidByUserId = paidByUserId,
        receivedByUserId = receivedByUserId,
        createdAt = createdAt.toEpochMilliseconds(),
        lastModifiedAt = lastModifiedAt.toEpochMilliseconds(),
        lastModifiedByUserId = lastModifiedByUserId,
        deletedAt = deletedAt?.toEpochMilliseconds(),
        deletedByUserId = deletedByUserId,
    )
}

fun TabEntryType.toDatabase(): TabEntryTypeDatabase {
    return when (this) {
        TabEntryType.EXPENSE -> TabEntryTypeDatabase.EXPENSE
        TabEntryType.INCOME -> TabEntryTypeDatabase.INCOME
        TabEntryType.SETTLEMENT -> TabEntryTypeDatabase.SETTLEMENT
    }
}

fun TabEntryTypeDatabase.toDomain(): TabEntryType {
    return when (this) {
        TabEntryTypeDatabase.EXPENSE -> TabEntryType.EXPENSE
        TabEntryTypeDatabase.INCOME -> TabEntryType.INCOME
        TabEntryTypeDatabase.SETTLEMENT -> TabEntryType.SETTLEMENT
    }
}
