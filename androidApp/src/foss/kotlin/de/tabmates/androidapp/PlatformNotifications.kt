package de.tabmates.androidapp

import android.app.Application
import androidx.compose.runtime.Composable

/**
 * FOSS (F-Droid) notification setup: there is none, and that is deliberate.
 *
 * This flavor has no Firebase Cloud Messaging and no local-notification source, so nothing ever
 * posts a notification — leaving the channels registered would only add empty categories to the
 * system settings screen.
 *
 * The permission gate is not merely unnecessary, it would be a bug. `POST_NOTIFICATIONS` reaches
 * the merged manifest only through the `firebase-messaging` and `kmpnotifier-core` AAR manifests,
 * which this flavor does not depend on. Requesting an undeclared permission is denied instantly
 * and `shouldShowRequestPermissionRationale` then returns false, so the Play gate's logic would
 * land on its "permanently denied" branch and show an unavoidable "open settings" dialog on every
 * cold start — for a feature this build does not have.
 *
 * See the `play` source set for the real implementations.
 */
internal fun Application.installNotificationChannels() = Unit

@Composable
internal fun NotificationPermissionGate() = Unit
