package de.tabmates.features.tabgroup.data.di

import de.tabmates.features.tabgroup.database.DatabaseFactory
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val platformTabgroupDataModule =
    module {
        single { DatabaseFactory(androidContext()) }
        single {
            get<DatabaseFactory>()
                .create()
                .build()
        }
    }
