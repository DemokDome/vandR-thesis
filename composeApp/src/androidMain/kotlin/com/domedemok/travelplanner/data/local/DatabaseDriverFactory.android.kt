package com.domedemok.travelplanner.data.local

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.domedemok.travelplanner.data.local.database.TravelDatabase

/**
 * Android implementation of DatabaseDriverFactory
 * Creates SQLite driver for Android platform
 */
actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = TravelDatabase.Schema,
            context = context,
            name = "travel.db"
        )
    }
}