package de.tabmates.features.tabgroup.data.sync

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import de.tabmates.core.domain.sync.LastServerContactStore
import de.tabmates.core.domain.sync.SyncCursorStore
import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.Result
import de.tabmates.features.tabgroup.database.TabMatesDatabase
import de.tabmates.features.tabgroup.database.TabMatesDatabaseConstructor
import de.tabmates.features.tabgroup.domain.models.Group
import de.tabmates.features.tabgroup.domain.models.GroupParticipant
import de.tabmates.features.tabgroup.domain.models.ParticipantType
import de.tabmates.features.tabgroup.domain.models.SplitType
import de.tabmates.features.tabgroup.domain.models.SyncSnapshot
import de.tabmates.features.tabgroup.domain.models.TabEntry
import de.tabmates.features.tabgroup.domain.models.TabEntrySplit
import de.tabmates.features.tabgroup.domain.sync.SyncService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.LocalDate
import kotlin.time.Clock
import kotlin.time.Instant

/** Fresh in-memory Room database, isolated per test. */
fun createInMemoryDatabase(): TabMatesDatabase =
    Room
        .inMemoryDatabaseBuilder<TabMatesDatabase> { TabMatesDatabaseConstructor.initialize() }
        .setDriver(BundledSQLiteDriver())
        .build()

class FakeSyncService(
    var result: Result<SyncSnapshot, DataError.Remote>,
) : SyncService {
    val receivedSince: MutableList<Instant?> = mutableListOf()

    override suspend fun sync(since: Instant?): Result<SyncSnapshot, DataError.Remote> {
        receivedSince += since
        return result
    }
}

class FakeSyncCursorStore(
    private var cursor: Instant? = null,
) : SyncCursorStore {
    override fun get(): Instant? = cursor

    override fun set(cursor: Instant) {
        this.cursor = cursor
    }

    override fun clear() {
        cursor = null
    }
}

class FakeLastServerContactStore : LastServerContactStore {
    private val _lastContactAt = MutableStateFlow<Instant?>(null)
    override val lastContactAt: StateFlow<Instant?> = _lastContactAt.asStateFlow()

    var recordCallCount: Int = 0
        private set

    override fun recordContactNow() {
        recordCallCount++
        _lastContactAt.value = Clock.System.now()
    }

    override fun clear() {
        _lastContactAt.value = null
    }
}

fun instant(epochMillis: Long): Instant = Instant.fromEpochMilliseconds(epochMillis)

fun participant(id: String): GroupParticipant =
    GroupParticipant(userId = id, username = "user-$id", participantType = ParticipantType.REGISTERED)

fun group(
    id: String,
    participantIds: List<String> = listOf("u1"),
): Group =
    Group(
        id = id,
        title = "Group $id",
        description = null,
        defaultCurrencyCode = "EUR",
        participants = participantIds.map { participant(it) }.toSet(),
        creator = participant(participantIds.first()),
        inviteToken = "token-$id",
        lastActivityAt = instant(0),
        lastTabEntry = null,
        createdAt = instant(0),
    )

fun split(
    id: String,
    tabEntryId: String,
    participantId: String,
): TabEntrySplit =
    TabEntrySplit(
        splitId = id,
        tabEntryId = tabEntryId,
        participantId = participantId,
        splitType = SplitType.EQUAL,
        value = 0.0,
        resolvedAmount = 5.0,
    )

fun expense(
    id: String,
    groupId: String,
    amount: Double = 10.0,
    deletedAt: Instant? = null,
    pendingSync: Boolean = false,
    splits: List<TabEntrySplit> = emptyList(),
): TabEntry.Expense =
    TabEntry.Expense(
        tabEntryId = id,
        groupId = groupId,
        title = "Expense $id",
        description = "",
        amount = amount,
        currencyCode = "EUR",
        creatorId = "u1",
        paidByUserId = "u1",
        entryDate = LocalDate(2026, 1, 1),
        createdAt = instant(0),
        lastModifiedAt = instant(0),
        lastModifiedByUserId = "u1",
        version = 0,
        deletedAt = deletedAt,
        deletedByUserId = deletedAt?.let { "u1" },
        splits = splits,
        isPendingSync = pendingSync,
    )

fun snapshot(
    serverTime: Instant,
    groups: List<Group> = emptyList(),
    activeGroupIds: List<String> = groups.map { it.id },
    tabEntries: List<TabEntry> = emptyList(),
    referencedParticipants: List<GroupParticipant> = emptyList(),
): SyncSnapshot =
    SyncSnapshot(
        serverTime = serverTime,
        groups = groups,
        activeGroupIds = activeGroupIds,
        tabEntries = tabEntries,
        referencedParticipants = referencedParticipants,
    )
