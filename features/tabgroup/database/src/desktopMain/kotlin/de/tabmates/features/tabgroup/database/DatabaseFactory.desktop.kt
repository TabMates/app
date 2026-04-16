package de.tabmates.features.tabgroup.database

import androidx.room3.Room
import androidx.room3.RoomDatabase
import java.io.File

actual class DatabaseFactory {
    actual fun create(): RoomDatabase.Builder<TabMatesDatabase> {
        val databaseFile =
            File(
                System.getProperty("user.home"),
                ".tabmates/${TabMatesDatabase.DATABASE_NAME}",
            )
        databaseFile.parentFile?.mkdirs()
        return Room.databaseBuilder(databaseFile.absolutePath)
    }
}
