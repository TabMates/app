package de.tabmates.features.tabgroup.presentation.navigation.profile

import de.tabmates.core.presentation.util.UiText

sealed interface ProfileEvent {
    data object SignedOut : ProfileEvent

    data class Error(val message: UiText) : ProfileEvent
}
