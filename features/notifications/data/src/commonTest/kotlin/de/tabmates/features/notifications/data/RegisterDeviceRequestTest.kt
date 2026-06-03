package de.tabmates.features.notifications.data

import de.tabmates.features.notifications.data.dto.requests.RegisterDeviceRequest
import de.tabmates.features.notifications.domain.DevicePlatform
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RegisterDeviceRequestTest {
    @Test
    fun devicePlatform_usesStableLowercaseWireValues() {
        assertEquals("android", DevicePlatform.ANDROID.wireValue)
        assertEquals("ios", DevicePlatform.IOS.wireValue)
        assertEquals("desktop", DevicePlatform.DESKTOP.wireValue)
        assertEquals("web", DevicePlatform.WEB.wireValue)
    }

    @Test
    fun serializes_token_platform_and_locale() {
        val json =
            Json.encodeToString(
                RegisterDeviceRequest(
                    token = "tok-123",
                    platform = DevicePlatform.IOS.wireValue,
                    locale = "de",
                ),
            )

        assertTrue(json.contains("\"token\":\"tok-123\""), json)
        // wireValue ("ios"), not the enum name ("IOS")
        assertTrue(json.contains("\"platform\":\"ios\""), json)
        assertTrue(json.contains("\"locale\":\"de\""), json)
    }
}
