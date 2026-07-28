package de.tabmates.features.tabgroup.presentation.navigation.settlementdetail

import de.tabmates.core.presentation.util.UiText

sealed interface SettlementDetailEvent {
    data object SettlementDeleted : SettlementDetailEvent

    /**
     * The settlement is gone — deleted elsewhere, or reached through a stale link. The screen backs
     * out instead of waiting for something that will never arrive.
     */
    data object SettlementUnavailable : SettlementDetailEvent

    data class Error(val message: UiText) : SettlementDetailEvent
}
