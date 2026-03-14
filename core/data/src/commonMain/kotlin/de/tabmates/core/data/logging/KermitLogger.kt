package de.tabmates.core.data.logging

import co.touchlab.kermit.Logger
import de.tabmates.core.domain.logging.TabMatesLogger

class KermitLogger : TabMatesLogger {
    override fun debug(
        tag: String,
        message: String,
    ) {
        Logger.d(tag = tag) { message }
    }

    override fun info(
        tag: String,
        message: String,
    ) {
        Logger.i(tag = tag) { message }
    }

    override fun warning(
        tag: String,
        message: String,
    ) {
        Logger.w(tag = tag) { message }
    }

    override fun error(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        Logger.e(tag = tag, throwable = throwable) { message }
    }
}
