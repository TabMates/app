package de.tabmates.features.tabgroup.database.entities

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import de.tabmates.features.tabgroup.database.entities.types.TabEntryTypeDatabase

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["groupId"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("groupId")],
)
data class TabEntryEntity(
    @PrimaryKey
    val tabEntryId: String,
    val title: String,
    val description: String,
    val amount: Float,
    val currencyCode: String,
    val entryType: TabEntryTypeDatabase,
    val groupId: String,
    val creatorId: String,
    val paidByUserId: String,
    val receivedByUserId: String,
    val createdAt: Long,
    val lastModifiedAt: Long,
    val lastModifiedByUserId: String,
    val deletedAt: Long?,
    val deletedByUserId: String?,
) {
    val isDeleted: Boolean
        get() = deletedAt != null
}
