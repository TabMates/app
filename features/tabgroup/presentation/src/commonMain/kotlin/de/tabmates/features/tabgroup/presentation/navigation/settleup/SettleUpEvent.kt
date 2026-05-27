package de.tabmates.features.tabgroup.presentation.navigation.settleup

import de.tabmates.core.presentation.util.UiText

sealed interface SettleUpEvent {
    data class PaymentRecorded(val toName: String) : SettleUpEvent

    data class Error(val message: UiText) : SettleUpEvent
}
