package de.tabmates.features.tabgroup.data.mappers

import de.tabmates.features.tabgroup.data.dto.TabEntrySplitDto
import de.tabmates.features.tabgroup.data.network.dto.WsSplitDto
import de.tabmates.features.tabgroup.database.entities.TabEntrySplitEntity
import de.tabmates.features.tabgroup.database.entities.types.SplitTypeDatabase
import de.tabmates.features.tabgroup.domain.models.SplitType
import de.tabmates.features.tabgroup.domain.models.TabEntrySplit

fun TabEntrySplitDto.toDomain(tabEntryId: String): TabEntrySplit {
    val (type, value) = split.toSplitTypeAndValue()
    return TabEntrySplit(
        splitId = id,
        tabEntryId = tabEntryId,
        participantId = participantId,
        splitType = type,
        value = value,
        resolvedAmount = resolvedAmount,
    )
}

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

/** EQUAL has no wire value — value defaults to 0.0. */
fun WsSplitDto.toSplitTypeAndValue(): Pair<SplitType, Double> =
    when (this) {
        WsSplitDto.Equal -> SplitType.EQUAL to 0.0
        is WsSplitDto.ExactAmount -> SplitType.EXACT_AMOUNT to amount
        is WsSplitDto.Percentage -> SplitType.PERCENTAGE to percentage
        is WsSplitDto.Shares -> SplitType.SHARES to shares
    }

fun toWsSplit(
    splitType: SplitType,
    value: Double,
): WsSplitDto =
    when (splitType) {
        SplitType.EQUAL -> WsSplitDto.Equal
        SplitType.EXACT_AMOUNT -> WsSplitDto.ExactAmount(value)
        SplitType.PERCENTAGE -> WsSplitDto.Percentage(value)
        SplitType.SHARES -> WsSplitDto.Shares(value)
    }
