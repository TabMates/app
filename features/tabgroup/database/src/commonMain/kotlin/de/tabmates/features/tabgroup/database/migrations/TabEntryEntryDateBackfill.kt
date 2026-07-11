package de.tabmates.features.tabgroup.database.migrations

import androidx.room3.migration.AutoMigrationSpec
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.async.executeSQL

/**
 * Backfills the new `entryDate` column (added in DB v4) from the existing `createdAt`
 * millisecond timestamp, mirroring the server's `V10` migration which derives `entry_date`
 * from `created_at`. New rows always carry a real `entryDate`; legacy rows get the calendar
 * date of their creation until the next server sync refreshes them.
 */
class TabEntryEntryDateBackfill : AutoMigrationSpec {
    override suspend fun onPostMigrate(connection: SQLiteConnection) {
        connection.executeSQL(
            "UPDATE TabEntryEntity SET entryDate = date(createdAt / 1000, 'unixepoch')",
        )
    }
}
