package de.tabmates.androidapp

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.tabmates.composeapp.deeplink.DeepLinkHandler
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(application = Application::class)
class MainActivityDeepLinkTest {
    @After
    fun tearDown() {
        DeepLinkHandler.listener = null
    }

    @Test
    fun onCreate_withDeepLinkIntent_forwardsUriToHandler() {
        var received: String? = null
        DeepLinkHandler.listener = { received = it }

        val intent =
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://app.tabmates.de/api/auth/verify?token=abc123"),
            )

        Robolectric
            .buildActivity(MainActivity::class.java, intent)
            .create()

        assertEquals("https://app.tabmates.de/api/auth/verify?token=abc123", received)
    }

    @Test
    fun onCreate_withoutData_doesNotCallHandler() {
        var received: String? = null
        DeepLinkHandler.listener = { received = it }

        Robolectric
            .buildActivity(MainActivity::class.java)
            .create()

        assertNull(received)
    }

    @Test
    fun onNewIntent_forwardsUriToHandler() {
        var received: String? = null

        val activityController =
            Robolectric
                .buildActivity(MainActivity::class.java)
                .create()

        DeepLinkHandler.listener = { received = it }

        val newIntent =
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://app.tabmates.de/api/auth/reset-password?token=xyz789"),
            )
        activityController.newIntent(newIntent)

        assertEquals("https://app.tabmates.de/api/auth/reset-password?token=xyz789", received)
    }

    // region — custom tabmates:// scheme (matches second intent-filter in AndroidManifest)

    @Test
    fun onCreate_withTabmatesSchemeVerify_forwardsUriToHandler() {
        var received: String? = null
        DeepLinkHandler.listener = { received = it }

        val intent =
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("tabmates://app.tabmates.de/api/auth/verify?token=abc123"),
            )

        Robolectric
            .buildActivity(MainActivity::class.java, intent)
            .create()

        assertEquals("tabmates://app.tabmates.de/api/auth/verify?token=abc123", received)
    }

    @Test
    fun onCreate_withTabmatesSchemeResetPassword_forwardsUriToHandler() {
        var received: String? = null
        DeepLinkHandler.listener = { received = it }

        val intent =
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("tabmates://app.tabmates.de/api/auth/reset-password?token=xyz789"),
            )

        Robolectric
            .buildActivity(MainActivity::class.java, intent)
            .create()

        assertEquals("tabmates://app.tabmates.de/api/auth/reset-password?token=xyz789", received)
    }

    @Test
    fun onNewIntent_withTabmatesScheme_forwardsUriToHandler() {
        var received: String? = null

        val activityController =
            Robolectric
                .buildActivity(MainActivity::class.java)
                .create()

        DeepLinkHandler.listener = { received = it }

        val newIntent =
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("tabmates://app.tabmates.de/api/auth/verify?token=abc123"),
            )
        activityController.newIntent(newIntent)

        assertEquals("tabmates://app.tabmates.de/api/auth/verify?token=abc123", received)
    }

    // endregion

    // region — non-hierarchical URIs from Android

    @Test
    fun onCreate_withNonHierarchicalMailtoUri_doesNotCrash() {
        var received: String? = null
        DeepLinkHandler.listener = { received = it }

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("mailto:user@example.com"))

        Robolectric
            .buildActivity(MainActivity::class.java, intent)
            .create()

        // URI is forwarded; resolver will safely return null
        assertEquals("mailto:user@example.com", received)
    }

    @Test
    fun onCreate_withNonHierarchicalTelUri_doesNotCrash() {
        var received: String? = null
        DeepLinkHandler.listener = { received = it }

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("tel:+1234567890"))

        Robolectric
            .buildActivity(MainActivity::class.java, intent)
            .create()

        assertEquals("tel:+1234567890", received)
    }

    // endregion
}
