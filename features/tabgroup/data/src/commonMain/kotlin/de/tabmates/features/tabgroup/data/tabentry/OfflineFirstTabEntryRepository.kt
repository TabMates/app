package de.tabmates.features.tabgroup.data.tabentry

import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.EmptyResult
import de.tabmates.core.domain.util.Result
import de.tabmates.features.tabgroup.data.mappers.toDomain
import de.tabmates.features.tabgroup.data.mappers.toEntity
import de.tabmates.features.tabgroup.database.TabMatesDatabase
import de.tabmates.features.tabgroup.domain.models.TabEntry
import de.tabmates.features.tabgroup.domain.models.TabEntrySplit
import de.tabmates.features.tabgroup.domain.tabentry.NewExpenseSplit
import de.tabmates.features.tabgroup.domain.tabentry.SplitResolver
import de.tabmates.features.tabgroup.domain.tabentry.TabEntryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import org.koin.core.annotation.Single
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Single(binds = [TabEntryRepository::class])
class OfflineFirstTabEntryRepository(
    private val database: TabMatesDatabase,
    private val outbox: TabEntryOutbox,
) : TabEntryRepository {
    override fun getTabEntriesForGroup(groupId: String): Flow<List<TabEntry>> =
        database.tabEntryDao
            .getTabEntriesByGroupId(groupId)
            .map { rows -> rows.map { it.toDomain() } }

    override fun getTabEntryById(tabEntryId: String): Flow<TabEntry?> =
        database.tabEntryDao
            .observeTabEntryById(tabEntryId)
            .map { it?.toDomain() }

    override suspend fun createExpense(
        groupId: String,
        title: String,
        description: String,
        amount: Double,
        currencyCode: String,
        paidByUserId: String,
        entryDate: LocalDate,
        splits: List<NewExpenseSplit>,
    ): Result<TabEntry.Expense, DataError.Remote> {
        val localId = generateLocalId()
        val now = Clock.System.now()
        val resolvedSplits = resolveSplits(localId, splits, amount)
        val expense =
            TabEntry.Expense(
                tabEntryId = localId,
                groupId = groupId,
                title = title,
                description = description,
                amount = amount,
                currencyCode = currencyCode,
                creatorId = paidByUserId,
                paidByUserId = paidByUserId,
                entryDate = entryDate,
                createdAt = now,
                lastModifiedAt = now,
                lastModifiedByUserId = paidByUserId,
                version = 0,
                deletedAt = null,
                deletedByUserId = null,
                splits = resolvedSplits,
                isPendingSync = true,
            )

        insertLocal(expense)
        outbox.enqueueCreateExpense(
            clientRequestId = localId,
            groupId = groupId,
            title = title,
            description = description,
            amount = amount,
            currencyCode = currencyCode,
            paidByUserId = paidByUserId,
            entryDate = entryDate,
            splits = splits,
        )
        return Result.Success(expense)
    }

    override suspend fun updateExpense(
        tabEntryId: String,
        groupId: String,
        title: String,
        description: String,
        amount: Double,
        currencyCode: String,
        paidByUserId: String,
        entryDate: LocalDate,
        splits: List<NewExpenseSplit>,
    ): Result<TabEntry.Expense, DataError.Remote> {
        // Preserve server-owned fields from the existing row; the server reconciles the rest on echo.
        val existing = database.tabEntryDao.getTabEntryById(tabEntryId)?.toDomain() as? TabEntry.Expense
        val now = Clock.System.now()
        val resolvedSplits = resolveSplits(tabEntryId, splits, amount)
        val expense =
            TabEntry.Expense(
                tabEntryId = tabEntryId,
                groupId = groupId,
                title = title,
                description = description,
                amount = amount,
                currencyCode = currencyCode,
                creatorId = existing?.creatorId ?: paidByUserId,
                paidByUserId = paidByUserId,
                entryDate = entryDate,
                createdAt = existing?.createdAt ?: now,
                lastModifiedAt = now,
                lastModifiedByUserId = paidByUserId,
                version = existing?.version ?: 0,
                deletedAt = null,
                deletedByUserId = null,
                splits = resolvedSplits,
                isPendingSync = true,
            )

        database.tabEntryDao.replaceTabEntryWithSplits(
            entry = expense.toEntity(pendingSync = true),
            splits = resolvedSplits.map { it.toEntity() },
            splitDao = database.tabEntrySplitDao,
        )
        outbox.enqueueUpdateExpense(
            tabEntryId = tabEntryId,
            groupId = groupId,
            title = title,
            description = description,
            amount = amount,
            currencyCode = currencyCode,
            paidByUserId = paidByUserId,
            entryDate = entryDate,
            splits = splits,
        )
        return Result.Success(expense)
    }

    override suspend fun createSettlement(
        groupId: String,
        title: String,
        description: String,
        amount: Double,
        currencyCode: String,
        paidByUserId: String,
        receivedByUserId: String,
        entryDate: LocalDate,
    ): Result<TabEntry.Settlement, DataError.Remote> {
        val localId = generateLocalId()
        val now = Clock.System.now()
        val settlement =
            TabEntry.Settlement(
                tabEntryId = localId,
                groupId = groupId,
                title = title,
                description = description,
                amount = amount,
                currencyCode = currencyCode,
                creatorId = paidByUserId,
                paidByUserId = paidByUserId,
                entryDate = entryDate,
                createdAt = now,
                lastModifiedAt = now,
                lastModifiedByUserId = paidByUserId,
                version = 0,
                deletedAt = null,
                deletedByUserId = null,
                receivedByUserId = receivedByUserId,
                isPendingSync = true,
            )

        database.tabEntryDao.upsertTabEntry(settlement.toEntity(pendingSync = true))
        outbox.enqueueCreateSettlement(
            clientRequestId = localId,
            groupId = groupId,
            title = title,
            description = description,
            amount = amount,
            currencyCode = currencyCode,
            paidByUserId = paidByUserId,
            receivedByUserId = receivedByUserId,
            entryDate = entryDate,
        )
        return Result.Success(settlement)
    }

    override suspend fun deleteTabEntry(tabEntryId: String): EmptyResult<DataError.Remote> {
        // If the entry's create never made it to the server, cancel it outright. Enqueuing a
        // remote delete here would only 404 — and could race the still-pending create's echo,
        // resurrecting the entry on the server after it's gone locally.
        if (!outbox.cancelPendingCreate(tabEntryId)) {
            // Persist intent first so a crash between enqueue and local delete still drives the
            // remote delete on next drain. Outbox upsert by id is idempotent.
            outbox.enqueueDeleteTabEntry(tabEntryId)
        }
        database.tabEntryDao.deleteTabEntryAndSplits(
            tabEntryId = tabEntryId,
            splitDao = database.tabEntrySplitDao,
        )
        return Result.Success(Unit)
    }

    private suspend fun insertLocal(expense: TabEntry.Expense) {
        database.tabEntryDao.upsertTabEntry(expense.toEntity(pendingSync = true))
        if (expense.splits.isNotEmpty()) {
            database.tabEntrySplitDao.upsertSplits(expense.splits.map { it.toEntity() })
        }
    }

    private fun resolveSplits(
        tabEntryId: String,
        splits: List<NewExpenseSplit>,
        totalAmount: Double,
    ): List<TabEntrySplit> {
        if (splits.isEmpty()) return emptyList()
        val resolvedAmounts = SplitResolver.resolveAmounts(splits, totalAmount)
        return splits.mapIndexed { index, split ->
            TabEntrySplit(
                splitId = generateLocalId(),
                tabEntryId = tabEntryId,
                participantId = split.participantId,
                splitType = split.splitType,
                value = split.value,
                resolvedAmount = resolvedAmounts[index],
            )
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun generateLocalId(): String = Uuid.random().toString()
}
