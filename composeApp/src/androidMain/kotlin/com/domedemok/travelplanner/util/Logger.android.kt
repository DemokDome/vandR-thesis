package com.domedemok.travelplanner.util

import android.util.Log

actual object Logger {
    actual fun e(tag: String, message: String) { Log.e(tag, message) }
    actual fun w(tag: String, message: String) { Log.w(tag, message) }
}
