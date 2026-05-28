package com.domedemok.travelplanner.data.repository.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class AuthBusinessRulesTest {

    // ── resolveDisplayName ──────────────────────────────────────────────────

    @Test
    fun `profile name wins over auth name when both present`() {
        assertEquals(
            "Alice (renamed)",
            AuthBusinessRules.resolveDisplayName("Alice (renamed)", "Alice"),
        )
    }

    @Test
    fun `auth name used when profile name is null`() {
        assertEquals("Alice", AuthBusinessRules.resolveDisplayName(null, "Alice"))
    }

    @Test
    fun `auth name used when profile name is blank`() {
        assertEquals("Alice", AuthBusinessRules.resolveDisplayName("", "Alice"))
    }

    @Test
    fun `empty string when both inputs are blank or null`() {
        assertEquals("", AuthBusinessRules.resolveDisplayName(null, null))
        assertEquals("", AuthBusinessRules.resolveDisplayName("", ""))
        assertEquals("", AuthBusinessRules.resolveDisplayName(null, ""))
    }

    // ── resolveCreatedAt ────────────────────────────────────────────────────

    @Test
    fun `profile createdAt wins when positive`() {
        assertEquals(1_700_000_000_000L, AuthBusinessRules.resolveCreatedAt(1_700_000_000_000L, 999L))
    }

    @Test
    fun `nowMs used when profile createdAt is null`() {
        assertEquals(999L, AuthBusinessRules.resolveCreatedAt(null, 999L))
    }

    @Test
    fun `nowMs used when profile createdAt is zero (sentinel for unknown)`() {
        assertEquals(999L, AuthBusinessRules.resolveCreatedAt(0L, 999L))
    }

    // ── buildUser ───────────────────────────────────────────────────────────

    @Test
    fun `buildUser merges auth identity with profile overlay`() {
        val user = AuthBusinessRules.buildUser(
            uid                = "u-1",
            authEmail          = "a@b.com",
            authDisplayName    = "Alice",
            profileDisplayName = "Alice Renamed",
            profileCreatedAt   = 1_700L,
            nowMs              = 9_999L,
        )
        assertEquals("u-1", user.id)
        assertEquals("a@b.com", user.email)
        assertEquals("Alice Renamed", user.displayName)
        assertEquals(1_700L, user.createdAt)
    }

    @Test
    fun `buildUser falls back through every field when profile is absent`() {
        val user = AuthBusinessRules.buildUser(
            uid                = "u-1",
            authEmail          = null,
            authDisplayName    = null,
            profileDisplayName = null,
            profileCreatedAt   = null,
            nowMs              = 5_000L,
        )
        assertEquals("", user.email)
        assertEquals("", user.displayName)
        assertEquals(5_000L, user.createdAt)
    }

    // ── buildProvisionalUser ────────────────────────────────────────────────

    @Test
    fun `provisional user always has createdAt zero sentinel`() {
        val user = AuthBusinessRules.buildProvisionalUser("u-1", "a@b.com", "Alice")
        assertEquals(0L, user.createdAt)
        assertEquals("Alice", user.displayName)
    }

    // ── buildSignUpProfilePayload ───────────────────────────────────────────

    @Test
    fun `signup payload contains the three mirrored profile fields`() {
        val user = com.domedemok.travelplanner.data.model.User(
            id          = "u-1",
            email       = "a@b.com",
            displayName = "Alice",
            createdAt   = 1234L,
        )
        val payload = AuthBusinessRules.buildSignUpProfilePayload(user)
        assertEquals(setOf("email", "displayName", "createdAt"), payload.keys)
        assertEquals("a@b.com", payload["email"])
        assertEquals("Alice", payload["displayName"])
        assertEquals(1234L, payload["createdAt"])
    }
}
