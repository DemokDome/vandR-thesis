package com.domedemok.travelplanner.util

import kotlin.time.ExperimentalTime

// ── Constants ───────────────────────────────────────────────────────────────

/**
 * Custom URI scheme + host used for join-trip deep links — these MUST stay in
 * sync with the `<intent-filter>` declaration in `AndroidManifest.xml`. The
 * manifest can't reference Kotlin constants, so any change here requires a
 * matching XML edit.
 */
const val JOIN_DEEP_LINK_SCHEME = "travelplanner"
const val JOIN_DEEP_LINK_HOST   = "join"

/** Convenience prefix combining scheme + host — used by ShareTripDialog to mint URLs. */
const val JOIN_DEEP_LINK_PREFIX = "$JOIN_DEEP_LINK_SCHEME://$JOIN_DEEP_LINK_HOST/"

/** Length of a trip's six-character alphanumeric join code (e.g. `AB3K7X`). */
const val JOIN_CODE_LENGTH = 6

/**
 * Alphabet used for join codes. Visually confusable characters (`O`, `0`, `I`, `1`)
 * are excluded so users can dictate codes verbally without ambiguity.
 */
private val JOIN_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

/** Length of locally generated random IDs (e.g. for AI chat messages). */
private const val RANDOM_ID_LENGTH = 20

private val ALPHANUMERIC_CHARS = ('A'..'Z') + ('a'..'z') + ('0'..'9')

// ── Time ────────────────────────────────────────────────────────────────────

/**
 * Wall-clock time in milliseconds since the Unix epoch.
 * Centralised so platform-specific timestamp APIs don't leak into business logic
 * (Android uses `System.currentTimeMillis()`, JS uses `Date.now()` under the hood).
 */
@OptIn(ExperimentalTime::class)
fun getCurrentTimestamp(): Long =
    kotlin.time.Clock.System.now().toEpochMilliseconds()

// ── ID generation ───────────────────────────────────────────────────────────

/** Generates a 20-character alphanumeric random ID for client-side message identifiers. */
fun generateUniqueId(): String =
    (1..RANDOM_ID_LENGTH).map { ALPHANUMERIC_CHARS.random() }.joinToString("")

/**
 * Generates a fresh [JOIN_CODE_LENGTH]-character join code from [JOIN_CODE_ALPHABET].
 * Used by both Android and JS trip repositories — keep here so both platforms produce
 * codes from the same alphabet.
 */
fun generateJoinCode(): String =
    (1..JOIN_CODE_LENGTH).map { JOIN_CODE_ALPHABET.random() }.joinToString("")

// ── Join-code parsing ───────────────────────────────────────────────────────

/**
 * Extracts a clean, uppercase join code from any raw string.
 *
 * Accepts three input forms:
 *  - deep-link URL  : `"travelplanner://join/AB3K7X"`
 *  - bare QR value  : `"AB3K7X"` or noisy raw barcode text
 *  - user text      : partial / full typed code (any case, with spaces)
 *
 * Always returns up to [JOIN_CODE_LENGTH] uppercase alphanumeric characters.
 */
fun cleanJoinCode(raw: String): String {
    val withoutPrefix = if (raw.startsWith(JOIN_DEEP_LINK_PREFIX, ignoreCase = true)) {
        raw.substring(JOIN_DEEP_LINK_PREFIX.length)
    } else raw
    return withoutPrefix.uppercase()
        .filter(Char::isLetterOrDigit)
        .take(JOIN_CODE_LENGTH)
}
