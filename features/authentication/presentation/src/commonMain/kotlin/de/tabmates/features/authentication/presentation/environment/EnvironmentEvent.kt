package de.tabmates.features.authentication.presentation.environment

sealed interface EnvironmentEvent {
    /** The app now talks to another backend; the screen has done its job and closes. */
    data object Switched : EnvironmentEvent
}
