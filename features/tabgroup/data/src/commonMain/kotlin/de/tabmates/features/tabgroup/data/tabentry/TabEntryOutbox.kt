package de.tabmates.features.tabgroup.data.tabentry

import de.tabmates.core.data.di.APPLICATION_SCOPE
import de.tabmates.core.domain.logging.TabMatesLogger
import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.Result
import de.tabmates.features.tabgroup.data.mappers.toWsSplit
import de.tabmates.features.tabgroup.data.network.ConnectionState
import de.tabmates.features.tabgroup.data.network.KtorWebSocketConnector
import de.tabmates.features.tabgroup.data.network.dto.NewTabEntrySplitWsPayload
import de.tabmates.features.tabgroup.data.network.dto.NewTabEntryWsPayload
import de.tabmates.features.tabgroup.data.network.dto.WebSocketMessageDto
import de.tabmates.features.tabgroup.data.network.dto.WsMessageType
import de.tabmates.features.tabgroup.database.TabMatesDatabase
import de.tabmates.features.tabgroup.database.entities.PendingOutboxEntity
import de.tabmates.features.tabgroup.domain.tabentry.NewTabEntrySplit
import de.tabmates.features.tabgroup.domain.tabentry.SplitResolver
import de.tabmates.features.tabgroup.domain.tabentry.TabEntryService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import kotlin.time.Clock

/**
 * Durable outbox for tab-entry writes. The repository enqueues every write here; the outbox
 * tries to dispatch it when the WS is CONNECTED. If dispatch fails transiently (offline, server
 * 5xx) the row is left untouched and re-drained on the next CONNECTED tick. Only PERMANENT
 * failures (server rejection, bad payload, auth) count toward [MAX_ATTEMPTS]; once reached, the
 * row is parked and skipped on subsequent drains.
 */
