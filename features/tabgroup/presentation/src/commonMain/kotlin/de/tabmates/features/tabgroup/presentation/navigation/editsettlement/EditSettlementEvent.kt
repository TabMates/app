package de.tabmates.features.tabgroup.presentation.navigation.editsettlement

import de.tabmates.core.presentation.util.UiText

sealed interface EditSettlementEvent {
    data object SettlementSaved : EditSettlementEvent

    data class Error(val message: UiText) : EditSettlementEvent
}
