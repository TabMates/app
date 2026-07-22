package de.tabmates.core.data.biometric

import androidx.fragment.app.FragmentActivity
import java.lang.ref.WeakReference

/**
 * Holds a weak reference to the currently-resumed [FragmentActivity].
 *
 * `BiometricPrompt` must be built from a `FragmentActivity`, but the shared graph only has an
 * application `Context`. The Android host activity registers itself here on resume and clears it on
 * destroy, letting [AndroidBiometricAuthenticator] reach the foreground activity without leaking it.
 */
object CurrentActivityHolder {
    private var ref: WeakReference<FragmentActivity>? = null

    fun set(activity: FragmentActivity) {
        ref = WeakReference(activity)
    }

    fun clear(activity: FragmentActivity) {
        // Only clear if the stored activity is the one being destroyed, to avoid a newer
        // activity's reference being wiped by an older one's teardown.
        if (ref?.get() === activity) ref = null
    }

    fun current(): FragmentActivity? = ref?.get()
}
