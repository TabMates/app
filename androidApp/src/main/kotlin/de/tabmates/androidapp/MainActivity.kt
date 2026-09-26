package de.tabmates.androidapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import de.tabmates.composeapp.App
import de.tabmates.composeapp.deeplink.DeepLinkHandler

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        handleIntent(intent)

        setContent {
            App()
            // Play flavor asks for POST_NOTIFICATIONS; the FOSS flavor has no notifications and
            // supplies a no-op. See PlatformNotifications.kt in src/play and src/foss.
            NotificationPermissionGate()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val uri = intent.data?.toString() ?: return
        DeepLinkHandler.onDeepLink(uri)
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
