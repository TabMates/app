@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package de.tabmates.features.tabgroup.presentation.util

actual fun platformNameComparator(): Comparator<String> {
    val collator = newCollator()
    return Comparator { a, b -> compareWith(collator, a, b) }
}

private fun newCollator(): JsAny = js("new Intl.Collator(undefined, { sensitivity: 'base' })")

private fun compareWith(
    collator: JsAny,
    a: String,
    b: String,
): Int = js("collator.compare(a, b)")
