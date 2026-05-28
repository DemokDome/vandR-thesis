package com.domedemok.travelplanner.data.local

import app.cash.sqldelight.db.SqlDriver
import com.domedemok.travelplanner.util.Logger

/**
 * JS implementation of [DatabaseDriverFactory].
 *
 * The web build is online-only and does not register a [TravelDatabase] in
 * Koin (`composeApp/src/jsMain/.../JsModule.kt` skips the database singleton).
 * This `actual` exists solely because the `expect class` is referenced from
 * `commonMain` Koin wiring — at runtime, [createDriver] is never invoked.
 *
 * If common-code is ever added that depends on the local DB, this throw will
 * surface the misconfiguration immediately instead of corrupting state with a
 * silent no-op driver.
 */
actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        Logger.e(
            "DatabaseDriverFactory",
            "createDriver() invoked on JS — web target has no local DB. Add a no-op " +
                "TravelDatabase shim in JsModule.kt before calling this from commonMain.",
        )
        throw UnsupportedOperationException(
            "Local database is not supported on web platform. Web uses Firebase only."
        )
    }
}
