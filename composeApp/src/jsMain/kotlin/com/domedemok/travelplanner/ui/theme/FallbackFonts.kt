package com.domedemok.travelplanner.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import com.domedemok.travelplanner.fonts.WebFontCache

/**
 * Primary text font — NotoSans Regular/Medium/Bold.
 *
 * Used by [buildTypography] as the base font for the global MaterialTheme
 * [Typography].  Script-specific fonts (Arabic, CJK, emoji, …) are NOT included
 * here; they are applied per-run by [EmojiText] via [annotateScripts].
 */
@Composable
actual fun rememberFallbackFontFamily(): FontFamily {
    return remember {
        val fonts = mutableListOf<Font>()

        WebFontCache.notoSansBytes?.let { bytes ->
            fonts += Font(identity = "NotoSans", data = bytes, weight = FontWeight.Normal)
        }
        (WebFontCache.notoSansMediumBytes ?: WebFontCache.notoSansBytes)?.let { bytes ->
            fonts += Font(identity = "NotoSans", data = bytes, weight = FontWeight.Medium)
        }
        (WebFontCache.notoSansBoldBytes ?: WebFontCache.notoSansBytes)?.let { bytes ->
            fonts += Font(identity = "NotoSans", data = bytes, weight = FontWeight.SemiBold)
            fonts += Font(identity = "NotoSans", data = bytes, weight = FontWeight.Bold)
            fonts += Font(identity = "NotoSans", data = bytes, weight = FontWeight.ExtraBold)
        }

        if (fonts.isEmpty()) FontFamily.Default else FontFamily(*fonts.toTypedArray())
    }
}

/**
 * Per-script font families for [EmojiText]'s inverted annotation strategy.
 *
 * [ScriptFonts.emoji] (NotoColorEmoji) is used as the **paragraph base font**
 * inside [EmojiText] so that supplementary-plane emoji render as font[0] — the
 * only reliable rendering path in Skia/CanvasKit.
 *
 * All other scripts ([latin], [arabic], [devanagari], [thai], [cjk]) are
 * applied as explicit [SpanStyle] overrides that shadow the NotoColorEmoji base.
 */
@Composable
actual fun rememberScriptFonts(): ScriptFonts {
    return remember {
        // Build NotoSans latin family (same data as rememberFallbackFontFamily,
        // but as a standalone single-font family for use in spans).
        val latinFonts = mutableListOf<Font>()
        WebFontCache.notoSansBytes?.let { bytes ->
            latinFonts += Font(identity = "NotoSans", data = bytes, weight = FontWeight.Normal)
        }
        (WebFontCache.notoSansMediumBytes ?: WebFontCache.notoSansBytes)?.let { bytes ->
            latinFonts += Font(identity = "NotoSans", data = bytes, weight = FontWeight.Medium)
        }
        (WebFontCache.notoSansBoldBytes ?: WebFontCache.notoSansBytes)?.let { bytes ->
            latinFonts += Font(identity = "NotoSans", data = bytes, weight = FontWeight.SemiBold)
            latinFonts += Font(identity = "NotoSans", data = bytes, weight = FontWeight.Bold)
            latinFonts += Font(identity = "NotoSans", data = bytes, weight = FontWeight.ExtraBold)
        }
        val latinFamily = if (latinFonts.isEmpty()) null
                          else FontFamily(*latinFonts.toTypedArray())

        ScriptFonts(
            latin = latinFamily,
            arabic = WebFontCache.notoArabicBytes?.let { bytes ->
                FontFamily(Font(identity = "NotoArabic", data = bytes, weight = FontWeight.Normal))
            },
            devanagari = WebFontCache.notoDevanagariBytes?.let { bytes ->
                FontFamily(Font(identity = "NotoDevanagari", data = bytes, weight = FontWeight.Normal))
            },
            thai = WebFontCache.notoThaiBytes?.let { bytes ->
                FontFamily(Font(identity = "NotoThai", data = bytes, weight = FontWeight.Normal))
            },
            cjk = WebFontCache.notoCjkBytes?.let { bytes ->
                FontFamily(Font(identity = "NotoCJK", data = bytes, weight = FontWeight.Normal))
            },
            // NotoColorEmoji: used as the paragraph base font in EmojiText so that
            // emoji render via font[0] rather than via a span (which fails for
            // surrogate pairs on Skia/CanvasKit).
            emoji = WebFontCache.notoColorEmojiBytes?.let { bytes ->
                FontFamily(Font(identity = "NotoColorEmoji", data = bytes, weight = FontWeight.Normal))
            },
        )
    }
}
