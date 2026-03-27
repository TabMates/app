package de.tabmates.core.data.di

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import org.koin.dsl.bind
import org.koin.dsl.module

actual val platformCoreDataModule =
    module {
        single { Darwin.create() } bind HttpClientEngine::class
    }
