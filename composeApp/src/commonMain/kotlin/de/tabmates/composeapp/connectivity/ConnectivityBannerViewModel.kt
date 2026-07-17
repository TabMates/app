@file:OptIn(FlowPreview::class)

package de.tabmates.composeapp.connectivity

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.tabmates.core.presentation.util.RelativeTimeSpan
import de.tabmates.core.presentation.util.relativeTimeSpan
import de.tabmates.features.tabgroup.domain.sync.ConnectionStatusRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.core.annotation.KoinViewModel
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Stable // holds an interface-typed field (RelativeTimeSpan)
data class ConnectivityBannerState(
    val isVisible: Boolean = false,
    val lastSyncedSpan: RelativeTimeSpan? = null,
)

@KoinViewModel
class ConnectivityBannerViewModel(
    private val connectionStatusRepository: ConnectionStatusRepository,
) : ViewModel() {
    // Re-evaluates the relative "last synced X ago" bucket while the banner stays visible.
    private val minuteTicker =
        flow {
            while (true) {
                emit(Unit)
                delay(TICK_INTERVAL)
            }
        }

    val state: StateFlow<ConnectivityBannerState> =
        combine(
            connectionStatusRepository.isConnected
                .map { !it }
                .distinctUntilChanged()
                // Brief drops (foreground/reconnect cycles) shouldn't flash the banner; going
                // back online hides it immediately.
                .debounce { isOffline -> if (isOffline) GRACE_PERIOD else Duration.ZERO },
            connectionStatusRepository.lastServerContactAt,
            minuteTicker,
        ) { isOffline, lastContactAt, _ ->
            ConnectivityBannerState(
                isVisible = isOffline,
                lastSyncedSpan = lastContactAt?.let { relativeTimeSpan(from = it, now = Clock.System.now()) },
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = ConnectivityBannerState(),
        )

    internal companion object {
        internal val GRACE_PERIOD = 5.seconds
        private val TICK_INTERVAL = 60.seconds
    }
}
