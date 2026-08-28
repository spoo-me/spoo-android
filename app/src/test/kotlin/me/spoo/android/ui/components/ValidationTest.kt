package me.spoo.android.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ValidationTest {
    @Test
    fun `normalizeUrl prepends https when no scheme was typed`() {
        assertEquals("https://example.com/promo", normalizeUrl("example.com/promo"))
        assertEquals("https://spoo.me", normalizeUrl("  spoo.me  "))
    }

    @Test
    fun `normalizeUrl leaves explicit schemes alone, typos included`() {
        assertEquals("http://example.com", normalizeUrl("http://example.com"))
        assertEquals("htp://example.com", normalizeUrl("htp://example.com"))
        assertEquals("spoo://oauth/callback", normalizeUrl("spoo://oauth/callback"))
    }

    @Test
    fun `normalizeUrl keeps empty input empty`() {
        assertEquals("", normalizeUrl("   "))
    }

    @Test
    fun `isLikelyUrl accepts hosts with a dot and localhost`() {
        assertTrue(isLikelyUrl("example.com"))
        assertTrue(isLikelyUrl("https://sub.domain.dev/path?q=1"))
        assertTrue(isLikelyUrl("http://localhost:8000/x"))
    }

    @Test
    fun `isLikelyUrl rejects non-urls`() {
        assertFalse(isLikelyUrl(""))
        assertFalse(isLikelyUrl("not a url"))
        assertFalse(isLikelyUrl("hello"))
        assertFalse(isLikelyUrl("htp://typo.example.com"))
        assertFalse(isLikelyUrl("ftp://example.com"))
    }

    @Test
    fun `password policy needs length, letter, digit, and @ or dot`() {
        assertTrue(isAcceptablePassword("word.word.42"))
        assertTrue(isAcceptablePassword("Str0ng@pass"))
        assertFalse(isAcceptablePassword("short.1"))
        assertFalse(isAcceptablePassword("no-digits.here"))
        assertFalse(isAcceptablePassword("0123456789"))
        assertFalse(isAcceptablePassword("letters4nddigits"))
    }

    @Test
    fun `alias characters are alphanumerics, underscore, hyphen`() {
        assertTrue("Fable-42_x".all(::isAliasChar))
        assertFalse(isAliasChar(' '))
        assertFalse(isAliasChar('/'))
        assertFalse(isAliasChar('é'))
    }

    @Test
    fun `suggested passwords always satisfy the policy`() {
        repeat(200) {
            val suggestion = suggestPassword()
            assertTrue(isAcceptablePassword(suggestion), "rejected: $suggestion")
            assertTrue(Regex("^[a-z]+\\.[a-z]+\\.[a-z]+\\.\\d{3}$").matches(suggestion), "shape: $suggestion")
        }
    }
}
