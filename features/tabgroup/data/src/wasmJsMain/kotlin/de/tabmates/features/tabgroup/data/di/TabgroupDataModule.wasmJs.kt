package de.tabmates.features.tabgroup.data.di

import de.tabmates.features.tabgroup.database.DatabaseFactory
import de.tabmates.features.tabgroup.sqlitewasmworker.createSQLiteWasmWorker
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

actual val platformTabgroupDataModule =
    module {
        singleOf(::DatabaseFactory)
        single {
            get<DatabaseFactory>()
                .create()
                .setDriver(createSQLiteWasmWorker())
                .build()
        }
    }
