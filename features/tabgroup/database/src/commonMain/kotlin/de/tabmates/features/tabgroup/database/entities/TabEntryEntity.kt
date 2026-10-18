package de.tabmates.features.tabgroup.database.entities

import androidx.room3.ColumnInfo
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
    val amount: Double,
    val currencyCode: String,
    /**
     * Rate locked in at creation (group default currency per 1 unit of [currencyCode]);
     * null = no snapshot, convert with live rates.
     */
    val exchangeRate: Double? = null,
    val entryType: TabEntryTypeDatabase,
    val groupId: String,
    val creatorId: String,
    val paidByUserId: String,
    val receivedByUserId: String?,
    /** Calendar date the entry happened, ISO-8601 ("YYYY-MM-DD"). Distinct from [createdAt]. */
    @ColumnInfo(defaultValue = "1970-01-01")
    val entryDate: String,
    val createdAt: Long,
    val lastModifiedAt: Long,
    val lastModifiedByUserId: String,
    val version: Int,
    val deletedAt: Long?,
    val deletedByUserId: String?,
    /** True while this row is an optimistic local write awaiting server confirmation. */
    @ColumnInfo(defaultValue = "0")
    val pendingSync: Boolean = false,
    /**
     * The recurring series that produced this entry, and the slot it filled (ISO "YYYY-MM-DD").
     * Both null for a hand-created entry, both set for a generated one.
     *
     * The slot is deliberately not [entryDate]: a generated entry is ordinary once written and its
     * date stays editable, but the slot it occupies must not move with it.
     */
    val recurringSeriesId: String? = null,
    val recurringOccurrenceDate: String? = null,
) {
    val isDeleted: Boolean
        get() = deletedAt != null
}
