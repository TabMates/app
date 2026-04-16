package de.tabmates.features.tabgroup.database

import androidx.room3.RoomDatabaseConstructor

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object TabMatesDatabaseConstructor : RoomDatabaseConstructor<TabMatesDatabase> {
    override fun initialize(): TabMatesDatabase
}
