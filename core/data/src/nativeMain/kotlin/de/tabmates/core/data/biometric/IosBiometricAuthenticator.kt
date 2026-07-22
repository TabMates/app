package de.tabmates.core.data.biometric

import de.tabmates.core.domain.biometric.BiometricAuthenticator
import de.tabmates.core.domain.biometric.BiometricAvailability
import de.tabmates.core.domain.biometric.BiometricPromptStrings
import de.tabmates.core.domain.biometric.BiometricResult
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSError
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthentication
import kotlin.coroutines.resume

/**
 * iOS biometric authentication backed by `LocalAuthentication`.
 *
 * Uses `LAPolicyDeviceOwnerAuthentication`, which evaluates Face ID / Touch ID and automatically
 * falls back to the device passcode — matching the Android device-credential fallback.
 */
@OptIn(ExperimentalForeignApi::class)
class IosBiometricAuthenticator : BiometricAuthenticator {
    // iOS error codes (LAError), compared numerically to avoid cinterop constant coupling.
    private companion object {
        const val LA_ERROR_USER_CANCEL = -2L
        const val LA_ERROR_USER_FALLBACK = -3L
        const val LA_ERROR_SYSTEM_CANCEL = -4L
        const val LA_ERROR_APP_CANCEL = -9L
        const val LA_ERROR_BIOMETRY_NOT_ENROLLED = -7L
        const val LA_ERROR_PASSCODE_NOT_SET = -8L
    }

    override fun availability(): BiometricAvailability =
        memScoped {
            val errorVar = alloc<ObjCObjectVar<NSError?>>()
            val canEvaluate =
                LAContext().canEvaluatePolicy(LAPolicyDeviceOwnerAuthentication, errorVar.ptr)
            if (canEvaluate) {
                BiometricAvailability.AVAILABLE
            } else {
                when (errorVar.value?.code) {
                    LA_ERROR_BIOMETRY_NOT_ENROLLED, LA_ERROR_PASSCODE_NOT_SET ->
                        BiometricAvailability.NOT_ENROLLED
                    else -> BiometricAvailability.NO_HARDWARE
                }
            }
        }

    override suspend fun authenticate(strings: BiometricPromptStrings): BiometricResult {
        val context = LAContext()
        strings.cancel.takeIf { it.isNotBlank() }?.let { context.localizedCancelTitle = it }
        return suspendCancellableCoroutine { continuation ->
            context.evaluatePolicy(
                policy = LAPolicyDeviceOwnerAuthentication,
                localizedReason = strings.subtitle.ifBlank { strings.title },
            ) { success, error ->
                if (!continuation.isActive) return@evaluatePolicy
                val result =
                    when {
                        success -> BiometricResult.Success
                        error?.code in cancelCodes -> BiometricResult.Cancelled
                        else -> BiometricResult.Error(error?.localizedDescription ?: "Authentication failed")
                    }
                continuation.resume(result)
            }
        }
    }

    private val cancelCodes =
        setOf(LA_ERROR_USER_CANCEL, LA_ERROR_USER_FALLBACK, LA_ERROR_SYSTEM_CANCEL, LA_ERROR_APP_CANCEL)
}
