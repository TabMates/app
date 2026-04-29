package de.tabmates.features.tabgroup.data.mappers

import de.tabmates.features.tabgroup.data.dto.SplitTypeDto
import de.tabmates.features.tabgroup.data.dto.TabEntrySplitDto
import de.tabmates.features.tabgroup.database.entities.TabEntrySplitEntity
import de.tabmates.features.tabgroup.database.entities.types.SplitTypeDatabase
import de.tabmates.features.tabgroup.domain.models.SplitType
import de.tabmates.features.tabgroup.domain.models.TabEntrySplit

fun TabEntrySplitDto.toDomain(tabEntryId: String): TabEntrySplit =
    TabEntrySplit(
        splitId = splitId,
        tabEntryId = tabEntryId,
        participantId = participant.userId,
        splitType = splitType.toDomain(),
        value = value,
        resolvedAmount = resolvedAmount,
    )

fun TabEntrySplit.toEntity(): TabEntrySplitEntity =
    TabEntrySplitEntity(
        splitId = splitId,
        tabEntryId = tabEntryId,
        participantId = participantId,
        splitType = splitType.toDatabase(),
        value = value,
        resolvedAmount = resolvedAmount,
    )

fun TabEntrySplitEntity.toDomain(): TabEntrySplit =
    TabEntrySplit(
        splitId = splitId,
        tabEntryId = tabEntryId,
        participantId = participantId,
        splitType = splitType.toDomain(),
        value = value,
        resolvedAmount = resolvedAmount,
    )

fun SplitTypeDto.toDomain(): SplitType =
    when (this) {
        SplitTypeDto.EQUAL -> SplitType.EQUAL
        SplitTypeDto.EXACT_AMOUNT -> SplitType.EXACT_AMOUNT
        SplitTypeDto.PERCENTAGE -> SplitType.PERCENTAGE
        SplitTypeDto.SHARES -> SplitType.SHARES
    }

fun SplitTypeDatabase.toDomain(): SplitType =
    when (this) {
        SplitTypeDatabase.EQUAL -> SplitType.EQUAL
        SplitTypeDatabase.EXACT_AMOUNT -> SplitType.EXACT_AMOUNT
        SplitTypeDatabase.PERCENTAGE -> SplitType.PERCENTAGE
        SplitTypeDatabase.SHARES -> SplitType.SHARES
    }

fun SplitType.toDatabase(): SplitTypeDatabase =
    when (this) {
        SplitType.EQUAL -> SplitTypeDatabase.EQUAL
        SplitType.EXACT_AMOUNT -> SplitTypeDatabase.EXACT_AMOUNT
        SplitType.PERCENTAGE -> SplitTypeDatabase.PERCENTAGE
        SplitType.SHARES -> SplitTypeDatabase.SHARES
    }
