package de.tabmates.features.tabgroup.domain.models

import kotlin.time.Instant

data class TabEntry(
    val tabEntryId: String,
    val title: String,
    val description: String,
    val amount: Double,
    val currencyCode: String,
    val entryType: TabEntryType,
    val groupId: String,
    val creatorId: String,
    val paidByUserId: String,
    val receivedByUserId: String?,
    val createdAt: Instant,
    val lastModifiedAt: Instant,
    val lastModifiedByUserId: String,
    val deletedAt: Instant?,
    val deletedByUserId: String?,
)
