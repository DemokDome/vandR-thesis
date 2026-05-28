package com.domedemok.travelplanner.util

/**
 * Minimal platform-aware logging facade.
 *
 * - Android → [android.util.Log]
 * - JS      → `console.error`
 *
 * Only error-level logging is exposed for now; extend as needed.
 */
expect object Logger {
    fun e(tag: String, message: String)
    fun w(tag: String, message: String)
}
