package de.tabmates.core.data.di

import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.KSafeMemoryPolicy
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

actual val platformCoreDataModule =
    module {
        single { Darwin.create() } bind HttpClientEngine::class
        single(named("prefs")) {
            KSafe(
                fileName = "prefs",
                memoryPolicy = KSafeMemoryPolicy.PLAIN_TEXT,
            )
        }
        single(named("vault")) {
            KSafe(fileName = "vault")
        }
    }
