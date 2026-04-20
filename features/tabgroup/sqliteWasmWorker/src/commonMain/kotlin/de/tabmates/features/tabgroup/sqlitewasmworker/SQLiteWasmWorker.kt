package de.tabmates.features.tabgroup.sqlitewasmworker

import androidx.sqlite.driver.web.WebWorkerSQLiteDriver

expect fun createSQLiteWasmWorker(): WebWorkerSQLiteDriver
