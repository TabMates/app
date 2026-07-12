package de.tabmates.core.data.preferences

import de.tabmates.core.domain.preferences.DeviceCurrencyProvider
import org.koin.core.annotation.Single

@Single(binds = [DeviceCurrencyProvider::class])
class DefaultDeviceCurrencyProvider : DeviceCurrencyProvider {
    override fun currentCurrencyCode(): String? = deviceCurrencyCode()
}
