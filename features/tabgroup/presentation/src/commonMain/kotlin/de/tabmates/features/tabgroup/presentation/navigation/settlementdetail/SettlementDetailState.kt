package de.tabmates.features.tabgroup.presentation.navigation.settlementdetail

import de.tabmates.features.tabgroup.domain.models.GroupParticipant
import de.tabmates.features.tabgroup.domain.models.TabEntry

data class SettlementDetailState(
    val settlementId: String = "",
    val isLoading: Boolean = true,
    // The lookup came back empty: the settlement was deleted, or the id names something this screen
    // can't render. Drives SettlementDetailEvent.SettlementUnavailable rather than any UI of its own.
    val isMissing: Boolean = false,
    val isDeleting: Boolean = false,
    val settlement: TabEntry.Settlement? = null,
    val currentUserId: String = "",
    val groupCurrencySymbol: String = "",
    val groupCurrencyDecimalDigits: Int = 2,
    val membersById: Map<String, GroupParticipant> = emptyMap(),
)
