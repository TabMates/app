package de.tabmates.features.notifications.data

import de.tabmates.features.notifications.data.dto.requests.RegisterDeviceRequest
import de.tabmates.features.notifications.domain.DevicePlatform
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RegisterDeviceRequestTest {
    @Test
    fun devicePlatform_usesStableBackendEnumWireValues() {
        assertEquals("ANDROID", DevicePlatform.ANDROID.wireValue)
        assertEquals("IOS", DevicePlatform.IOS.wireValue)
        assertEquals("DESKTOP", DevicePlatform.DESKTOP.wireValue)
        assertEquals("WEB", DevicePlatform.WEB.wireValue)
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
        // wireValue matches the backend PlatformDto enum name
        assertTrue(json.contains("\"platform\":\"IOS\""), json)
        assertTrue(json.contains("\"locale\":\"de\""), json)
    }
}
