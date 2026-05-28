package com.domedemok.travelplanner.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import com.domedemok.travelplanner.ui.theme.LocalScriptFonts
import com.domedemok.travelplanner.ui.theme.ScriptFonts

// ── Core utility ──────────────────────────────────────────────────────────────

/**
 * Annotates each non-emoji script run with the appropriate [FontFamily] from
 * [fonts] via [SpanStyle], so Skia/CanvasKit renders Latin, CJK, Arabic, and
 * Thai correctly.
 *
 * Emoji (U+10000+, stored as surrogate pairs) are intentionally **not** tagged
 * with a span.  [EmojiText] sets NotoColorEmoji as the paragraph's base font,
 * so untagged characters fall through to it.  This is the only reliable emoji
 * rendering path in Skia/CanvasKit: SpanStyle font overrides fail for surrogate
 * pairs because CMP's UTF-16 → Skia index conversion loses pair boundaries.
 *
 * Consecutive code points that share the same family are coalesced into one
 * span to minimise [AnnotatedString] overhead.
 *
 * Fast paths:
 *  - All [fonts] fields null (Android) → returns a plain [AnnotatedString].
 *  - Pure Latin/IPA/Cyrillic/Greek/Hebrew (no surrogates, all chars ≤ U+05FF)
 *    → wraps the entire string in a single [fonts.latin] span (O(1)).
 */
fun String.annotateScripts(fonts: ScriptFonts): AnnotatedString {
    // Android fast path: all null → OS handles everything, no spans needed
    if (fonts.latin == null && fonts.arabic == null && fonts.devanagari == null
        && fonts.thai == null && fonts.cjk == null && fonts.emoji == null) {
        return AnnotatedString(this)
    }

    // Pure-Latin fast path: wrap the whole string in one NotoSans span
    if (fonts.latin != null && none { it.isHighSurrogate() || it.code > 0x05FF }) {
        return buildAnnotatedString {
            withStyle(SpanStyle(fontFamily = fonts.latin)) { append(this@annotateScripts) }
        }
    }

    return buildAnnotatedString {
        var i        = 0
        var runStart = 0
        var runFamily: FontFamily? = null

        while (i < this@annotateScripts.length) {
            val c = this@annotateScripts[i]
            val codePoint: Int
            val charCount: Int
            if (c.isHighSurrogate() && i + 1 < this@annotateScripts.length
                && this@annotateScripts[i + 1].isLowSurrogate()) {
                // Surrogate pair (emoji, U+10000+) — familyFor returns null → no span
                codePoint = 0x10000 + (c.code - 0xD800) * 0x400 + (this@annotateScripts[i + 1].code - 0xDC00)
                charCount = 2
            } else {
                codePoint = c.code
                charCount = 1
            }

            val family = fonts.familyFor(codePoint)
            if (family != runFamily) {
                // Flush the current run [runStart, i)
                if (runStart < i) {
                    val slice = this@annotateScripts.substring(runStart, i)
                    if (runFamily != null) {
                        withStyle(SpanStyle(fontFamily = runFamily)) { append(slice) }
                    } else {
                        append(slice)
                    }
                }
                runStart  = i
                runFamily = family
            }
            i += charCount
        }

        // Flush the final run [runStart, length)
        if (runStart < this@annotateScripts.length) {
            val slice = this@annotateScripts.substring(runStart)
            if (runFamily != null) {
                withStyle(SpanStyle(fontFamily = runFamily)) { append(slice) }
            } else {
                append(slice)
            }
        }
    }
}

// ── Drop-in Text wrapper ───────────────────────────────────────────────────────

/**
 * Drop-in replacement for [androidx.compose.material3.Text] that renders emoji
 * and all non-Latin scripts correctly on every platform.
 *
 * **On Android** no spans are added — the OS font stack handles everything.
 *
 * **On web (JS/CanvasKit)**:
 *  - The paragraph's base font is overridden to NotoColorEmoji, so that
 *    supplementary-plane emoji (which cannot be targeted via SpanStyle on
 *    Skia/CanvasKit) render as the paragraph's `font[0]`.
 *  - Latin, CJK, Arabic, Devanagari, and Thai runs are tagged with explicit
 *    [SpanStyle] overrides that shadow the NotoColorEmoji base, preserving
 *    correct glyph metrics for all non-emoji text.
 *
 * Use this wherever text may contain emoji, CJK, Arabic, Devanagari, or Thai —
 * in particular chat messages, place names, AI responses, and itinerary items.
 */
@Composable
fun EmojiText(
    text:       String,
    modifier:   Modifier     = Modifier,
    color:      Color        = Color.Unspecified,
    fontSize:   TextUnit     = TextUnit.Unspecified,
    fontWeight: FontWeight?  = null,
    lineHeight: TextUnit     = TextUnit.Unspecified,
    maxLines:   Int          = Int.MAX_VALUE,
    overflow:   TextOverflow = TextOverflow.Clip,
    style:      TextStyle    = LocalTextStyle.current,
) {
    val scriptFonts = LocalScriptFonts.current
    val annotated   = text.annotateScripts(scriptFonts)

    // Set NotoColorEmoji as the paragraph base font so that untagged emoji
    // characters render via font[0].  Non-emoji runs are overridden by spans
    // from annotateScripts, so this base font only affects actual emoji.
    // On Android scriptFonts.emoji is null → style is unchanged.
    val effectiveStyle = if (scriptFonts.emoji != null) {
        style.copy(fontFamily = scriptFonts.emoji)
    } else {
        style
    }

    androidx.compose.material3.Text(
        text       = annotated,
        modifier   = modifier,
        color      = color,
        fontSize   = fontSize,
        fontWeight = fontWeight,
        lineHeight = lineHeight,
        maxLines   = maxLines,
        overflow   = overflow,
        style      = effectiveStyle,
    )
}
