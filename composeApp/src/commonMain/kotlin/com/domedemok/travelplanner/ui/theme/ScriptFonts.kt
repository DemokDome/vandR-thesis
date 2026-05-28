package com.domedemok.travelplanner.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.FontFamily

// ── Data class ────────────────────────────────────────────────────────────────

/**
 * Holds a dedicated [FontFamily] for each script that needs explicit override.
 * Null means "no override — use the paragraph's base font".
 *
 * ## Inverted strategy (JS/CanvasKit)
 *
 * Skia's per-glyph FontFamily fallback is unreliable on the JS target: only
 * `font[0]` of a multi-font family is ever used.  The correct approach is to
 * apply per-run [SpanStyle] overrides via [annotateScripts].
 *
 * However, SpanStyle font overrides fail for **supplementary-plane emoji**
 * (U+10000+, stored as surrogate pairs in UTF-16): CMP's UTF-16 → Skia index
 * conversion loses the surrogate-pair boundaries, so the emoji span is never
 * applied to the correct glyph.
 *
 * Fix — inverted strategy used by [EmojiText]:
 * - The paragraph's base font is set to [emoji] (NotoColorEmoji), so emoji
 *   code points render correctly as `font[0]` without any span.
 * - Every non-emoji run ([latin], [arabic], [devanagari], [thai], [cjk]) is
 *   explicitly tagged with a [SpanStyle] that overrides the NotoColorEmoji base.
 *
 * On Android all fields are null — the OS font stack handles everything.
 */
data class ScriptFonts(
    val latin:      FontFamily? = null,  // Latin, Cyrillic, Greek, Hebrew, IPA (U+0000–U+05FF)
    val arabic:     FontFamily? = null,  // Arabic, Persian, Urdu  (U+0600–U+06FF…)
    val devanagari: FontFamily? = null,  // Hindi, Marathi          (U+0900–U+097F)
    val thai:       FontFamily? = null,  // Thai                    (U+0E00–U+0E7F)
    val cjk:        FontFamily? = null,  // Japanese / Chinese / Korean
    val emoji:      FontFamily? = null,  // NotoColorEmoji — used as paragraph base font in EmojiText
) {
    /**
     * Returns the [FontFamily] for a span that should override the paragraph's
     * base font for [codePoint], or null to leave it to the base font.
     *
     * Supplementary-plane code points (emoji, U+10000+) always return null so
     * they fall through to the NotoColorEmoji paragraph base set by [EmojiText].
     */
    fun familyFor(codePoint: Int): FontFamily? = when {
        // Supplementary plane (emoji) — fall through to paragraph base (NotoColorEmoji)
        codePoint >= 0x10000 -> null

        // Latin, IPA, Cyrillic, Greek, Hebrew, Armenian, Georgian, etc.
        codePoint in 0x0000..0x05FF -> latin

        // Arabic script (Arabic, Extended-A, Presentation Forms A+B)
        codePoint in 0x0600..0x06FF ||
        codePoint in 0x0750..0x077F ||
        codePoint in 0xFB50..0xFDFF ||
        codePoint in 0xFE70..0xFEFF -> arabic

        // Devanagari
        codePoint in 0x0900..0x097F -> devanagari

        // Thai
        codePoint in 0x0E00..0x0E7F -> thai

        // CJK: Hiragana, Katakana, CJK Unified Ideographs (BMP + Extension A),
        //      CJK Compatibility Ideographs, Hangul syllables
        codePoint in 0x3040..0x30FF ||
        codePoint in 0x4E00..0x9FFF ||
        codePoint in 0x3400..0x4DBF ||
        codePoint in 0xF900..0xFAFF ||
        codePoint in 0xAC00..0xD7AF -> cjk

        else -> null
    }
}

// ── CompositionLocal ──────────────────────────────────────────────────────────

/** App-wide script fonts; provided by [TravelPlannerTheme]. */
val LocalScriptFonts = staticCompositionLocalOf { ScriptFonts() }

// ── Expect ────────────────────────────────────────────────────────────────────

/**
 * Builds the per-script [ScriptFonts] for the current platform.
 *
 * Android: returns the empty default — the OS font stack handles every script.
 * JS/web: builds a dedicated [FontFamily] for each script from the bytes
 *         pre-loaded by [com.domedemok.travelplanner.fonts.WebFontCache].
 */
@Composable
expect fun rememberScriptFonts(): ScriptFonts
