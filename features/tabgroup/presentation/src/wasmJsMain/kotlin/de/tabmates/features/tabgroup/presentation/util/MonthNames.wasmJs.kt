@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package de.tabmates.features.tabgroup.presentation.util

actual fun platformShortMonthNames(): List<String> = (0..11).map { shortMonthForIndex(it) }

private fun shortMonthForIndex(month: Int): String =
    js("new Intl.DateTimeFormat(undefined, { month: 'short' }).format(new Date(2020, month, 1))")
