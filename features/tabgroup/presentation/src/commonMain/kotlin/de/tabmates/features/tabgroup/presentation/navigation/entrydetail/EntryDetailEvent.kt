package de.tabmates.features.tabgroup.presentation.navigation.entrydetail

import de.tabmates.core.presentation.util.UiText

sealed interface EntryDetailEvent {
    data object EntryDeleted : EntryDetailEvent

    /**
     * The entry is gone — deleted elsewhere, or reached through a stale link. The screen backs out
     * instead of waiting for something that will never arrive.
     */
    data object EntryUnavailable : EntryDetailEvent

    data class Error(val message: UiText) : EntryDetailEvent
}
