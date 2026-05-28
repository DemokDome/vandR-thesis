package com.domedemok.travelplanner.i18n

import androidx.compose.runtime.compositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// ── Language enum ────────────────────────────────────────────────────────────

enum class AppLanguage(val displayName: String, val code: String) {
    EN("English", "en"),
    HU("Magyar",  "hu"),
}

// ── Language manager (singleton observable) ──────────────────────────────────

/**
 * Singleton that holds the currently selected language.
 * Screens observe [language] to react to changes; the ProfileScreen
 * writes to it via [setLanguage].
 */
object LanguageManager {
    private val _language = MutableStateFlow(AppLanguage.EN)
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    fun setLanguage(lang: AppLanguage) {
        _language.value = lang
    }
}

// ── Composition local ────────────────────────────────────────────────────────

/** Returns the [Strings] instance for [language]. */
fun stringsFor(language: AppLanguage): Strings = when (language) {
    AppLanguage.EN -> EnStrings
    AppLanguage.HU -> HuStrings
}

/**
 * Provides the active [Strings] to the entire Compose tree.
 * Access from any composable with:  `val s = LocalStrings.current`
 */
val LocalStrings = compositionLocalOf<Strings> { EnStrings }
