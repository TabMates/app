package de.tabmates.features.tabgroup.database

import androidx.room3.AutoMigration
import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import de.tabmates.features.tabgroup.database.dao.ActivityEventDao
import de.tabmates.features.tabgroup.database.dao.CurrencyDao
import de.tabmates.features.tabgroup.database.dao.ExchangeRateDao
import de.tabmates.features.tabgroup.database.dao.GroupDao
import de.tabmates.features.tabgroup.database.dao.GroupParticipantCrossRefDao
import de.tabmates.features.tabgroup.database.dao.GroupParticipantDao
import de.tabmates.features.tabgroup.database.dao.PendingOutboxDao
import de.tabmates.features.tabgroup.database.dao.TabEntryDao
import de.tabmates.features.tabgroup.database.dao.TabEntrySplitDao
import de.tabmates.features.tabgroup.database.entities.ActivityEventEntity
import de.tabmates.features.tabgroup.database.entities.ActivityFieldChangeEntity
import de.tabmates.features.tabgroup.database.entities.CurrencyEntity
import de.tabmates.features.tabgroup.database.entities.ExchangeRateEntity
import de.tabmates.features.tabgroup.database.entities.GroupEntity
import de.tabmates.features.tabgroup.database.entities.GroupParticipantCrossRef
import de.tabmates.features.tabgroup.database.entities.GroupParticipantEntity
import de.tabmates.features.tabgroup.database.entities.PendingOutboxEntity
import de.tabmates.features.tabgroup.database.entities.TabEntryEntity
import de.tabmates.features.tabgroup.database.entities.TabEntrySplitEntity
import de.tabmates.features.tabgroup.database.migrations.TabEntryEntryDateBackfill
import de.tabmates.features.tabgroup.database.view.LastTabEntryView

@Database(
    entities = [
        GroupEntity::class,
        GroupParticipantCrossRef::class,
        GroupParticipantEntity::class,
        TabEntryEntity::class,
        TabEntrySplitEntity::class,
        CurrencyEntity::class,
        ExchangeRateEntity::class,
        PendingOutboxEntity::class,
        ActivityEventEntity::class,
        ActivityFieldChangeEntity::class,
    ],
    views = [
        LastTabEntryView::class,
    ],
    version = 6,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4, spec = TabEntryEntryDateBackfill::class),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6),
    ],
)
@ConstructedBy(TabMatesDatabaseConstructor::class)
abstract class TabMatesDatabase : RoomDatabase() {
    abstract val groupDao: GroupDao
    abstract val groupParticipantCrossRefDao: GroupParticipantCrossRefDao
    abstract val groupParticipantDao: GroupParticipantDao
    abstract val tabEntryDao: TabEntryDao
    abstract val tabEntrySplitDao: TabEntrySplitDao
    abstract val currencyDao: CurrencyDao
    abstract val exchangeRateDao: ExchangeRateDao
    abstract val pendingOutboxDao: PendingOutboxDao
    abstract val activityEventDao: ActivityEventDao

    companion object {
        const val DATABASE_NAME = "tabmates.db"
    }
}
