package de.tabmates.features.tabgroup.data.network.dto

import kotlinx.serialization.Serializable

/**
 * The WebSocket envelope, used in both directions.
 *
 * [requestId] is **required on every outgoing write** — the server rejects an envelope without one
 * as `INVALID_JSON` — and is what the `ACK` (or the error raised while handling the write) echoes
 * back, so the outbox can tell which pending row a verdict belongs to. It stays nullable here
 * because the same type decodes inbound frames, where the server leaves it null on broadcasts and
 * on the errors it raises before the envelope is parsed.
 */
@Serializable
data class WebSocketMessageDto(
    val type: String,
    val payload: String,
    val requestId: String? = null,
)
