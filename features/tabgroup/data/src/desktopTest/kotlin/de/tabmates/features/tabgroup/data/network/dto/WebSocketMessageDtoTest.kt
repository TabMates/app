package de.tabmates.features.tabgroup.data.network.dto

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The envelope's `requestId` is what makes a write answerable, and the server refuses one without
 * it. These pin the wire shape down in both directions.
 */
class WebSocketMessageDtoTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `an outgoing envelope serializes its requestId`() {
        val encoded =
            json.encodeToString(
                WebSocketMessageDto.serializer(),
                WebSocketMessageDto(
                    type = WsMessageType.NEW_TAB_ENTRY,
                    payload = "{}",
                    requestId = "req-1",
                ),
            )

        assertTrue(encoded.contains(""""requestId":"req-1""""), encoded)
    }

    @Test
    fun `a broadcast decodes with a null requestId`() {
        val decoded =
            json.decodeFromString(
                WebSocketMessageDto.serializer(),
                """{"type":"NEW_TAB_ENTRY","payload":"{}","requestId":null}""",
            )

        assertNull(decoded.requestId)
    }

    @Test
    fun `an ack decodes with the id it answers`() {
        val decoded =
            json.decodeFromString(
                WebSocketMessageDto.serializer(),
                """{"type":"ACK","payload":"{}","requestId":"req-1"}""",
            )

        assertEquals(WsMessageType.ACK, decoded.type)
        assertEquals("req-1", decoded.requestId)
    }

    @Test
    fun `an error payload without retryable is treated as retryable`() {
        val decoded =
            json.decodeFromString(
                WsErrorPayload.serializer(),
                """{"code":"INTERNAL_ERROR","message":"boom"}""",
            )

        assertTrue(
            decoded.retryable,
            "a frame missing the flag must leave the write pending, never discard the user's input",
        )
    }
}
