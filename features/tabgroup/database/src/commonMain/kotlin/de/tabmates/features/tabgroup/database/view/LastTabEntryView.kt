package de.tabmates.features.tabgroup.database.view

import androidx.room3.DatabaseView
import de.tabmates.features.tabgroup.database.entities.types.TabEntryTypeDatabase

@DatabaseView(
    viewName = "last_tab_entry_per_group",
    value = """
        SELECT te1.*
        FROM tabentryentity te1
        JOIN (
            SELECT groupId, MAX(createdAt) AS max_created_at
            FROM tabentryentity
            WHERE deletedAt IS NULL
            GROUP BY groupId
        ) te2 ON te1.groupId = te2.groupId AND te1.createdAt = te2.max_created_at
        WHERE te1.deletedAt IS NULL
    """,
)
data class LastTabEntryView(
    val tabEntryId: String,
    val title: String,
    val description: String,
    val amount: Double,
    val currencyCode: String,
    val entryType: TabEntryTypeDatabase,
    val groupId: String,
    val creatorId: String,
    val paidByUserId: String,
    val receivedByUserId: String?,
    val createdAt: Long,
    val lastModifiedAt: Long,
    val lastModifiedByUserId: String,
    val version: Int,
    val deletedAt: Long?,
    val deletedByUserId: String?,
)
