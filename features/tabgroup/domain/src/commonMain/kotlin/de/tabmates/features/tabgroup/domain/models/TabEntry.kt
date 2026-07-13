package de.tabmates.features.tabgroup.domain.models

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

sealed class TabEntry {
    abstract val tabEntryId: String
    abstract val groupId: String
    abstract val title: String
    abstract val description: String
    abstract val amount: Double
    abstract val currencyCode: String

    /**
     * Exchange rate locked in when the entry was created (or its currency last changed): units
     * of the group's default currency per 1 unit of [currencyCode], so
     * `converted amount = amount * exchangeRate`. Null when the entry is in the group's own
     * currency, no rate was available at creation, or the entry predates this field — consumers
     * must fall back to live rates then.
     */
    abstract val exchangeRate: Double?
    abstract val creatorId: String
    abstract val paidByUserId: String

    /** Calendar date the entry actually happened (client-chosen), distinct from [createdAt]. */
    abstract val entryDate: LocalDate
    abstract val createdAt: Instant
    abstract val lastModifiedAt: Instant
    abstract val lastModifiedByUserId: String
    abstract val version: Int
    abstract val deletedAt: Instant?
    abstract val deletedByUserId: String?

    /** True while this entry is a local optimistic write not yet confirmed by the server. */
    abstract val isPendingSync: Boolean

    val isDeleted: Boolean
        get() = deletedAt != null

    data class Expense(
        override val tabEntryId: String,
        override val groupId: String,
        override val title: String,
        override val description: String,
        override val amount: Double,
        override val currencyCode: String,
        override val exchangeRate: Double? = null,
        override val creatorId: String,
        override val paidByUserId: String,
        override val entryDate: LocalDate,
        override val createdAt: Instant,
        override val lastModifiedAt: Instant,
        override val lastModifiedByUserId: String,
        override val version: Int,
        override val deletedAt: Instant?,
        override val deletedByUserId: String?,
        val splits: List<TabEntrySplit>,
        override val isPendingSync: Boolean = false,
    ) : TabEntry()

    data class Income(
        override val tabEntryId: String,
        override val groupId: String,
        override val title: String,
        override val description: String,
        override val amount: Double,
        override val currencyCode: String,
        override val exchangeRate: Double? = null,
        override val creatorId: String,
        override val paidByUserId: String,
        override val entryDate: LocalDate,
        override val createdAt: Instant,
        override val lastModifiedAt: Instant,
        override val lastModifiedByUserId: String,
        override val version: Int,
        override val deletedAt: Instant?,
        override val deletedByUserId: String?,
        val splits: List<TabEntrySplit>,
        override val isPendingSync: Boolean = false,
    ) : TabEntry()

    data class Settlement(
        override val tabEntryId: String,
        override val groupId: String,
        override val title: String,
        override val description: String,
        override val amount: Double,
        override val currencyCode: String,
        override val exchangeRate: Double? = null,
        override val creatorId: String,
        override val paidByUserId: String,
        override val entryDate: LocalDate,
        override val createdAt: Instant,
        override val lastModifiedAt: Instant,
        override val lastModifiedByUserId: String,
        override val version: Int,
        override val deletedAt: Instant?,
        override val deletedByUserId: String?,
        val receivedByUserId: String,
        override val isPendingSync: Boolean = false,
    ) : TabEntry()
}
