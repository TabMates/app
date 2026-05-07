package de.tabmates.features.tabgroup.data.di

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import de.tabmates.features.tabgroup.data.lifecycle.AppLifecycleObserver
import de.tabmates.features.tabgroup.data.network.ConnectionErrorHandler
import de.tabmates.features.tabgroup.data.network.ConnectivityObserver
import de.tabmates.features.tabgroup.database.DatabaseFactory
import de.tabmates.features.tabgroup.database.TabMatesDatabase
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@Configuration
actual class PlatformTabgroupDataModule {
    @Single
    fun provideDatabaseFactory(): DatabaseFactory = DatabaseFactory()

    @Single
    fun provideDatabase(factory: DatabaseFactory): TabMatesDatabase =
        factory
            .create()
            .setDriver(BundledSQLiteDriver())
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Single
    fun provideConnectionErrorHandler(): ConnectionErrorHandler = ConnectionErrorHandler()

    @Single
    fun provideConnectivityObserver(): ConnectivityObserver = ConnectivityObserver()

    @Single
    fun provideAppLifecycleObserver(): AppLifecycleObserver = AppLifecycleObserver()
}
