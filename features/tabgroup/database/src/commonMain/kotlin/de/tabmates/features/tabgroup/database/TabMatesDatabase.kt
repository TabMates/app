package de.tabmates.features.tabgroup.database

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import de.tabmates.features.tabgroup.database.dao.CurrencyDao
import de.tabmates.features.tabgroup.database.dao.ExchangeRateDao
import de.tabmates.features.tabgroup.database.dao.GroupDao
import de.tabmates.features.tabgroup.database.dao.GroupParticipantCrossRefDao
import de.tabmates.features.tabgroup.database.dao.GroupParticipantDao
import de.tabmates.features.tabgroup.database.dao.TabEntryDao
import de.tabmates.features.tabgroup.database.dao.TabEntrySplitDao
import de.tabmates.features.tabgroup.database.entities.CurrencyEntity
import de.tabmates.features.tabgroup.database.entities.ExchangeRateEntity
import de.tabmates.features.tabgroup.database.entities.GroupEntity
import de.tabmates.features.tabgroup.database.entities.GroupParticipantCrossRef
import de.tabmates.features.tabgroup.database.entities.GroupParticipantEntity
import de.tabmates.features.tabgroup.database.entities.TabEntryEntity
import de.tabmates.features.tabgroup.database.entities.TabEntrySplitEntity
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
    ],
    views = [
        LastTabEntryView::class,
    ],
    version = 1,
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

    companion object {
        const val DATABASE_NAME = "tabmates.db"
    }
}
