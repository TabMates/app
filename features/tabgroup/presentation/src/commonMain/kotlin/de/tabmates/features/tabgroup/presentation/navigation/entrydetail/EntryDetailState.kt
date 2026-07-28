package de.tabmates.features.tabgroup.presentation.navigation.entrydetail

import de.tabmates.features.tabgroup.domain.models.GroupParticipant
import de.tabmates.features.tabgroup.domain.models.TabEntry
import de.tabmates.features.tabgroup.domain.models.TabEntrySplit
import de.tabmates.features.tabgroup.presentation.navigation.addentry.EntryKind
import kotlin.time.Instant

data class EntryDetailState(
    val entryId: String = "",
    val isLoading: Boolean = true,
    // The lookup came back empty: the entry was deleted, or the id names something this screen can't
    // render. Drives EntryDetailEvent.EntryUnavailable rather than any UI of its own.
    val isMissing: Boolean = false,
    val isDeleting: Boolean = false,
    // The entry being shown — an expense or an income (both split-carrying). Null while loading or
    // for an entry kind this screen doesn't render (e.g. a settlement, which has its own screen).
    val entry: TabEntry? = null,
    val entryKind: EntryKind = EntryKind.EXPENSE,
    val splits: List<TabEntrySplit> = emptyList(),
    val currentUserId: String = "",
    // The currency the entry was recorded in — amounts on this screen render in it.
    val entryCurrencyCode: String = "",
    val entryCurrencySymbol: String = "",
    val entryCurrencyDecimalDigits: Int = 2,
    // The group's default currency, base for the converted amount and exchange rate.
    val groupCurrencyCode: String = "",
    val groupCurrencySymbol: String = "",
    val groupCurrencyDecimalDigits: Int = 2,
    val ratesByCurrency: Map<String, Double> = emptyMap(),
    val ratesLastUpdatedAt: Instant? = null,
    val membersById: Map<String, GroupParticipant> = emptyMap(),
) {
    /** True when the entry's currency differs from the group's default currency. */
    val isForeignCurrency: Boolean
        get() =
            groupCurrencyCode.isNotEmpty() &&
                entryCurrencyCode.isNotEmpty() &&
                entryCurrencyCode != groupCurrencyCode
}
