package de.tabmates.composeapp.session

import de.tabmates.core.presentation.util.UiText

sealed interface ReauthEvent {
    /** Back in as the same account; the outbox can drain. */
    data object ReauthSucceeded : ReauthEvent

    data class ReauthFailed(
        val error: UiText,
    ) : ReauthEvent
}
