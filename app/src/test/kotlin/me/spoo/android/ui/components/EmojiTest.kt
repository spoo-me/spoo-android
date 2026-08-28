package me.spoo.android.ui.components

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// Widget labels join with a middle dot, so "is this ASCII?" is the wrong
// question to ask about a codepoint: it sends every separator down the
// artwork path, where the asset lookup fails on every render.
class EmojiTest {
    @Test
    fun `label separators are not treated as emoji`() {
        assertFalse(isEmojiCodePoint('·'.code))
        assertFalse(isEmojiCodePoint('/'.code))
        assertFalse(isEmojiCodePoint('A'.code))
        assertFalse(isEmojiCodePoint('9'.code))
    }

    @Test
    fun `real emoji are detected`() {
        assertTrue(isEmojiCodePoint("🎮".codePointAt(0)))
        assertTrue(isEmojiCodePoint("🔥".codePointAt(0)))
        assertTrue(isEmojiCodePoint("😎".codePointAt(0)))
        assertTrue(isEmojiCodePoint("✨".codePointAt(0)))
    }

    @Test
    fun `a plain widget label needs no artwork at all`() {
        val label = "CLICKS · 30D"
        assertTrue(label.codePoints().noneMatch(::isEmojiCodePoint))
    }
}
