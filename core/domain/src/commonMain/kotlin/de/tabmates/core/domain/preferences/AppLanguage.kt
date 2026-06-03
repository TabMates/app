package de.tabmates.core.domain.preferences

/**
 * User-selectable in-app language. [SYSTEM] follows the device locale; every other entry
 * pins a specific language via its BCP-47 [languageTag]. Add new supported languages here.
 */
enum class AppLanguage(val languageTag: String?) {
    SYSTEM(null),
    ENGLISH("en"),
    GERMAN("de"),
}
