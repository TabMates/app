package de.tabmates.features.tabgroup.database.view

import androidx.room3.DatabaseView

@DatabaseView(
    viewName = "last_tab_entry_per_group",
    value = """
        SELECT te1.*
        FROM tabentryentity te1
        JOIN (
            SELECT groupId, MAX(createdAt) AS max_created_at
            FROM tabentryentity
            GROUP BY groupId
        ) te2 ON te1.groupId = te2.groupId AND te1.createdAt = te2.max_created_at
    """,
)
data class LastAddedTabEntryView(
    val tabEntryId: String,
    val groupId: String,
    val title: String,
    val creatorId: String,
    val createdAt: Long,
)
