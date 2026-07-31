package de.tabmates.core.data.di

import de.tabmates.core.data.BuildKonfig
import de.tabmates.core.data.networking.HttpClientFactory
import de.tabmates.core.domain.auth.SessionInvalidator
import de.tabmates.core.domain.auth.SessionStorage
import de.tabmates.core.domain.logging.TabMatesLogger
import de.tabmates.core.domain.update.UpgradeRequiredNotifier
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

const val APPLICATION_SCOPE = "applicationScope"
const val WS_BASE_URL = "wsBaseUrl"

@Module
@Configuration
@ComponentScan("de.tabmates.core.data")
class CoreDataModule {
    @Single
    fun provideJson(): Json =
        Json {
            ignoreUnknownKeys = true
        }

    @Single
    @Named(APPLICATION_SCOPE)
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Single
    @Named(WS_BASE_URL)
    fun provideWsBaseUrl(): String = BuildKonfig.BASE_URL_WS

    @Single
    fun provideUpgradeRequiredNotifier(): UpgradeRequiredNotifier = UpgradeRequiredNotifier()

    @Single
    fun provideHttpClient(
        logger: TabMatesLogger,
        sessionStorage: SessionStorage,
        json: Json,
        engine: HttpClientEngine,
        upgradeRequiredNotifier: UpgradeRequiredNotifier,
        sessionInvalidator: SessionInvalidator,
    ): HttpClient =
        HttpClientFactory(
            logger,
            sessionStorage,
            json,
            upgradeRequiredNotifier,
            sessionInvalidator,
        ).create(engine)
}
