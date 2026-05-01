package de.tabmates.features.tabgroup.data.di

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import de.tabmates.features.tabgroup.database.DatabaseFactory
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

actual val platformTabgroupDataModule =
    module {
        singleOf(::DatabaseFactory)
        single {
            get<DatabaseFactory>()
                .create()
                .setDriver(BundledSQLiteDriver())
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
        }
    }
