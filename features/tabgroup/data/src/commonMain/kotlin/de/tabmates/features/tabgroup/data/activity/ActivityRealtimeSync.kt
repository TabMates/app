package de.tabmates.features.tabgroup.data.activity

import de.tabmates.core.data.di.APPLICATION_SCOPE
import de.tabmates.core.domain.logging.TabMatesLogger
import de.tabmates.features.tabgroup.data.dto.ActivityEventDto
import de.tabmates.features.tabgroup.data.mappers.toChangeEntities
import de.tabmates.features.tabgroup.data.mappers.toDomain
import de.tabmates.features.tabgroup.data.mappers.toEntity
import de.tabmates.features.tabgroup.data.network.KtorWebSocketConnector
import de.tabmates.features.tabgroup.data.network.dto.WebSocketMessageDto
import de.tabmates.features.tabgroup.data.network.dto.WsMessageType
import de.tabmates.features.tabgroup.database.TabMatesDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

/**
 * Applies `ACTIVITY_EVENT` frames to the local mirror so the feed updates live.
 *
 * The server broadcasts to every session in the group **including the actor**, so this also handles
 * the echo of the user's own writes — which is exactly what retires their pending row. Self-authored
 * events must therefore not be filtered out.
 *
 * It deliberately never touches the [ActivityCursorStore][de.tabmates.core.domain.sync.ActivityCursorStore]:
 * only the paged sync advances the cursor. That, plus upsert-by-id, is what makes a dropped or
 * out-of-order frame heal itself on the next reconnect sync instead of leaving a permanent hole.
 */
@Single(createdAtStart = true)
class ActivityRealtimeSync(
    webSocketConnector: KtorWebSocketConnector,
    private val database: TabMatesDatabase,
    private val json: Json,
    private val logger: TabMatesLogger,
    @Named(APPLICATION_SCOPE) applicationScope: CoroutineScope,
) {
    init {
        webSocketConnector
            .messages
            .filterNotNull()
            .filter { it.type == WsMessageType.ACTIVITY_EVENT }
            .onEach { message -> handle(message) }
            .launchIn(applicationScope)
    }

    private suspend fun handle(message: WebSocketMessageDto) {
        try {
            val event = json.decodeFromString(ActivityEventDto.serializer(), message.payload).toDomain()
            database.activityEventDao.upsertPage(
                events = listOf(event.toEntity()),
                changes = event.toChangeEntities(),
            )
        } catch (e: Throwable) {
            // A frame for a group not yet mirrored fails the group foreign key. Dropping it is safe:
            // the next paged sync refetches everything past the cursor, which never advanced here.
            logger.error(TAG, "Failed to apply ACTIVITY_EVENT frame", e)
        }
    }

    private companion object {
        private const val TAG = "ActivityRealtimeSync"
    }
}
