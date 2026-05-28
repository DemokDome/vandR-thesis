package com.domedemok.travelplanner.util

/**
 * Heuristic input validator + lightweight sanitiser used for all user-typed text
 * across the app (auth, trips, places, expenses, notes).
 *
 * **Defense-in-depth, not the primary defence.** Firestore writes are already
 * parameterised on both Android and JS, so SQL injection cannot succeed against
 * the backend. The XSS strip-tag pass mainly protects against malicious text
 * being stored verbatim and rendered later in a richer surface (e.g. HTML
 * export, web view). For a real security boundary, output-escape at render time.
 */
object SecurityValidator {

    // ── Length limits ────────────────────────────────────────────────────────

    private const val MAX_NAME_LENGTH        = 50
    private const val MAX_DESCRIPTION_LENGTH = 250
    private const val MAX_NOTE_LENGTH        = 100
    private const val MIN_PASSWORD_LENGTH    = 8

    private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")

    // Catches common bypass patterns: "' OR 1=1 --", ";DROP TABLE", etc.
    // Heuristic only — Firestore parameterised writes are the real defence.
    private val SQL_INJECTION_REGEX = Regex(
        "(.*(--|;\\s*DROP|;\\s*DELETE|'\\s*OR\\s+'?1'?\\s*=\\s*'?1).*?)",
        RegexOption.IGNORE_CASE,
    )

    private val HTML_TAG_REGEX = Regex("<[^>]*>")

    // ── Sanitisation ─────────────────────────────────────────────────────────

    /**
     * Strips HTML / `<script>` tags from user input.
     *
     * Intentionally does **not** trim — Compose `TextField`s rely on trailing
     * spaces during composition (otherwise diacritic input on some IMEs breaks).
     */
    fun sanitize(input: String): String = input.replace(HTML_TAG_REGEX, "")

    /** Returns a localised threat description, or null when the input looks safe. */
    private fun checkSecurityThreats(input: String): String? =
        if (SQL_INJECTION_REGEX.matches(input))
            "Security threat detected: Malicious code (SQLi) in input."
        else null

    // ── Auth validations ─────────────────────────────────────────────────────

    fun validateEmail(email: String): String? {
        val sanitized = sanitize(email)
        if (sanitized.isBlank()) return "Email address is required."
        checkSecurityThreats(sanitized)?.let { return it }
        if (!sanitized.matches(EMAIL_REGEX)) return "Invalid email format."
        return null
    }

    /**
     * Validates password complexity. Intentionally NOT sanitised — passwords may
     * contain `<`, `>`, quotes etc., and the user's literal input is what gets
     * hashed by Firebase Auth.
     */
    fun validatePassword(password: String): String? {
        val errors = buildList {
            if (password.length < MIN_PASSWORD_LENGTH) add("• At least $MIN_PASSWORD_LENGTH characters")
            if (password.none(Char::isUpperCase))      add("• At least one uppercase letter")
            if (password.none(Char::isLowerCase))      add("• At least one lowercase letter")
            if (password.none(Char::isDigit))          add("• At least one number")
        }
        return if (errors.isEmpty()) null
        else "Password must contain:\n" + errors.joinToString("\n")
    }

    fun validateDisplayName(name: String): String? =
        validateText(name, MAX_NAME_LENGTH, blankError = "Name is required.",
            tooLongError = "Name can be at most $MAX_NAME_LENGTH characters.")

    // ── Trip / Place validations ─────────────────────────────────────────────

    fun validateTripName(name: String): String? =
        validateText(name, MAX_NAME_LENGTH, blankError = "Trip name cannot be empty.",
            tooLongError = "Trip name can be at most $MAX_NAME_LENGTH characters.")

    fun validateTripDescription(description: String): String? =
        validateText(description, MAX_DESCRIPTION_LENGTH, blankError = null,
            tooLongError = "Description can be at most $MAX_DESCRIPTION_LENGTH characters.")

    fun validateNote(note: String): String? =
        validateText(note, MAX_NOTE_LENGTH, blankError = null,
            tooLongError = "Note can be at most $MAX_NOTE_LENGTH characters.")

    // ── Expense / budget validations ─────────────────────────────────────────

    fun validateExpenseTitle(title: String): String? =
        validateText(title, MAX_NAME_LENGTH, blankError = "The name cannot be empty.",
            tooLongError = "The name can be at most $MAX_NAME_LENGTH characters.")

    fun validateExpenseAmount(amount: Double): String? =
        if (amount <= 0.0) "The amount must be greater than zero." else null

    // ── Internal helpers ─────────────────────────────────────────────────────

    /**
     * Runs the canonical sanitise → blank → length → threat pipeline.
     * Pass `blankError = null` for fields where empty input is allowed (notes, descriptions).
     */
    private fun validateText(
        input:        String,
        maxLength:    Int,
        blankError:   String?,
        tooLongError: String,
    ): String? {
        val sanitized = sanitize(input)
        if (blankError != null && sanitized.isBlank()) return blankError
        if (sanitized.length > maxLength) return tooLongError
        return checkSecurityThreats(sanitized)
    }
}
