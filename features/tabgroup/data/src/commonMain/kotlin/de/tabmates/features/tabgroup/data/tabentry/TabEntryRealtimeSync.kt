package de.tabmates.features.tabgroup.data.tabentry

import de.tabmates.core.data.di.APPLICATION_SCOPE
import de.tabmates.core.domain.logging.TabMatesLogger
import de.tabmates.core.domain.util.onFailure
import de.tabmates.features.tabgroup.data.dto.TabEntryDto
import de.tabmates.features.tabgroup.data.mappers.referencedParticipants
import de.tabmates.features.tabgroup.data.mappers.toDomain
import de.tabmates.features.tabgroup.data.mappers.toEntity
import de.tabmates.features.tabgroup.data.network.WebSocketChannel
import de.tabmates.features.tabgroup.data.network.dto.GroupMetadataChangedWsPayload
import de.tabmates.features.tabgroup.data.network.dto.TabEntryDeletedWsPayload
import de.tabmates.features.tabgroup.data.network.dto.WebSocketMessageDto
import de.tabmates.features.tabgroup.data.network.dto.WsErrorPayload
import de.tabmates.features.tabgroup.data.network.dto.WsMessageType
import de.tabmates.features.tabgroup.database.TabMatesDatabase
import de.tabmates.features.tabgroup.domain.group.GroupRepository
import de.tabmates.features.tabgroup.domain.models.TabEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

/**
 * Consumes the WebSocket message stream and applies tab-entry events to the local DB.
 * Server fans out `NEW_TAB_ENTRY` / `UPDATED_TAB_ENTRY` / `TAB_ENTRY_DELETED` to every connected
 * group member (including the original sender), so this single collector handles both echoes of
 * the user's own writes and remote writes from other group members with the same code paths.
 */
@Single(createdAtStart = true)
class TabEntryRealtimeSync(
    webSocketConnector: WebSocketChannel,
    private val database: TabMatesDatabase,
    private val groupRepository: GroupRepository,
    private val json: Json,
    private val logger: TabMatesLogger,
    @Named(APPLICATION_SCOPE) private val applicationScope: CoroutineScope,
) {
    init {
        webSocketConnector
            .messages
            .onEach { message -> handle(message) }
            .launchIn(applicationScope)
    }

    private suspend fun handle(message: WebSocketMessageDto) {
        try {
            when (message.type) {
                WsMessageType.NEW_TAB_ENTRY,
                WsMessageType.UPDATED_TAB_ENTRY,
                // An ack carries the same canonical TabEntryDto, and on a *replayed* one it is the
                // only thing that does: the server does not repeat the broadcast for a write it
                // already applied, so a client retrying after a lost ack is brought up to date by
                // this frame alone. The upsert is idempotent, so applying both on the normal path
                // costs nothing. Clearing the outbox row is TabEntryOutbox's half of the same frame.
                WsMessageType.ACK,
                -> handleUpsert(message.payload)

                WsMessageType.TAB_ENTRY_DELETED -> handleDeleted(message.payload)

                WsMessageType.GROUP_METADATA_CHANGED -> handleGroupMetadataChanged(message.payload)

                // Owned by ActivityRealtimeSync; named here only to keep it out of the unknown-type log.
                WsMessageType.ACTIVITY_EVENT -> Unit

                WsMessageType.ERROR -> handleError(message.payload)

                else -> logger.warning(TAG, "Unknown WS message type=${message.type}")
            }
        } catch (e: Throwable) {
            logger.error(TAG, "Failed to handle WS message of type ${message.type}", e)
        }
    }

    private suspend fun handleUpsert(payload: String) {
        val dto = json.decodeFromString(TabEntryDto.serializer(), payload)
        val entry = dto.toDomain()
        logger.debug(TAG, "WS echo received id=${entry.tabEntryId}")
        // A soft-deleted entry is gone as far as this client is concerned, and local queries do not
        // filter on deletedAt — upserting one would put it back on screen. Reached via a replayed
        // ACK: the server answers a retry of a write whose entry has since been deleted with the
        // canonical DTO, deletedAt and all. This mirrors what the paged sync does with its
        // deletedIds, so both paths agree.
        if (dto.deletedAt != null) {
            logger.debug(TAG, "WS frame carries a deleted entry ${entry.tabEntryId}; removing it")
            database.tabEntryDao.deleteTabEntryAndSplits(
                tabEntryId = entry.tabEntryId,
                splitDao = database.tabEntrySplitDao,
            )
            return
        }
        // The entry may reference participants missing locally (ex-members whose old entry got
        // edited, or members whose join event hasn't been applied yet) — persist them first so
        // the split table's participant FK holds.
        database.groupParticipantDao.upsertParticipants(
            dto.referencedParticipants().distinctBy { it.userId }.map { it.toDomain().toEntity() },
        )
        // Optimistic local splits have client-generated ids; server returns its own ids on echo,
        // so the entry + canonical split set must be applied atomically to avoid orphaned rows.
        val splits =
            when (entry) {
                is TabEntry.Expense -> entry.splits.map { it.toEntity() }
                is TabEntry.Income -> entry.splits.map { it.toEntity() }
                is TabEntry.Settlement -> emptyList()
            }
        database.tabEntryDao.replaceTabEntryWithSplits(
            entry = entry.toEntity(),
            splits = splits,
            splitDao = database.tabEntrySplitDao,
        )
    }

    private suspend fun handleDeleted(payload: String) {
        val event = json.decodeFromString(TabEntryDeletedWsPayload.serializer(), payload)
        database.tabEntryDao.deleteTabEntryAndSplits(
            tabEntryId = event.tabEntryId,
            splitDao = database.tabEntrySplitDao,
        )
    }

    private suspend fun handleGroupMetadataChanged(payload: String) {
        val event = json.decodeFromString(GroupMetadataChangedWsPayload.serializer(), payload)
        // Server broadcasts this on participant join/leave and placeholder claims. Re-fetch the
        // group so the local cache (and every Flow observer) reflects the new participant list
        // without waiting for the next cold-start sync.
        groupRepository
            .fetchGroupById(event.groupId)
            .onFailure { error ->
                logger.error(TAG, "Failed to refresh group ${event.groupId} after metadata change: $error")
            }
    }

    // Diagnostic only — TabEntryOutbox owns what an error does to the write that caused it.
    private fun handleError(payload: String) {
        val error = json.decodeFromString(WsErrorPayload.serializer(), payload)
        logger.warning(
            TAG,
            "Server WS error code=${error.code} retryable=${error.retryable} message=${error.message}",
        )
    }

    private companion object {
        private const val TAG = "TabEntryRealtimeSync"
    }
}
