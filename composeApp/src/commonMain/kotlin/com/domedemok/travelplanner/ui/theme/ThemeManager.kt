package com.domedemok.travelplanner.ui.theme

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The three possible theme modes the user can pick.
 *
 * [SYSTEM] — follow the OS dark/light setting (default).
 * [LIGHT]  — always light, regardless of system setting.
 * [DARK]   — always dark, regardless of system setting.
 */
enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

/**
 * Singleton that holds the currently selected [ThemeMode].
 * Screens observe [themeMode] to react to changes; the ProfileScreen
 * writes to it via [setThemeMode].
 *
 * Follows the same pattern as [com.domedemok.travelplanner.i18n.LanguageManager].
 */
object ThemeManager {
    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        if (_themeMode.value != mode) _themeMode.value = mode
    }
}
