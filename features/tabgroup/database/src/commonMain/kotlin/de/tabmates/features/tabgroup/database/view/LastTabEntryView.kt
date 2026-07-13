package de.tabmates.features.tabgroup.database.view

import androidx.room3.DatabaseView
import de.tabmates.features.tabgroup.database.entities.types.TabEntryTypeDatabase

@DatabaseView(
    viewName = "last_tab_entry_per_group",
    value = """
        SELECT te1.*
        FROM tabentryentity te1
        WHERE te1.deletedAt IS NULL
          AND te1.createdAt = (
              SELECT te2.createdAt
              FROM tabentryentity te2
              WHERE te2.groupId = te1.groupId AND te2.deletedAt IS NULL
              ORDER BY te2.entryDate DESC, te2.createdAt DESC
              LIMIT 1
          )
    """,
)
data class LastTabEntryView(
    val tabEntryId: String,
    val title: String,
    val description: String,
    val amount: Double,
    val currencyCode: String,
    val exchangeRate: Double?,
    val entryType: TabEntryTypeDatabase,
    val groupId: String,
    val creatorId: String,
    val paidByUserId: String,
    val receivedByUserId: String?,
    val entryDate: String,
    val createdAt: Long,
    val lastModifiedAt: Long,
    val lastModifiedByUserId: String,
    val version: Int,
    val deletedAt: Long?,
    val deletedByUserId: String?,
)
