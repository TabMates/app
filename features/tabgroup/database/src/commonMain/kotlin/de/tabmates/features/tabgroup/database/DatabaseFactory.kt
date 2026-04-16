package de.tabmates.features.tabgroup.database

import androidx.room3.RoomDatabase

expect class DatabaseFactory {
    fun create(): RoomDatabase.Builder<TabMatesDatabase>
}
