package de.tabmates.features.tabgroup.data.network

import de.tabmates.core.domain.logging.TabMatesLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import kotlin.coroutines.coroutineContext

actual class ConnectivityObserver(
    private val logger: TabMatesLogger,
) {
    actual val isConnected =
        flow {
            while (true) {
                val connected = isConnected()
                logger.info(TAG, "Connectivity state on Desktop: $connected")
                emit(connected)
                delay(5_000L)
            }
        }

    private val connectivityTargets =
        listOf(
            InetSocketAddress("8.8.8.8", 53),
            InetSocketAddress("1.1.1.1", 53),
            InetSocketAddress("208.67.222.222", 53),
        )

    private suspend fun isConnected(): Boolean {
        val hasInterface =
            try {
                NetworkInterface
                    .getNetworkInterfaces()
                    .asSequence()
                    .any { networkInterface ->
                        !networkInterface.isLoopback &&
                            networkInterface.isUp &&
                            networkInterface.inetAddresses.hasMoreElements()
                    }
            } catch (_: Exception) {
                coroutineContext.ensureActive()
                false
            }

        if (!hasInterface) {
            return false
        }

        return withContext(Dispatchers.IO) {
            connectivityTargets.any { target ->
                try {
                    Socket().use {
                        it.soTimeout = 3_000
                        it.connect(target)
                        true
                    }
                } catch (_: Exception) {
                    coroutineContext.ensureActive()
                    false
                }
            }
        }
    }

    private companion object {
        const val TAG = "ConnectivityObserver"
    }
}
