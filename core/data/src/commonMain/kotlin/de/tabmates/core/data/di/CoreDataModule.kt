package de.tabmates.core.data.di

import de.tabmates.core.data.networking.HttpClientFactory
import de.tabmates.core.domain.auth.SessionStorage
import de.tabmates.core.domain.logging.TabMatesLogger
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@Configuration
@ComponentScan("de.tabmates.core.data")
class CoreDataModule {
    @Single
    fun provideHttpClient(
        logger: TabMatesLogger,
        sessionStorage: SessionStorage,
        engine: HttpClientEngine,
    ): HttpClient = HttpClientFactory(logger, sessionStorage).create(engine)
}