@Single(createdAtStart = true)
class TabEntryOutbox(
    private val database: TabMatesDatabase,
    private val webSocketConnector: KtorWebSocketConnector,
    private val service: TabEntryService,
    private val json: Json,
    private val logger: TabMatesLogger,
    @Named(APPLICATION_SCOPE) private val applicationScope: CoroutineScope,
) {
    private val mutex = Mutex()

    /**
     * Per-session retry counter. Resets every app launch so the user can recover stuck rows
     * (e.g. a transient server bug that masqueraded as permanent) simply by reopening the app.
     * Always accessed under [mutex]; the DB [PendingOutboxEntity.attemptCount] is kept for
     * forensics only and never consulted for parking decisions.
     */
    private val sessionAttempts = mutableMapOf<String, Int>()

    init {
        webSocketConnector
            .connectionState
            .filter { it == ConnectionState.CONNECTED }
            .onEach { drain() }
            .launchIn(applicationScope)
    }

    suspend fun enqueueCreateExpense(
        clientRequestId: String,
        groupId: String,
        title: String,
        description: String,
        amount: Double,
        currencyCode: String,
        exchangeRate: Double?,
        paidByUserId: String,
        entryDate: LocalDate,
        splits: List<NewTabEntrySplit>,
        expectedVersion: Int?,
    ) {
        logger.debug(TAG, "Outbox enqueue create id=$clientRequestId")
        val payload =
            NewTabEntryWsPayload.Expense(
                id = clientRequestId,
                groupId = groupId,
                paidByUserId = paidByUserId,
                title = title,
                description = description,
                amount = amount,
                currency = currencyCode,
                exchangeRate = exchangeRate,
                entryDate = entryDate,
                splits = buildSplitPayloads(splits, amount),
            )
        val envelope =
            WebSocketMessageDto(
                type = WsMessageType.NEW_TAB_ENTRY,
                payload = json.encodeToString(NewTabEntryWsPayload.serializer(), payload),
            )
        database.pendingOutboxDao.upsert(
            PendingOutboxEntity(
                id = clientRequestId,
                type = OUTBOX_TYPE_NEW_TAB_ENTRY,
                payload = json.encodeToString(WebSocketMessageDto.serializer(), envelope),
                createdAt = Clock.System.now().toEpochMilliseconds(),
                expectedVersion = expectedVersion,
            ),
        )
        applicationScope.launch { drain() }
    }

    suspend fun enqueueCreateSettlement(
        clientRequestId: String,
        groupId: String,
        title: String,
        description: String,
        amount: Double,
        currencyCode: String,
        exchangeRate: Double?,
        paidByUserId: String,
        receivedByUserId: String,
        entryDate: LocalDate,
        expectedVersion: Int?,
    ) {
        val payload =
            NewTabEntryWsPayload.Settlement(
                id = clientRequestId,
                groupId = groupId,
                paidByUserId = paidByUserId,
                title = title,
                description = description,
                amount = amount,
                currency = currencyCode,
                exchangeRate = exchangeRate,
                entryDate = entryDate,
                receivedByUserId = receivedByUserId,
            )
        val envelope =
            WebSocketMessageDto(
                type = WsMessageType.NEW_TAB_ENTRY,
                payload = json.encodeToString(NewTabEntryWsPayload.serializer(), payload),
            )
        database.pendingOutboxDao.upsert(
            PendingOutboxEntity(
                id = clientRequestId,
                type = OUTBOX_TYPE_NEW_TAB_ENTRY,
                payload = json.encodeToString(WebSocketMessageDto.serializer(), envelope),
                createdAt = Clock.System.now().toEpochMilliseconds(),
                expectedVersion = expectedVersion,
            ),
        )
        applicationScope.launch { drain() }
    }

    suspend fun enqueueUpdateExpense(
        tabEntryId: String,
        groupId: String,
        title: String,
        description: String,
        amount: Double,
        currencyCode: String,
        exchangeRate: Double?,
        paidByUserId: String,
        entryDate: LocalDate,
        splits: List<NewTabEntrySplit>,
        expectedVersion: Int?,
    ) {
        val payload =
            NewTabEntryWsPayload.Expense(
                id = tabEntryId,
                groupId = groupId,
                paidByUserId = paidByUserId,
                title = title,
                description = description,
                amount = amount,
                currency = currencyCode,
                exchangeRate = exchangeRate,
                entryDate = entryDate,
                splits = buildSplitPayloads(splits, amount),
            )
        val envelope =
            WebSocketMessageDto(
                type = WsMessageType.UPDATED_TAB_ENTRY,
                payload = json.encodeToString(NewTabEntryWsPayload.serializer(), payload),
            )
        database.pendingOutboxDao.upsert(
            PendingOutboxEntity(
                id = "$UPDATE_ID_PREFIX$tabEntryId",
                type = OUTBOX_TYPE_TAB_ENTRY_UPDATE,
                payload = json.encodeToString(WebSocketMessageDto.serializer(), envelope),
                createdAt = Clock.System.now().toEpochMilliseconds(),
                expectedVersion = expectedVersion,
            ),
        )
        applicationScope.launch { drain() }
    }

    suspend fun enqueueCreateIncome(
        clientRequestId: String,
        groupId: String,
        title: String,
        description: String,
        amount: Double,
        currencyCode: String,
        exchangeRate: Double?,
        paidByUserId: String,
        entryDate: LocalDate,
        splits: List<NewTabEntrySplit>,
        expectedVersion: Int?,
    ) {
        logger.debug(TAG, "Outbox enqueue create income id=$clientRequestId")
        val payload =
            NewTabEntryWsPayload.Income(
                id = clientRequestId,
                groupId = groupId,
                paidByUserId = paidByUserId,
                title = title,
                description = description,
                amount = amount,
                currency = currencyCode,
                exchangeRate = exchangeRate,
                entryDate = entryDate,
                splits = buildSplitPayloads(splits, amount),
            )
        val envelope =
            WebSocketMessageDto(
                type = WsMessageType.NEW_TAB_ENTRY,
                payload = json.encodeToString(NewTabEntryWsPayload.serializer(), payload),
            )
        database.pendingOutboxDao.upsert(
            PendingOutboxEntity(
                id = clientRequestId,
                type = OUTBOX_TYPE_NEW_TAB_ENTRY,
                payload = json.encodeToString(WebSocketMessageDto.serializer(), envelope),
                createdAt = Clock.System.now().toEpochMilliseconds(),
                expectedVersion = expectedVersion,
            ),
        )
        applicationScope.launch { drain() }
    }

    suspend fun enqueueUpdateIncome(
        tabEntryId: String,
        groupId: String,
        title: String,
        description: String,
        amount: Double,
        currencyCode: String,
        exchangeRate: Double?,
        paidByUserId: String,
        entryDate: LocalDate,
        splits: List<NewTabEntrySplit>,
        expectedVersion: Int?,
    ) {
        val payload =
            NewTabEntryWsPayload.Income(
                id = tabEntryId,
                groupId = groupId,
                paidByUserId = paidByUserId,
                title = title,
                description = description,
                amount = amount,
                currency = currencyCode,
                exchangeRate = exchangeRate,
                entryDate = entryDate,
                splits = buildSplitPayloads(splits, amount),
            )
        val envelope =
            WebSocketMessageDto(
                type = WsMessageType.UPDATED_TAB_ENTRY,
                payload = json.encodeToString(NewTabEntryWsPayload.serializer(), payload),
            )
        database.pendingOutboxDao.upsert(
            PendingOutboxEntity(
                id = "$UPDATE_ID_PREFIX$tabEntryId",
                type = OUTBOX_TYPE_TAB_ENTRY_UPDATE,
                payload = json.encodeToString(WebSocketMessageDto.serializer(), envelope),
                createdAt = Clock.System.now().toEpochMilliseconds(),
                expectedVersion = expectedVersion,
            ),
        )
        applicationScope.launch { drain() }
    }

    suspend fun enqueueUpdateSettlement(
        tabEntryId: String,
        groupId: String,
        title: String,
        description: String,
        amount: Double,
        currencyCode: String,
        exchangeRate: Double?,
        paidByUserId: String,
        receivedByUserId: String,
        entryDate: LocalDate,
        expectedVersion: Int?,
    ) {
        val payload =
            NewTabEntryWsPayload.Settlement(
                id = tabEntryId,
                groupId = groupId,
                paidByUserId = paidByUserId,
                title = title,
                description = description,
                amount = amount,
                currency = currencyCode,
                exchangeRate = exchangeRate,
                entryDate = entryDate,
                receivedByUserId = receivedByUserId,
            )
        val envelope =
            WebSocketMessageDto(
                type = WsMessageType.UPDATED_TAB_ENTRY,
                payload = json.encodeToString(NewTabEntryWsPayload.serializer(), payload),
            )
        database.pendingOutboxDao.upsert(
            PendingOutboxEntity(
                id = "$UPDATE_ID_PREFIX$tabEntryId",
                type = OUTBOX_TYPE_TAB_ENTRY_UPDATE,
                payload = json.encodeToString(WebSocketMessageDto.serializer(), envelope),
                createdAt = Clock.System.now().toEpochMilliseconds(),
                expectedVersion = expectedVersion,
            ),
        )
        applicationScope.launch { drain() }
    }

    /**
     * Cancels a not-yet-dispatched create for [tabEntryId] (and any queued update for it).
     * Returns `true` when a pending **create** row was present, meaning the entry never reached
     * the server, so the caller can skip enqueuing a remote delete that would only 404 — and,
     * worse, could race the create's own echo back onto the server. Runs under [mutex] so it is
     * atomic against [drain]: if the create has already been dispatched its row is gone and this
     * returns `false`, letting the caller fall back to a normal remote delete.
     */
    suspend fun cancelPendingCreate(tabEntryId: String): Boolean =
        mutex.withLock {
            val pending = database.pendingOutboxDao.getAll()
            val createRow =
                pending.firstOrNull { it.id == tabEntryId && it.type == OUTBOX_TYPE_NEW_TAB_ENTRY }
                    ?: return@withLock false
            database.pendingOutboxDao.deleteById(createRow.id)
            sessionAttempts.remove(createRow.id)
            val updateId = "$UPDATE_ID_PREFIX$tabEntryId"
            database.pendingOutboxDao.deleteById(updateId)
            sessionAttempts.remove(updateId)
            true
        }

    /**
     * Queues a remote delete, carrying a snapshot of what is being deleted.
     *
     * The caller must read the snapshot *before* wiping the local row: nothing else survives the
     * delete, so without it the activity feed's pending row has no title or amount to show.
     */
    suspend fun enqueueDeleteTabEntry(
        tabEntryId: String,
        groupId: String? = null,
        title: String? = null,
        amount: Double? = null,
        currencyCode: String? = null,
        entryType: String? = null,
    ) {
        val payload =
            PendingDeletePayload(
                tabEntryId = tabEntryId,
                groupId = groupId,
                title = title,
                amount = amount,
                currencyCode = currencyCode,
                entryType = entryType,
            )
        database.pendingOutboxDao.upsert(
            PendingOutboxEntity(
                id = "$DELETE_ID_PREFIX$tabEntryId",
                type = OUTBOX_TYPE_TAB_ENTRY_DELETE,
                payload = json.encodeToString(PendingDeletePayload.serializer(), payload),
                createdAt = Clock.System.now().toEpochMilliseconds(),
            ),
        )
        applicationScope.launch { drain() }
    }

    private fun buildSplitPayloads(
        splits: List<NewTabEntrySplit>,
        amount: Double,
    ): List<NewTabEntrySplitWsPayload> {
        if (splits.isEmpty()) return emptyList()
        val resolved = SplitResolver.resolveAmounts(splits, amount)
        return splits.mapIndexed { index, split ->
            NewTabEntrySplitWsPayload(
                id = null,
                participantId = split.participantId,
                split = toWsSplit(split.splitType, split.value),
                resolvedAmount = resolved[index],
            )
        }
    }

    private suspend fun drain() {
        // No connection → nothing to attempt. Drain will re-run on next CONNECTED edge.
        if (webSocketConnector.connectionState.value != ConnectionState.CONNECTED) {
            logger.debug(
                TAG,
                "Outbox drain skipped: not connected (state=${webSocketConnector.connectionState.value})",
            )
            return
        }

        mutex.withLock {
            val pending = database.pendingOutboxDao.getAll()
            for (item in pending) {
                val sessionCount = sessionAttempts[item.id] ?: 0
                if (sessionCount >= MAX_ATTEMPTS) {
                    // Parked for this session — will retry on next app launch.
                    continue
                }
                when (val result = dispatch(item)) {
                    is DispatchResult.Success -> {
                        logger.debug(
                            TAG,
                            "Outbox dispatched id=${item.id} (sent, awaiting echo); deleting row",
                        )
                        database.pendingOutboxDao.deleteById(item.id)
                        sessionAttempts.remove(item.id)
                    }

                    is DispatchResult.Transient -> {
                        // Don't burn attempt budget on retriable failures (offline, 5xx, timeouts).
                        logger.debug(
                            TAG,
                            "Transient dispatch failure id=${item.id} reason=${result.reason}; leaving for retry",
                        )
                    }

                    is DispatchResult.Permanent -> {
                        val nextCount = sessionCount + 1
                        sessionAttempts[item.id] = nextCount
                        val tag =
                            if (nextCount >= MAX_ATTEMPTS) {
                                logger.error(
                                    TAG,
                                    "Outbox entry id=${item.id} exhausted retries ($nextCount/$MAX_ATTEMPTS) this session; parking until restart. last=${result.reason}",
                                )
                                "permanent_${result.reason}_parked_session"
                            } else {
                                "permanent_${result.reason}_attempt_$nextCount"
                            }
                        recordAttempt(item, lastError = tag)
                    }
                }
            }
        }
    }

    private suspend fun dispatch(item: PendingOutboxEntity): DispatchResult =
        try {
            when (item.type) {
                OUTBOX_TYPE_NEW_TAB_ENTRY,
                OUTBOX_TYPE_TAB_ENTRY_UPDATE,
                -> {
                    dispatchNewTabEntry(item.payload)
                }

                OUTBOX_TYPE_TAB_ENTRY_DELETE -> {
                    dispatchDelete(json.decodePendingDeletePayload(item.payload).tabEntryId)
                }

                else -> {
                    logger.warning(TAG, "Dropping unknown outbox entry type=${item.type} id=${item.id}")
                    DispatchResult.Success
                }
            }
        } catch (e: Throwable) {
            logger.error(TAG, "Outbox dispatch crashed for id=${item.id}", e)
            DispatchResult.Permanent("dispatch_crash")
        }

    private suspend fun dispatchNewTabEntry(envelopeJson: String): DispatchResult =
        when (val result = webSocketConnector.sendMessage(envelopeJson)) {
            is Result.Success -> {
                DispatchResult.Success
            }

            is Result.Failure -> {
                // Lost the socket between drain gate and send — retry on next CONNECTED tick.
                when (result.error) {
                    DataError.Connection.NOT_CONNECTED -> DispatchResult.Transient("not_connected")
                    DataError.Connection.MESSAGE_SEND_FAILED -> DispatchResult.Transient("send_failed")
                }
            }
        }

    private suspend fun dispatchDelete(tabEntryId: String): DispatchResult =
        when (val result = service.deleteTabEntry(tabEntryId)) {
            is Result.Success -> {
                DispatchResult.Success
            }

            is Result.Failure -> {
                when (result.error) {
                    DataError.Remote.NO_INTERNET,
                    DataError.Remote.REQUEST_TIMEOUT,
                    DataError.Remote.SERVER_ERROR,
                    DataError.Remote.SERVICE_UNAVAILABLE,
                    DataError.Remote.TOO_MANY_REQUESTS,
                    DataError.Remote.UNKNOWN,
                    -> DispatchResult.Transient(result.error.name.lowercase())

                    // 404 on delete: entry is already gone on the server, treat as success.
                    DataError.Remote.NOT_FOUND -> DispatchResult.Success

                    DataError.Remote.BAD_REQUEST,
                    DataError.Remote.UNAUTHORIZED,
                    DataError.Remote.FORBIDDEN,
                    DataError.Remote.CONFLICT,
                    DataError.Remote.PAYLOAD_TOO_LARGE,
                    DataError.Remote.SERIALIZATION,
                    // Turnstile only gates the auth endpoints, never this delete; classify as
                    // permanent for exhaustiveness (it would never clear on retry anyway).
                    DataError.Remote.TURNSTILE_FAILED,
                    -> DispatchResult.Permanent(result.error.name.lowercase())
                }
            }
        }

    private suspend fun recordAttempt(
        item: PendingOutboxEntity,
        lastError: String,
    ) {
        database.pendingOutboxDao.upsert(
            item.copy(
                attemptCount = item.attemptCount + 1,
                lastAttemptAt = Clock.System.now().toEpochMilliseconds(),
                lastError = lastError,
            ),
        )
    }

    private sealed class DispatchResult {
        data object Success : DispatchResult()

        data class Transient(val reason: String) : DispatchResult()

        data class Permanent(val reason: String) : DispatchResult()
    }

    /**
     * The id scheme and type tags are read back by the activity feed's merge layer, which recovers a
     * write's `tabEntryId` by stripping these prefixes, so they are shared rather than private.
     */
    internal companion object {
        private const val TAG = "TabEntryOutbox"
        private const val MAX_ATTEMPTS = 10
        const val DELETE_ID_PREFIX = "delete:"
        const val UPDATE_ID_PREFIX = "update:"
        const val OUTBOX_TYPE_NEW_TAB_ENTRY = "new_tab_entry"
        const val OUTBOX_TYPE_TAB_ENTRY_UPDATE = "tab_entry.update"
        const val OUTBOX_TYPE_TAB_ENTRY_DELETE = "tab_entry.delete"
    }
}
