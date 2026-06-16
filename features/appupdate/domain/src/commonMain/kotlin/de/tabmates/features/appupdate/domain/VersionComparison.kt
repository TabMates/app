package de.tabmates.features.appupdate.domain

/** True if [current] is a strictly lower version than [target] (numeric, dot-separated). */
fun isVersionLower(
    current: String,
    target: String,
): Boolean = compareVersions(current, target) < 0

/**
 * Compares two dot-separated version strings segment by segment (e.g. "1.2.0" vs "1.10").
 * Missing trailing segments count as 0, so "1.2" == "1.2.0". Non-numeric leading characters
 * per segment are stripped; unparseable segments count as 0.
 */
internal fun compareVersions(
    a: String,
    b: String,
): Int {
    val pa = a.numericSegments()
    val pb = b.numericSegments()
    val max = maxOf(pa.size, pb.size)
    for (i in 0 until max) {
        val cmp = pa.getOrElse(i) { 0 }.compareTo(pb.getOrElse(i) { 0 })
        if (cmp != 0) return cmp
    }
    return 0
}

private fun String.numericSegments(): List<Int> =
    split('.', '-', '+')
        .map { segment -> segment.takeWhile { it.isDigit() }.toIntOrNull() ?: 0 }
