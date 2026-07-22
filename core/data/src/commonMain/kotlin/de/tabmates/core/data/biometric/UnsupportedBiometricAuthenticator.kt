package de.tabmates.core.data.biometric

import de.tabmates.core.domain.biometric.BiometricAuthenticator
import de.tabmates.core.domain.biometric.BiometricAvailability
import de.tabmates.core.domain.biometric.BiometricPromptStrings
import de.tabmates.core.domain.biometric.BiometricResult

/** Default authenticator for platforms without a biometric concept (Desktop, Web). */
class UnsupportedBiometricAuthenticator : BiometricAuthenticator {
    override fun availability(): BiometricAvailability = BiometricAvailability.UNSUPPORTED

    override suspend fun authenticate(strings: BiometricPromptStrings): BiometricResult =
        BiometricResult.Error("Biometric authentication is not supported on this platform")
}
