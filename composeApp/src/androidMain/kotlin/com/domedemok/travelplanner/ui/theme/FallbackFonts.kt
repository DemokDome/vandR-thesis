package com.domedemok.travelplanner.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily

@Composable
actual fun rememberFallbackFontFamily(): FontFamily = FontFamily.Default

@Composable
actual fun rememberScriptFonts(): ScriptFonts = ScriptFonts()
