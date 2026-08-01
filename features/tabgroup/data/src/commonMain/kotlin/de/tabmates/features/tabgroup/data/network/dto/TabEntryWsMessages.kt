package de.tabmates.features.tabgroup.data.network.dto

import kotlinx.datetime.LocalDate
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

/**
 * Envelope/payload types match the server contract in
 * `de.tabmates.server.groups.api.dto.ws` and
 * `de.tabmates.server.groups.api.websocket.TabGroupWebSocketHandler`.
 *
 * Envelope `type` values mirror the server enums
 * `IncomingWebSocketMessageType` / `OutgoingWebSocketMessageType` exactly.
 */
object WsMessageType {
    // Outgoing (client -> server)
    const val NEW_TAB_ENTRY = "NEW_TAB_ENTRY"
    const val UPDATED_TAB_ENTRY = "UPDATED_TAB_ENTRY"

    // Incoming (server -> client)

    /**
     * Acknowledges one write, unicast to the session that sent it and correlated by the envelope's
     * `requestId`. Carries the canonical `TabEntryDto`, the same shape [NEW_TAB_ENTRY] does.
     */
    const val ACK = "ACK"
    const val TAB_ENTRY_DELETED = "TAB_ENTRY_DELETED"
    const val GROUP_METADATA_CHANGED = "GROUP_METADATA_CHANGED"
    const val ACTIVITY_EVENT = "ACTIVITY_EVENT"
    const val ERROR = "ERROR"
}

/**
 * Outgoing payload for `NEW_TAB_ENTRY` / `UPDATED_TAB_ENTRY`.
 * Mirrors server `NewTabEntryDto` sealed hierarchy with Jackson discriminator `entryType`.
 *
 * `id` is the client-generated local id (acts as idempotency key for create; required for update).
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("entryType")
sealed class NewTabEntryWsPayload {
    abstract val id: String?
    abstract val groupId: String
    abstract val paidByUserId: String
    abstract val title: String
    abstract val description: String
    abstract val amount: Double
    abstract val currency: String

    /**
     * Rate locked in for this entry (group default currency per 1 unit of [currency]); null =
     * same currency / no rate available. Optional on the server side, so older servers ignore it.
     */
    abstract val exchangeRate: Double?

    @Serializable
    @SerialName("EXPENSE")
    data class Expense(
        override val id: String? = null,
        override val groupId: String,
        override val paidByUserId: String,
        override val title: String,
        override val description: String,
        override val amount: Double,
        override val currency: String,
        override val exchangeRate: Double? = null,
        val entryDate: LocalDate,
        val splits: List<NewTabEntrySplitWsPayload>,
    ) : NewTabEntryWsPayload()

    @Serializable
    @SerialName("INCOME")
    data class Income(
        override val id: String? = null,
        override val groupId: String,
        override val paidByUserId: String,
        override val title: String,
        override val description: String,
        override val amount: Double,
        override val currency: String,
        override val exchangeRate: Double? = null,
        val entryDate: LocalDate,
        val splits: List<NewTabEntrySplitWsPayload>,
    ) : NewTabEntryWsPayload()

    @Serializable
    @SerialName("SETTLEMENT")
    data class Settlement(
        override val id: String? = null,
        override val groupId: String,
        override val paidByUserId: String,
        override val title: String,
        override val description: String,
        override val amount: Double,
        override val currency: String,
        override val exchangeRate: Double? = null,
        val entryDate: LocalDate,
        val receivedByUserId: String,
    ) : NewTabEntryWsPayload()
}

/** Mirrors server `NewTabEntrySplitDto`. */
@Serializable
data class NewTabEntrySplitWsPayload(
    val id: String? = null,
    val participantId: String,
    val split: WsSplitDto,
    val resolvedAmount: Double,
)

/** Mirrors server `DeleteTabEntryDto`. */
@Serializable
data class TabEntryDeletedWsPayload(
    val groupId: String,
    val tabEntryId: String,
)

/** Mirrors server `GroupMetadataChangedDto`. */
@Serializable
data class GroupMetadataChangedWsPayload(
    val groupId: String,
)

/** Mirrors server `ErrorDto`. */
@Serializable
data class WsErrorPayload(
    val code: String,
    val message: String,
    /**
     * Whether re-sending the write could succeed. The server owns this call so the outbox does not
     * have to hardcode [code] lists.
     *
     * Defaults to `true` rather than `false`: a non-retryable verdict rolls the user's optimistic
     * write back, and a frame that somehow arrives without the field must leave that write pending
     * instead of discarding what they typed.
     */
    val retryable: Boolean = true,
)
