package de.tabmates.features.tabgroup.database

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase

actual class DatabaseFactory(
    private val context: Context,
) {
    actual fun create(): RoomDatabase.Builder<TabMatesDatabase> {
        val databaseFile = context.getDatabasePath(TabMatesDatabase.DATABASE_NAME)
        return Room.databaseBuilder(
            context.applicationContext,
            databaseFile.absolutePath,
        )
    }
}
