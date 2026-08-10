package de.tabmates.features.tabgroup.presentation.navigation.profile

sealed interface ProfileEvent {
    data object SignedOut : ProfileEvent
}
