package de.tabmates.features.tabgroup.presentation.navigation.addentry

import de.tabmates.core.presentation.util.UiText

sealed interface AddEntryEvent {
    data object EntrySaved : AddEntryEvent

    data class Error(val message: UiText) : AddEntryEvent
}
