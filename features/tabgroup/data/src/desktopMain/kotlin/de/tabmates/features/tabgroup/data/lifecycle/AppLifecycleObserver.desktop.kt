package de.tabmates.features.tabgroup.data.lifecycle

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

actual class AppLifecycleObserver {
    actual val isInForeground: Flow<Boolean>
        get() = flowOf(true)
}
