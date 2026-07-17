package de.tabmates.features.tabgroup.data.sync

import de.tabmates.features.tabgroup.data.network.ConnectionState
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LastServerContactTrackerTest {
    @Test
    fun leavingConnectedStamps() {
        assertTrue(leftConnectedState(ConnectionState.CONNECTED, ConnectionState.DISCONNECTED))
        assertTrue(leftConnectedState(ConnectionState.CONNECTED, ConnectionState.CONNECTING))
        assertTrue(leftConnectedState(ConnectionState.CONNECTED, ConnectionState.ERROR_NETWORK))
        assertTrue(leftConnectedState(ConnectionState.CONNECTED, ConnectionState.ERROR_UNKNOWN))
    }

    @Test
    fun stayingConnectedDoesNotStamp() {
        assertFalse(leftConnectedState(ConnectionState.CONNECTED, ConnectionState.CONNECTED))
    }

    @Test
    fun transitionsNotStartingFromConnectedNeverStamp() {
        val nonConnected = ConnectionState.entries.filter { it != ConnectionState.CONNECTED }
        for (previous in nonConnected) {
            for (current in ConnectionState.entries) {
                assertFalse(
                    leftConnectedState(previous, current),
                    "previous=$previous current=$current",
                )
            }
        }
    }
}
