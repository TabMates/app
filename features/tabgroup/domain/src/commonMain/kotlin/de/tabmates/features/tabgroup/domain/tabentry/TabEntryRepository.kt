package de.tabmates.features.tabgroup.domain.tabentry

import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.EmptyResult
import de.tabmates.core.domain.util.Result
import de.tabmates.features.tabgroup.domain.models.TabEntry
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

interface TabEntryRepository {
    fun getTabEntriesForGroup(groupId: String): Flow<List<TabEntry>>

    fun getTabEntryById(tabEntryId: String): Flow<TabEntry?>

    suspend fun fetchTabEntries(
        groupId: String,
        before: Instant? = null,
        pageSize: Int = DEFAULT_PAGE_SIZE,
    ): Result<List<TabEntry>, DataError.Remote>

    suspend fun createExpense(
        groupId: String,
        title: String,
        description: String,
        amount: Double,
        currencyCode: String,
        paidByUserId: String,
        createdAt: Instant,
        splits: List<NewExpenseSplit>,
    ): Result<TabEntry.Expense, DataError.Remote>

    suspend fun updateExpense(
        tabEntryId: String,
        groupId: String,
        title: String,
        description: String,
        amount: Double,
        currencyCode: String,
        paidByUserId: String,
        createdAt: Instant,
        splits: List<NewExpenseSplit>,
    ): Result<TabEntry.Expense, DataError.Remote>

    suspend fun createSettlement(
        groupId: String,
        title: String,
        description: String,
        amount: Double,
        currencyCode: String,
        paidByUserId: String,
        receivedByUserId: String,
        createdAt: Instant,
    ): Result<TabEntry.Settlement, DataError.Remote>

    suspend fun deleteTabEntry(tabEntryId: String): EmptyResult<DataError.Remote>

    companion object {
        const val DEFAULT_PAGE_SIZE = 20
    }
}
