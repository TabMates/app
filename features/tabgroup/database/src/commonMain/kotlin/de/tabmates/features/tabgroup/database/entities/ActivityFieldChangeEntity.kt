package de.tabmates.features.tabgroup.database.entities

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import de.tabmates.features.tabgroup.database.entities.types.ActivityFieldDatabase

/**
 * One before/after pair belonging to an `ENTRY_UPDATED` / `GROUP_UPDATED` [ActivityEventEntity].
 *
 * A child table rather than a serialized column: this schema has no JSON-blob precedent and registers
 * no type converters, and [ActivityEventWithChanges] then reads the same way [TabEntryWithSplits]
 * does.
 *
 * The key is generated because rows are never addressed individually — they are rewritten wholesale
 * per event by `ActivityEventDao.upsertPage`.
 */
@Entity(
    foreignKeys = [
        ForeignKey(
            entity = ActivityEventEntity::class,
            parentColumns = ["id"],
            childColumns = ["activityEventId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("activityEventId")],
)
data class ActivityFieldChangeEntity(
    @PrimaryKey(autoGenerate = true)
    val changeId: Long = 0,
    val activityEventId: String,
    val field: ActivityFieldDatabase,
    /** Both values are null for [ActivityFieldDatabase.SPLITS], which is a flag rather than a diff. */
    val oldValue: String? = null,
    val newValue: String? = null,
)
