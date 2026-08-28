package me.spoo.android.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import me.spoo.SpooDecodeException
import me.spoo.SpooIOException

// SDK exceptions with internal constructors (validation, rate limit, ...)
// are exercised end to end against the live backend instead; these cover
// the publicly constructible paths and the fallback contract.
class ErrorsTest {
    @Test
    fun `io failures become a connectivity sentence`() {
        val friendly = friendlyError(SpooIOException("timeout", null), "fallback")
        assertEquals("Can't reach spoo.me. Check your connection and try again.", friendly.message)
        assertNull(friendly.field)
    }

    @Test
    fun `decode failures suggest updating the app`() {
        val friendly = friendlyError(SpooDecodeException("bad json", null), "fallback")
        assertEquals("Unexpected response from the server. Try updating the app.", friendly.message)
    }

    @Test
    fun `unknown failures fall back verbatim`() {
        val friendly = friendlyError(IllegalStateException("boom"), "Couldn't create the link.")
        assertEquals("Couldn't create the link.", friendly.message)
        assertNull(friendly.field)
    }
}
