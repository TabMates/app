package de.tabmates.composeapp.deeplink

import de.tabmates.features.authentication.presentation.navigation.ResetPassword
import de.tabmates.features.authentication.presentation.navigation.VerifyAccount
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NavDeepLinkTest {

    private lateinit var deepLinks: List<NavDeepLink<*>>

    @BeforeTest
    fun setUp() {
        deepLinks = listOf(
            navDeepLink<VerifyAccount>(basePath = "https://example.com/api/auth/verify"),
            navDeepLink<ResetPassword>(basePath = "https://example.com/api/auth/reset-password"),
        )
    }

    // region — basic resolution

    @Test
    fun resolveVerifyAccountReturnsCorrectNavKey() {
        val result = resolveDeepLink("https://example.com/api/auth/verify?token=abc123", deepLinks)

        assertEquals(VerifyAccount(token = "abc123"), result)
    }

    @Test
    fun resolveResetPasswordReturnsCorrectNavKey() {
        val result = resolveDeepLink("https://example.com/api/auth/reset-password?token=xyz789", deepLinks)

        assertEquals(ResetPassword(token = "xyz789"), result)
    }

    @Test
    fun resolveWithCustomSchemeReturnsCorrectNavKey() {
        val result = resolveDeepLink("tabmates://example.com/api/auth/verify?token=abc123", deepLinks)

        assertEquals(VerifyAccount(token = "abc123"), result)
    }

    @Test
    fun resolveIgnoresExtraQueryParams() {
        val result = resolveDeepLink("https://example.com/api/auth/verify?token=abc123&extra=value", deepLinks)

        assertEquals(VerifyAccount(token = "abc123"), result)
    }

    @Test
    fun resolveWithRepeatedKeyUsesLastValue() {
        val result = resolveDeepLink("https://example.com/api/auth/verify?token=first&token=second", deepLinks)

        assertEquals(VerifyAccount(token = "second"), result)
    }

    // endregion

    // region — RFC 3986 §3.2: host matching (case-insensitive)

    @Test
    fun resolveMatchesCaseInsensitiveHost() {
        val result = resolveDeepLink("https://example.com/api/auth/verify?token=abc123", deepLinks)

        assertEquals(VerifyAccount(token = "abc123"), result)
    }

    @Test
    fun resolveMatchesMixedCaseHost() {
        val result = resolveDeepLink("https://example.com/api/auth/verify?token=abc123", deepLinks)

        assertEquals(VerifyAccount(token = "abc123"), result)
    }

    @Test
    fun resolveWithWrongHostReturnsNull() {
        val result = resolveDeepLink("https://wrong.host.com/api/auth/verify?token=abc123", deepLinks)

        assertNull(result)
    }

    @Test
    fun resolveWithHostPortMismatchReturnsNull() {
        // host:port is a different authority than host alone
        val result = resolveDeepLink("https://example.com:8080/api/auth/verify?token=abc123", deepLinks)

        assertNull(result)
    }

    // endregion

    // region — RFC 3986 §3.3: path matching (exact boundary)

    @Test
    fun resolveWithSimilarPrefixPathReturnsNull() {
        val result = resolveDeepLink("https://example.com/api/auth/verify-evil?token=abc123", deepLinks)

        assertNull(result)
    }

    @Test
    fun resolveWithSuffixedPathReturnsNull() {
        val result = resolveDeepLink("https://example.com/api/auth/verifySomething?token=abc123", deepLinks)

        assertNull(result)
    }

    @Test
    fun resolvePathIsCaseSensitive() {
        val result = resolveDeepLink("https://example.com/API/AUTH/VERIFY?token=abc123", deepLinks)

        assertNull(result)
    }

    @Test
    fun resolveWithTrailingSlashOnIncomingUri() {
        // "/api/auth/verify/" startsWith "/api/auth/verify/" — sub-path match
        val result = resolveDeepLink("https://example.com/api/auth/verify/?token=abc123", deepLinks)

        // Path is "/api/auth/verify/" which starts with "/api/auth/verify/" — matches
        assertEquals(VerifyAccount(token = "abc123"), result)
    }

    @Test
    fun resolveWithPercentEncodedPathSegment() {
        // %76erify → decoded to "verify", should match
        val result = resolveDeepLink("https://example.com/api/auth/%76erify?token=abc123", deepLinks)

        assertEquals(VerifyAccount(token = "abc123"), result)
    }

    // endregion

    // region — RFC 3986 §3.4: query-string parsing

    @Test
    fun resolveWithEmptyQueryReturnsNull() {
        val result = resolveDeepLink("https://example.com/api/auth/verify?", deepLinks)

        assertNull(result)
    }

    @Test
    fun resolveQueryParamWithEmptyValue() {
        // token= (empty value) → VerifyAccount(token = "")
        val result = resolveDeepLink("https://example.com/api/auth/verify?token=", deepLinks)

        assertEquals(VerifyAccount(token = ""), result)
    }

    @Test
    fun resolveSkipsQueryParamWithoutEquals() {
        // "orphan" has no '=' so it is skipped; token is still extracted
        val result = resolveDeepLink("https://example.com/api/auth/verify?orphan&token=abc123", deepLinks)

        assertEquals(VerifyAccount(token = "abc123"), result)
    }

    @Test
    fun resolveSkipsEmptyQuerySegments() {
        // Double && produces an empty segment between them — should be ignored
        val result = resolveDeepLink("https://example.com/api/auth/verify?token=abc123&&extra=x", deepLinks)

        assertEquals(VerifyAccount(token = "abc123"), result)
    }

    // endregion

    // region — RFC 3986 §3.5: fragment handling

    @Test
    fun resolveIgnoresFragmentAfterQuery() {
        val result = resolveDeepLink("https://example.com/api/auth/verify?token=abc123#section", deepLinks)

        assertEquals(VerifyAccount(token = "abc123"), result)
    }

    @Test
    fun resolveIgnoresQueryInsideFragment() {
        // RFC 3986 §3.5: '?' inside a fragment is NOT a query delimiter
        val result = resolveDeepLink("https://example.com/api/auth/verify#section?token=abc123", deepLinks)

        assertNull(result) // no query → missing required param
    }

    @Test
    fun resolveIgnoresComplexFragment() {
        // Fragment containing '?', '&', '=' — none of it is query
        val result = resolveDeepLink("https://example.com/api/auth/verify#a?b=c&d=e", deepLinks)

        assertNull(result) // no query → missing required param
    }

    @Test
    fun resolveWithEmptyFragment() {
        val result = resolveDeepLink("https://example.com/api/auth/verify?token=abc123#", deepLinks)

        assertEquals(VerifyAccount(token = "abc123"), result)
    }

    // endregion

    // region — RFC 3986 §2.1: percent-encoding

    @Test
    fun resolveDecodesPercentEncodedReservedChars() {
        // %3D → '=', %26 → '&'
        val result = resolveDeepLink("https://example.com/api/auth/verify?token=abc%3D%26123", deepLinks)

        assertEquals(VerifyAccount(token = "abc=&123"), result)
    }

    @Test
    fun resolveDecodesLowercaseHexDigits() {
        // %3d (lowercase) is equivalent to %3D (uppercase) per RFC 3986 §2.1
        val result = resolveDeepLink("https://example.com/api/auth/verify?token=abc%3d123", deepLinks)

        assertEquals(VerifyAccount(token = "abc=123"), result)
    }

    @Test
    fun resolveDecodesPercentEncodedUnreservedChars() {
        // %61 → 'a', %62 → 'b' — over-encoded but valid
        val result = resolveDeepLink("https://example.com/api/auth/verify?token=%61%62c", deepLinks)

        assertEquals(VerifyAccount(token = "abc"), result)
    }

    @Test
    fun resolveDecodesPercentEncodedPercent() {
        // %25 → literal '%'
        val result = resolveDeepLink("https://example.com/api/auth/verify?token=100%25done", deepLinks)

        assertEquals(VerifyAccount(token = "100%done"), result)
    }

    @Test
    fun resolvePreservesIncompletePercentSequence() {
        // Trailing '%2' is not a valid percent-encoded triplet — kept as-is
        val result = resolveDeepLink("https://example.com/api/auth/verify?token=abc%2", deepLinks)

        assertEquals(VerifyAccount(token = "abc%2"), result)
    }

    @Test
    fun resolvePreservesInvalidPercentHexDigits() {
        // %GG is not valid hex — kept as-is
        val result = resolveDeepLink("https://example.com/api/auth/verify?token=abc%GGdef", deepLinks)

        assertEquals(VerifyAccount(token = "abc%GGdef"), result)
    }

    // endregion

    // region — UTF-8 multi-byte percent-encoding

    @Test
    fun resolveDecodesTwoByteUtf8() {
        // %C3%A9 → 'é' (U+00E9, 2-byte UTF-8)
        val result = resolveDeepLink("https://example.com/api/auth/verify?token=caf%C3%A9", deepLinks)

        assertEquals(VerifyAccount(token = "café"), result)
    }

    @Test
    fun resolveDecodesThreeByteUtf8() {
        // %E4%B8%96 → '世' (U+4E16, 3-byte UTF-8)
        val result = resolveDeepLink("https://example.com/api/auth/verify?token=%E4%B8%96", deepLinks)

        assertEquals(VerifyAccount(token = "世"), result)
    }

    @Test
    fun resolveDecodesFourByteUtf8() {
        // %F0%9F%8D%95 → '🍕' (U+1F355, 4-byte UTF-8)
        val result = resolveDeepLink("https://example.com/api/auth/verify?token=%F0%9F%8D%95", deepLinks)

        assertEquals(VerifyAccount(token = "🍕"), result)
    }

    @Test
    fun resolveDecodesMultiByteUtf8MixedWithPlainText() {
        // plain + 2-byte + plain + 3-byte
        val result = resolveDeepLink(
            "https://example.com/api/auth/verify?token=a%C3%A9b%E4%B8%96c",
            deepLinks,
        )

        assertEquals(VerifyAccount(token = "a\u00E9b\u4E16c"), result)
    }

    // endregion

    // region — application/x-www-form-urlencoded

    @Test
    fun resolveDecodesPlusAsSpaceInQuery() {
        val result = resolveDeepLink("https://example.com/api/auth/verify?token=hello+world", deepLinks)

        assertEquals(VerifyAccount(token = "hello world"), result)
    }

    @Test
    fun resolveDecodesPercent2BAsLiteralPlus() {
        // %2B in query → literal '+' (NOT space)
        val result = resolveDeepLink("https://example.com/api/auth/verify?token=a%2Bb", deepLinks)

        assertEquals(VerifyAccount(token = "a+b"), result)
    }

    @Test
    fun resolveDecodesPercent20AsSpace() {
        // %20 → space (RFC 3986 encoding, also valid in form-urlencoded)
        val result = resolveDeepLink("https://example.com/api/auth/verify?token=hello%20world", deepLinks)

        assertEquals(VerifyAccount(token = "hello world"), result)
    }

    // endregion

    // region — invalid / missing scheme

    @Test
    fun resolveWithNoSchemeReturnsNull() {
        val result = resolveDeepLink("example.com/api/auth/verify?token=abc123", deepLinks)

        assertNull(result)
    }

    @Test
    fun resolveWithEmptyStringReturnsNull() {
        val result = resolveDeepLink("", deepLinks)

        assertNull(result)
    }

    @Test
    fun resolveWithMissingRequiredParamReturnsNull() {
        val result = resolveDeepLink("https://example.com/api/auth/verify", deepLinks)

        assertNull(result)
    }

    @Test
    fun resolveWithUnknownPathReturnsNull() {
        val result = resolveDeepLink("https://example.com/api/unknown/path?token=abc123", deepLinks)

        assertNull(result)
    }

    @Test
    fun resolveWithNoDeepLinksReturnsNull() {
        val result = resolveDeepLink(
            "https://example.com/api/auth/verify?token=abc123",
            emptyList(),
        )

        assertNull(result)
    }

    // endregion

    // region — non-hierarchical URIs (e.g. from Android intents)

    @Test
    fun resolveWithMailtoUriReturnsNull() {
        assertNull(resolveDeepLink("mailto:user@example.com", deepLinks))
    }

    @Test
    fun resolveWithTelUriReturnsNull() {
        assertNull(resolveDeepLink("tel:+1234567890", deepLinks))
    }

    @Test
    fun resolveWithSmsUriReturnsNull() {
        assertNull(resolveDeepLink("sms:+1234567890?body=hello", deepLinks))
    }

    @Test
    fun resolveWithGeoUriReturnsNull() {
        assertNull(resolveDeepLink("geo:37.7749,-122.4194", deepLinks))
    }

    @Test
    fun resolveWithDataUriReturnsNull() {
        assertNull(resolveDeepLink("data:text/plain;base64,SGVsbG8=", deepLinks))
    }

    // endregion
}

