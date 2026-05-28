package com.domedemok.travelplanner.ui.components

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Platform discriminator — true on JS/web, false on Android.
 * Provided by each platform's entry-point before [App] is composed.
 *
 * **Not for layout decisions.** Use [LocalLayoutMode] for "sidebar vs bottom
 * nav" and any other purely spatial choices — that one is driven by actual
 * window width, so a tablet in landscape and a desktop browser behave alike.
 *
 * Reserve this flag for genuinely platform-bound capabilities: feature
 * availability, API surface differences, anything where the platform itself
 * is the deciding factor regardless of window size.
 */
val LocalIsWeb = staticCompositionLocalOf { false }
