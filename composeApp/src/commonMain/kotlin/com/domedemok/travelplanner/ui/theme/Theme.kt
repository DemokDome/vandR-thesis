package com.domedemok.travelplanner.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily

private val DarkColorScheme = darkColorScheme(
    primary              = dark_primary,
    onPrimary            = dark_onPrimary,
    primaryContainer     = dark_primaryContainer,
    onPrimaryContainer   = dark_onPrimaryContainer,
    secondary            = dark_secondary,
    onSecondary          = dark_onSecondary,
    secondaryContainer   = dark_secondaryContainer,
    onSecondaryContainer = dark_onSecondaryContainer,
    background           = dark_background,
    onBackground         = dark_onBackground,
    surface              = dark_surface,
    onSurface            = dark_onSurface,
    surfaceVariant       = dark_surfaceVariant,
    onSurfaceVariant     = dark_onSurfaceVariant,
    outline              = dark_outline,
    error                = dark_error,
    onError              = dark_onError,
    errorContainer       = dark_errorContainer,
    onErrorContainer     = dark_onErrorContainer,
    inverseSurface       = dark_inverseSurface,
    inverseOnSurface     = dark_inverseOnSurface,
)

private val LightColorScheme = lightColorScheme(
    primary              = light_primary,
    onPrimary            = light_onPrimary,
    primaryContainer     = light_primaryContainer,
    onPrimaryContainer   = light_onPrimaryContainer,
    secondary            = light_secondary,
    onSecondary          = light_onSecondary,
    secondaryContainer   = light_secondaryContainer,
    onSecondaryContainer = light_onSecondaryContainer,
    background           = light_background,
    onBackground         = light_onBackground,
    surface              = light_surface,
    onSurface            = light_onSurface,
    surfaceVariant       = light_surfaceVariant,
    onSurfaceVariant     = light_onSurfaceVariant,
    outline              = light_outline,
    error                = light_error,
    onError              = light_onError,
    errorContainer       = light_errorContainer,
    onErrorContainer     = light_onErrorContainer,
    inverseSurface       = light_inverseSurface,
    inverseOnSurface     = light_inverseOnSurface,
)

@Composable
fun TravelPlannerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // On Android FontFamily.Default is returned — the OS font stack handles all scripts.
    // On web (Skia/CanvasKit) per-script FontFamilies are built from bytes pre-loaded by
    // WebFontCache and applied as explicit SpanStyles via annotateScripts, bypassing
    // Skia's unreliable per-glyph FontFamily fallback on the JS target.
    val fallback    = rememberFallbackFontFamily()
    val scriptFonts = rememberScriptFonts()
    // Typography is keyed on fallback — stable on Android (always Default),
    // recomputed once after fonts load on web.
    val typography  = remember(fallback) { buildTypography(fallback) }

    CompositionLocalProvider(LocalScriptFonts provides scriptFonts) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography  = typography,
            content     = content,
        )
    }
}

// ---------------------------------------------------------------------------
// Popup / modal theme re-propagation
// ---------------------------------------------------------------------------

/**
 * Returns `true` when the app should render in dark mode, reading from
 * [ThemeManager] directly so it works in any composition context —
 * including inside [AlertDialog] / [ModalBottomSheet] popups on CMP web
 * where the parent composition tree may not be automatically inherited.
 */
@Composable
fun rememberCurrentDarkTheme(): Boolean {
    val themeMode by ThemeManager.themeMode.collectAsState()
    return when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT  -> false
        ThemeMode.DARK   -> true
    }
}

/**
 * Wraps [content] in [TravelPlannerTheme] using the current app theme mode.
 *
 * On CMP JS/WASM, `Dialog` and `ModalBottomSheet` create a popup whose
 * composition context may be isolated from the parent tree, meaning
 * [MaterialTheme] colors might revert to M3 defaults (light mode) inside
 * those overlays.  Placing `PopupThemeProvider { … }` as the first child
 * inside any popup content lambda guarantees the correct dark/light scheme
 * is always applied.
 */
@Composable
fun PopupThemeProvider(content: @Composable () -> Unit) {
    TravelPlannerTheme(darkTheme = rememberCurrentDarkTheme(), content = content)
}

// ---------------------------------------------------------------------------
// Typography helpers
// ---------------------------------------------------------------------------

/**
 * Copies the Material3 default [Typography] with [fontFamily] injected into
 * every style, preserving M3's carefully tuned sizes, weights, and spacing.
 *
 * When [fontFamily] is [FontFamily.Default] (Android) the unmodified M3
 * defaults are returned directly — zero overhead.
 */
private fun buildTypography(fontFamily: FontFamily): Typography {
    if (fontFamily == FontFamily.Default) return Typography()
    val d = Typography()
    return Typography(
        displayLarge   = d.displayLarge.copy(fontFamily   = fontFamily),
        displayMedium  = d.displayMedium.copy(fontFamily  = fontFamily),
        displaySmall   = d.displaySmall.copy(fontFamily   = fontFamily),
        headlineLarge  = d.headlineLarge.copy(fontFamily  = fontFamily),
        headlineMedium = d.headlineMedium.copy(fontFamily = fontFamily),
        headlineSmall  = d.headlineSmall.copy(fontFamily  = fontFamily),
        titleLarge     = d.titleLarge.copy(fontFamily     = fontFamily),
        titleMedium    = d.titleMedium.copy(fontFamily    = fontFamily),
        titleSmall     = d.titleSmall.copy(fontFamily     = fontFamily),
        bodyLarge      = d.bodyLarge.copy(fontFamily      = fontFamily),
        bodyMedium     = d.bodyMedium.copy(fontFamily     = fontFamily),
        bodySmall      = d.bodySmall.copy(fontFamily      = fontFamily),
        labelLarge     = d.labelLarge.copy(fontFamily     = fontFamily),
        labelMedium    = d.labelMedium.copy(fontFamily    = fontFamily),
        labelSmall     = d.labelSmall.copy(fontFamily     = fontFamily),
    )
}