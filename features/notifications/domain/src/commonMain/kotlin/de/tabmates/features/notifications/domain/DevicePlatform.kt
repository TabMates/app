package de.tabmates.features.notifications.domain

/**
 * Platform a push-notification device token belongs to. [wireValue] is the stable string sent to
 * the backend on registration — decoupled from the enum name so renaming a constant can't silently
 * change the API contract. Values match the backend's `PlatformDto` enum names
 * (TabMatesServer notification/…/api/dto/RegisterDeviceRequest.kt).
 */
enum class DevicePlatform(val wireValue: String) {
    ANDROID("ANDROID"),
    IOS("IOS"),
    DESKTOP("DESKTOP"),
    WEB("WEB"),
}
