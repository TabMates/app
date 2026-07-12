package de.tabmates.features.tabgroup.presentation.navigation.settlementdetail

import de.tabmates.core.presentation.util.UiText

sealed interface SettlementDetailEvent {
    data object SettlementDeleted : SettlementDetailEvent

    data class Error(val message: UiText) : SettlementDetailEvent
}
