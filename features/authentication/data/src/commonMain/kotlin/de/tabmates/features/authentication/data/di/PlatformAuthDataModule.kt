package de.tabmates.features.authentication.data.di

/**
 * Platform-specific Koin module providing the
 * [de.tabmates.features.authentication.domain.TurnstileTokenProvider].
 * Android/iOS/Desktop actuals bind a no-op (Turnstile-exempt); the web actual binds the
 * invisible-widget provider.
 */
expect class PlatformAuthDataModule()
