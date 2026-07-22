package de.tabmates.core.data.biometric

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import de.tabmates.core.domain.biometric.BiometricAuthenticator
import de.tabmates.core.domain.biometric.BiometricAvailability
import de.tabmates.core.domain.biometric.BiometricPromptStrings
import de.tabmates.core.domain.biometric.BiometricResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Android biometric authentication backed by `androidx.biometric.BiometricPrompt`.
 *
 * Allows the device-credential (PIN/pattern/password) fallback alongside a strong biometric, so a
 * user without an enrolled fingerprint/face can still unlock. The prompt requires a
 * [androidx.fragment.app.FragmentActivity], obtained from [CurrentActivityHolder].
 */
class AndroidBiometricAuthenticator(
    private val context: Context,
) : BiometricAuthenticator {
    override fun availability(): BiometricAvailability =
        when (BiometricManager.from(context).canAuthenticate(allowedAuthenticators())) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NOT_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE,
            -> BiometricAvailability.NO_HARDWARE
            else -> BiometricAvailability.NO_HARDWARE
        }

    override suspend fun authenticate(strings: BiometricPromptStrings): BiometricResult {
        val activity =
            CurrentActivityHolder.current()
                ?: return BiometricResult.Error("No foreground activity available for biometric prompt")

        // BiometricPrompt must be created and shown on the main thread.
        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                val executor = ContextCompat.getMainExecutor(activity)
                val prompt =
                    BiometricPrompt(
                        activity,
                        executor,
                        object : BiometricPrompt.AuthenticationCallback() {
                            override fun onAuthenticationSucceeded(
                                result: BiometricPrompt.AuthenticationResult,
                            ) {
                                if (continuation.isActive) continuation.resume(BiometricResult.Success)
                            }

                            override fun onAuthenticationError(
                                errorCode: Int,
                                errString: CharSequence,
                            ) {
                                if (!continuation.isActive) return
                                val result =
                                    when (errorCode) {
                                        BiometricPrompt.ERROR_USER_CANCELED,
                                        BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                                        BiometricPrompt.ERROR_CANCELED,
                                        -> BiometricResult.Cancelled
                                        else -> BiometricResult.Error("$errorCode: $errString")
                                    }
                                continuation.resume(result)
                            }

                            // Called on a non-fatal miss (e.g. wrong finger). The prompt stays up,
                            // so we wait for a terminal success/error rather than resuming here.
                            override fun onAuthenticationFailed() = Unit
                        },
                    )

                val promptInfo =
                    BiometricPrompt.PromptInfo.Builder()
                        .setTitle(strings.title)
                        .setSubtitle(strings.subtitle)
                        .apply {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
                            } else {
                                // Pre-R can't combine authenticators; the deprecated flag still
                                // offers the device-credential fallback on API 26-29.
                                @Suppress("DEPRECATION")
                                setDeviceCredentialAllowed(true)
                            }
                        }
                        .build()

                prompt.authenticate(promptInfo)
                continuation.invokeOnCancellation { prompt.cancelAuthentication() }
            }
        }
    }

    private fun allowedAuthenticators(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BIOMETRIC_STRONG or DEVICE_CREDENTIAL
        } else {
            BIOMETRIC_STRONG
        }
}
