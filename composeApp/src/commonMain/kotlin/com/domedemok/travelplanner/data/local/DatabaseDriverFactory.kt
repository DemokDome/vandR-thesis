package com.domedemok.travelplanner.data.local

import app.cash.sqldelight.db.SqlDriver

/**
 * Database Driver Factory
 *
 * This is an expect class - platform-specific implementations
 * will be provided in androidMain and jsMain/wasmJsMain
 */
expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}