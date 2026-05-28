package com.domedemok.travelplanner.util

actual object Logger {
    actual fun e(tag: String, message: String) { console.error("[$tag] $message") }
    actual fun w(tag: String, message: String) { console.warn("[$tag] $message") }
}
