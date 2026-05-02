package de.tabmates.features.tabgroup.data.di

import android.content.Context
import de.tabmates.features.tabgroup.database.DatabaseFactory
import de.tabmates.features.tabgroup.database.TabMatesDatabase
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@Configuration
actual class PlatformTabgroupDataModule {
    @Single
    fun provideDatabaseFactory(context: Context): DatabaseFactory = DatabaseFactory(context)

    @Single
    fun provideDatabase(factory: DatabaseFactory): TabMatesDatabase =
        factory
            .create()
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
}
