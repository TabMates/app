package de.tabmates.composeapp.deeplink

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.serializer

/**
 * Maps a base URI (scheme + host + path) to a [NavKey] by deserializing query
 * parameters into a `@Serializable` NavKey.
 *
 * Mirrors the upcoming Nav3 `navDeepLink<T>(basePath)` API.
 * TODO: Replace with `androidx.navigation3.NavDeepLink` when available.
 *
 * @param basePath Full URI prefix including scheme, host, and path
 *   (e.g. `https://example.com/api/auth/verify`).
 * @param serializer The KotlinX [KSerializer] for the target NavKey type.
 */
data class NavDeepLink<T : NavKey>(
    val basePath: String,
    val serializer: KSerializer<T>,
    val pathSuffixParam: String? = null,
)

/**
 * Creates a [NavDeepLink] definition.
 *
 * Mirrors the upcoming Nav3 `navDeepLink<T>(basePath)` API signature.
 * TODO: Replace import with `androidx.navigation3.navDeepLink` when available.
 *
 * @param pathSuffixParam If set, the path segment immediately following [basePath]
 *   (e.g. `abc123` in `/j/abc123`) is decoded as the value for this query param.
 */
inline fun <reified T : NavKey> navDeepLink(
    basePath: String,
    pathSuffixParam: String? = null,
): NavDeepLink<T> = NavDeepLink(basePath = basePath, serializer = serializer(), pathSuffixParam = pathSuffixParam)

private val lenientJson = Json { ignoreUnknownKeys = true; isLenient = true }

/**
 * Resolves a deep-link URI to a [NavKey] by matching against registered [NavDeepLink]s.
 *
 * Matching follows RFC 3986 decomposition: the scheme is ignored (both `https://`
 * and custom schemes like `tabmates://` match), the host is compared
 * case-insensitively, and the path must match exactly (or be a direct sub-path).
 * Query parameters are decoded per `application/x-www-form-urlencoded`.
 *
 * @param uri The incoming deep-link URI string.
 * @param deepLinks The list of registered deep-link definitions to match against.
 * @return The resolved [NavKey], or `null` if no definition matches the URI.
 */
fun resolveDeepLink(uri: String, deepLinks: List<NavDeepLink<*>>): NavKey? {
    val parsed = decomposeUri(uri) ?: return null
    val decodedPath = percentDecode(parsed.path)

    val deepLink = deepLinks.firstOrNull { link ->
        val base = decomposeUri(link.basePath) ?: return@firstOrNull false
        parsed.host.equals(base.host, ignoreCase = true) &&
            pathMatches(incoming = decodedPath, base = base.path)
    } ?: return null

    val basePath = decomposeUri(deepLink.basePath)?.path.orEmpty()
    val queryParams = parseQueryParams(parsed.rawQuery).toMutableMap()
    deepLink.pathSuffixParam?.let { name ->
        val suffix = decodedPath
            .removePrefix(basePath)
            .removePrefix("/")
            .substringBefore('/')
        if (suffix.isNotEmpty()) queryParams[name] = suffix
    }
    return decodeNavKey(deepLink, queryParams)
}

// ---------------------------------------------------------------------------
// URI decomposition (RFC 3986)
// ---------------------------------------------------------------------------

/**
 * Minimal RFC 3986 decomposition of an absolute URI into host, path, and
 * raw query. Returns `null` for URIs without a `://` scheme delimiter.
 */
private data class DecomposedUri(
    val host: String,
    val path: String,
    val rawQuery: String,
)

private fun decomposeUri(uri: String): DecomposedUri? {
    val schemeEnd = uri.indexOf("://")
    if (schemeEnd == -1) return null

    val afterScheme = uri.substring(schemeEnd + 3)
    // RFC 3986 §3.5: the fragment is always the last component — strip first
    val withoutFragment = afterScheme.substringBefore("#")
    // RFC 3986 §3.4: everything after the first '?' is the query
    val authorityAndPath = withoutFragment.substringBefore("?")
    val rawQuery = withoutFragment.substringAfter("?", "")

    val host = authorityAndPath.substringBefore("/")
    val path = authorityAndPath.removePrefix(host) // retains leading '/'

    return DecomposedUri(host = host, path = path, rawQuery = rawQuery)
}

/** Exact match or direct sub-path (trailing `/`). */
private fun pathMatches(incoming: String, base: String): Boolean =
    incoming == base || incoming.startsWith("$base/")

// ---------------------------------------------------------------------------
// NavKey decoding
// ---------------------------------------------------------------------------

private fun <T : NavKey> decodeNavKey(
    deepLink: NavDeepLink<T>,
    queryParams: Map<String, String>,
): NavKey? =
    try {
        val jsonObject = JsonObject(queryParams.mapValues { JsonPrimitive(it.value) })
        lenientJson.decodeFromJsonElement(deepLink.serializer, jsonObject)
    } catch (_: Exception) {
        null
    }

// ---------------------------------------------------------------------------
// Query-string parsing (application/x-www-form-urlencoded)
// ---------------------------------------------------------------------------

private fun parseQueryParams(rawQuery: String): Map<String, String> {
    if (rawQuery.isBlank()) return emptyMap()
    return rawQuery.split("&").mapNotNull { param ->
        val parts = param.split("=", limit = 2)
        if (parts.size == 2) formDecode(parts[0]) to formDecode(parts[1]) else null
    }.toMap()
}

/**
 * Decodes an `application/x-www-form-urlencoded` component:
 * `+` → space, then RFC 3986 percent-decode.
 */
private fun formDecode(value: String): String =
    percentDecode(value.replace('+', ' '))

// ---------------------------------------------------------------------------
// Percent decoding (RFC 3986 §2.1) — UTF-8 aware
// ---------------------------------------------------------------------------

/**
 * Decodes percent-encoded octets per RFC 3986 §2.1. Consecutive `%XX`
 * sequences are collected into a byte buffer and decoded together as UTF-8,
 * so multibyte characters (e.g. `%C3%A9` → `é`) work correctly.
 */
private fun percentDecode(value: String): String {
    if ('%' !in value) return value

    return buildString(value.length) {
        val bytes = mutableListOf<Byte>()
        var i = 0
        while (i < value.length) {
            val ch = value[i]
            if (ch == '%' && i + 2 < value.length) {
                val code = value.substring(i + 1, i + 3).toIntOrNull(16)
                if (code != null) {
                    bytes.add(code.toByte())
                    i += 3
                    continue
                }
            }
            // Flush accumulated bytes as UTF-8 before appending a plain char
            if (bytes.isNotEmpty()) {
                append(bytes.toByteArray().decodeToString())
                bytes.clear()
            }
            append(ch)
            i++
        }
        // Flush remaining bytes
        if (bytes.isNotEmpty()) {
            append(bytes.toByteArray().decodeToString())
        }
    }
}

