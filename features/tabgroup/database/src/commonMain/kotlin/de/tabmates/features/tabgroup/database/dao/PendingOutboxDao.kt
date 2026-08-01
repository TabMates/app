package de.tabmates.features.tabgroup.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import de.tabmates.features.tabgroup.database.entities.PendingOutboxEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingOutboxDao {
    @Upsert
    suspend fun upsert(item: PendingOutboxEntity)

    @Query("SELECT * FROM PendingOutboxEntity ORDER BY createdAt ASC")
    suspend fun getAll(): List<PendingOutboxEntity>

    /**
     * Resolves the row a server acknowledgement belongs to. Needed as well as the outbox's
     * in-memory map: an ack can arrive on a socket opened after the process that sent the write
     * was killed, and by then only the persisted id survives.
     */
    @Query("SELECT * FROM PendingOutboxEntity WHERE requestId = :requestId LIMIT 1")
    suspend fun getByRequestId(requestId: String): PendingOutboxEntity?

    @Query("SELECT * FROM PendingOutboxEntity ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<PendingOutboxEntity>>

    @Query("SELECT COUNT(*) FROM PendingOutboxEntity")
    fun observeCount(): Flow<Int>

    @Query("DELETE FROM PendingOutboxEntity WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM PendingOutboxEntity")
    suspend fun deleteAll()
}
