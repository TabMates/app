package de.tabmates.features.tabgroup.data.di

import de.tabmates.features.tabgroup.database.DatabaseFactory
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

actual val platformTabgroupDataModule =
    module {
        singleOf(::DatabaseFactory)
        single {
            get<DatabaseFactory>()
                .create()
                /* TODO: Implement web target. See:
                 *  https://github.com/danysantiago/room-web-demo/blob/main/sqliteWasmWorker/src/wasmJsMain/kotlin/org/dany/worker/SQLiteWasmWorker.wasmJs.kt
                 * .setDriver(WebWorkerSQLiteDriver(jsWorker()))
                 */
                .build()
        }
    }
