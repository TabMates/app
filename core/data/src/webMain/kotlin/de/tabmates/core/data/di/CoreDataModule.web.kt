package de.tabmates.core.data.di

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.js.Js
import org.koin.dsl.bind
import org.koin.dsl.module

actual val platformCoreDataModule =
    module {
        single { Js.create() } bind HttpClientEngine::class
    }
