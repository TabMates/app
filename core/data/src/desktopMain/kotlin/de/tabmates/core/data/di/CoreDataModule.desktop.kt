package de.tabmates.core.data.di

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.apache5.Apache5
import org.koin.dsl.bind
import org.koin.dsl.module

actual val platformCoreDataModule =
    module {
        single { Apache5.create() } bind HttpClientEngine::class
    }
