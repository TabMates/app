package de.tabmates.features.tabgroup.database

import androidx.room3.Room
import androidx.room3.RoomDatabase

actual class DatabaseFactory {
    actual fun create(): RoomDatabase.Builder<TabMatesDatabase> {
        return Room.databaseBuilder(TabMatesDatabase.DATABASE_NAME)
    }
}
