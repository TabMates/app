package de.tabmates.features.tabgroup.presentation.navigation.creategroup

import de.tabmates.core.domain.preferences.DeviceCurrencyProvider

class FakeDeviceCurrencyProvider(
    private val code: String? = null,
) : DeviceCurrencyProvider {
    override fun currentCurrencyCode(): String? = code
}
