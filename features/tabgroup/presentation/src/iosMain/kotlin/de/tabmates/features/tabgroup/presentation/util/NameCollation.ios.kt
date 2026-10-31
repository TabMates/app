package de.tabmates.features.tabgroup.presentation.util

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSCaseInsensitiveSearch
import platform.Foundation.NSDiacriticInsensitiveSearch
import platform.Foundation.NSLocale
import platform.Foundation.NSMakeRange
import platform.Foundation.NSString
import platform.Foundation.compare
import platform.Foundation.create
import platform.Foundation.currentLocale

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual fun platformNameComparator(): Comparator<String> {
    val locale = NSLocale.currentLocale
    return Comparator { a, b ->
        val left = NSString.create(string = a)
        left
            .compare(
                string = b,
                options = NSCaseInsensitiveSearch or NSDiacriticInsensitiveSearch,
                range = NSMakeRange(0u, left.length),
                locale = locale,
            ).toInt()
    }
}
