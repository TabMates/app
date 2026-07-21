package de.tabmates.features.tabgroup.presentation.navigation.entrydetail

import de.tabmates.core.presentation.util.UiText

sealed interface EntryDetailEvent {
    data object EntryDeleted : EntryDetailEvent

    data class Error(val message: UiText) : EntryDetailEvent
}
