package me.spoo.android.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.em
import coil3.compose.SubcomposeAsyncImage

/**
 * One emoji drawn with the bundled Fluent 3D artwork (microsoft/
 * fluentui-emoji, MIT), keyed by first codepoint — the alias catalogue
 * is single-codepoint by contract. Falls back to the system glyph for
 * the handful Fluent models as multi-codepoint sequences.
 */
@Composable
fun FluentEmoji(
    char: String,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val hex = char.codePointAt(0).toString(16)
    SubcomposeAsyncImage(
        model = "file:///android_asset/emoji/$hex.webp",
        contentDescription = null,
        modifier = modifier.size(size),
        error = {
            Box(Modifier.fillMaxSize()) { Text(char.emojiPresentation()) }
        },
    )
}

/**
 * Whether a codepoint has bundled Fluent artwork. Deliberately narrow: a
 * "not ASCII" test also catches the middle dot that separates every widget
 * label, whose asset lookup then fails on every single render.
 */
internal fun isEmojiCodePoint(cp: Int): Boolean =
    cp in 0x1F000..0x1FAFF ||
        cp in 0x2600..0x27BF ||
        cp in 0x2B00..0x2BFF ||
        cp in 0x2190..0x21FF

/** Variation selector 16: a presentation hint, never its own glyph. */
internal const val VARIATION_SELECTOR_16 = 0xFE0F

/**
 * Text whose emoji render as Fluent 3D inline images. Aliases are
 * emoji-only or ASCII-only (API rule), so anything non-ASCII here is an
 * emoji codepoint; plain ASCII strings take the fast path.
 */
@Composable
fun EmojiText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
) {
    if (text.codePoints().noneMatch(::isEmojiCodePoint)) {
        Text(
            text,
            modifier = modifier,
            style = style,
            color = color,
            maxLines = maxLines,
            overflow = overflow,
            softWrap = softWrap,
        )
        return
    }
    val inline = mutableMapOf<String, InlineTextContent>()
    val annotated =
        buildAnnotatedString {
            var i = 0
            while (i < text.length) {
                val cp = text.codePointAt(i)
                val count = Character.charCount(cp)
                val piece = text.substring(i, i + count)
                if (cp == VARIATION_SELECTOR_16) {
                    i += count
                    continue
                }
                if (!isEmojiCodePoint(cp)) {
                    append(piece)
                } else {
                    val id = cp.toString(16)
                    appendInlineContent(id, piece)
                    inline[id] =
                        InlineTextContent(
                            Placeholder(1.15.em, 1.15.em, PlaceholderVerticalAlign.TextCenter),
                        ) {
                            SubcomposeAsyncImage(
                                model = "file:///android_asset/emoji/$id.webp",
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                error = { Text(piece.emojiPresentation()) },
                            )
                        }
                }
                i += count
            }
        }
    Text(
        annotated,
        modifier = modifier,
        style = style,
        color = color,
        maxLines = maxLines,
        overflow = overflow,
        softWrap = softWrap,
        inlineContent = inline,
    )
}
