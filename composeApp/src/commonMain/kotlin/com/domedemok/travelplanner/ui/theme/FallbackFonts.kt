package com.domedemok.travelplanner.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily

/**
 * Returns a [FontFamily] for the primary UI text (Latin, Cyrillic, Greek,
 * script supplements, CJK).  Does NOT include emoji — those are handled
 * via [rememberScriptFonts] and applied via [annotateScripts].
 *
 * On Android [FontFamily.Default] is returned; the system handles everything.
 * On web (Skia/CanvasKit) the family is built from bytes loaded by WebFontCache.
 */
@Composable
expect fun rememberFallbackFontFamily(): FontFamily
