package de.tabmates.core.domain.preferences

/** Resolves the currency implied by the device's current locale. */
interface DeviceCurrencyProvider {
    /** ISO 4217 code for the device's current locale, or null if it cannot be determined. */
    fun currentCurrencyCode(): String?
}
