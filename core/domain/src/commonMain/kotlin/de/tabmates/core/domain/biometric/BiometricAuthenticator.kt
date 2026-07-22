package de.tabmates.core.domain.biometric

/** Whether the device can perform biometric (or device-credential) authentication right now. */
enum class BiometricAvailability {
    /** Biometrics (or a device credential fallback) are enrolled and ready to use. */
    AVAILABLE,

    /** Hardware exists but the user has not enrolled a biometric or set a device credential. */
    NOT_ENROLLED,

    /** The device has no biometric hardware (or a device credential the app can use). */
    NO_HARDWARE,

    /** This platform has no biometric-authentication concept (Desktop/Web). */
    UNSUPPORTED,
}

/** Outcome of a single authentication attempt. */
sealed interface BiometricResult {
    /** The user authenticated successfully. */
    data object Success : BiometricResult

    /** The user cancelled the prompt (back press, negative button, dismissed). */
    data object Cancelled : BiometricResult

    /**
     * Authentication could not proceed or failed unrecoverably (lockout, no hardware,
     * unsupported platform). [message] is a developer-facing description, not for display.
     */
    data class Error(val message: String) : BiometricResult
}

/** Localized, caller-supplied copy for the system biometric prompt. */
data class BiometricPromptStrings(
    val title: String,
    val subtitle: String,
    /** Fallback/cancel button label; ignored where the OS renders a device-credential fallback. */
    val cancel: String,
)

/**
 * Local device authentication (fingerprint / face / device PIN-password fallback).
 *
 * Platform-specific: Android uses `androidx.biometric.BiometricPrompt`, iOS uses
 * `LocalAuthentication`, and Desktop/Web fall back to an unsupported no-op. Injected so the
 * app-lock gate and the Settings toggle can gate access to the app behind the device owner.
 */
interface BiometricAuthenticator {
    /** Current hardware/enrollment state; drives whether the Settings toggle can be enabled. */
    fun availability(): BiometricAvailability

    /** Present the OS prompt and suspend until the user authenticates, cancels, or it errors. */
    suspend fun authenticate(strings: BiometricPromptStrings): BiometricResult
}
