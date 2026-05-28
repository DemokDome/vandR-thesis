package com.domedemok.travelplanner.data.repository.rules

import com.domedemok.travelplanner.data.repository.rules.TripBusinessRules.JoinDecision
import com.domedemok.travelplanner.data.repository.rules.TripBusinessRules.RemoveMemberDecision
import com.domedemok.travelplanner.data.repository.rules.TripBusinessRules.ShareDecision
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class TripBusinessRulesTest {

    // ── evaluateJoinAttempt ──────────────────────────────────────────────────

    @Test
    fun `join attempt by owner returns AlreadyMember`() {
        val result = TripBusinessRules.evaluateJoinAttempt(
            tripCreatorId     = "owner-1",
            existingMemberIds = setOf("owner-1"),
            joinerId          = "owner-1",
            joinerName        = "Owner",
        )
        assertEquals(JoinDecision.AlreadyMember, result)
    }

    @Test
    fun `join attempt by existing member returns AlreadyMember`() {
        val result = TripBusinessRules.evaluateJoinAttempt(
            tripCreatorId     = "owner-1",
            existingMemberIds = setOf("owner-1", "member-2"),
            joinerId          = "member-2",
            joinerName        = "Bob",
        )
        assertEquals(JoinDecision.AlreadyMember, result)
    }

    @Test
    fun `join attempt by new user returns Allowed with joiner data`() {
        val result = TripBusinessRules.evaluateJoinAttempt(
            tripCreatorId     = "owner-1",
            existingMemberIds = setOf("owner-1"),
            joinerId          = "newcomer-3",
            joinerName        = "Cara",
        )
        val allowed = assertIs<JoinDecision.Allowed>(result)
        assertEquals("newcomer-3", allowed.joinerId)
        assertEquals("Cara", allowed.joinerName)
    }

    // ── evaluateShareAttempt ─────────────────────────────────────────────────

    @Test
    fun `share to self returns SelfShare`() {
        val result = TripBusinessRules.evaluateShareAttempt(
            sharerUserId      = "user-1",
            targetUserId      = "user-1",
            existingMemberIds = setOf("user-1"),
            targetUserName    = "Me",
        )
        assertEquals(ShareDecision.SelfShare, result)
    }

    @Test
    fun `share to already-member returns AlreadyShared`() {
        val result = TripBusinessRules.evaluateShareAttempt(
            sharerUserId      = "user-1",
            targetUserId      = "user-2",
            existingMemberIds = setOf("user-1", "user-2"),
            targetUserName    = "Bob",
        )
        assertEquals(ShareDecision.AlreadyShared, result)
    }

    @Test
    fun `share to new user returns Allowed with target data`() {
        val result = TripBusinessRules.evaluateShareAttempt(
            sharerUserId      = "user-1",
            targetUserId      = "user-2",
            existingMemberIds = setOf("user-1"),
            targetUserName    = "Bob",
        )
        val allowed = assertIs<ShareDecision.Allowed>(result)
        assertEquals("user-2", allowed.targetUserId)
        assertEquals("Bob", allowed.targetUserName)
    }

    // ── evaluateRemoveMember ────────────────────────────────────────────────

    @Test
    fun `non-owner attempting removal is rejected`() {
        val result = TripBusinessRules.evaluateRemoveMember(
            removerId        = "member-2",
            tripCreatorId    = "owner-1",
            existingMembers  = mapOf("owner-1" to "Owner", "member-2" to "Bob"),
            memberIdToRemove = "owner-1",
        )
        assertEquals(RemoveMemberDecision.NotOwner, result)
    }

    @Test
    fun `owner removing existing member preserves their display name`() {
        val result = TripBusinessRules.evaluateRemoveMember(
            removerId        = "owner-1",
            tripCreatorId    = "owner-1",
            existingMembers  = mapOf("owner-1" to "Owner", "member-2" to "Bob"),
            memberIdToRemove = "member-2",
        )
        val allowed = assertIs<RemoveMemberDecision.Allowed>(result)
        assertEquals("member-2", allowed.removedUserId)
        assertEquals("Bob", allowed.preservedDisplayName)
    }

    @Test
    fun `owner removing unknown member falls back to Unknown`() {
        val result = TripBusinessRules.evaluateRemoveMember(
            removerId        = "owner-1",
            tripCreatorId    = "owner-1",
            existingMembers  = mapOf("owner-1" to "Owner"),
            memberIdToRemove = "ghost-99",
        )
        val allowed = assertIs<RemoveMemberDecision.Allowed>(result)
        assertEquals("Unknown", allowed.preservedDisplayName)
    }

    // ── resolveLeaverDisplayName ────────────────────────────────────────────

    @Test
    fun `leaver name comes from tripMembers when present`() {
        val name = TripBusinessRules.resolveLeaverDisplayName(
            currentMembers   = mapOf("u-1" to "Alice (renamed)"),
            leaverId         = "u-1",
            authDisplayName  = "Alice",
        )
        assertEquals("Alice (renamed)", name)
    }

    @Test
    fun `leaver name falls back to auth display name when missing from members`() {
        val name = TripBusinessRules.resolveLeaverDisplayName(
            currentMembers   = emptyMap(),
            leaverId         = "u-1",
            authDisplayName  = "Alice",
        )
        assertEquals("Alice", name)
    }

    @Test
    fun `leaver name falls back to Unknown when auth name is blank`() {
        val name = TripBusinessRules.resolveLeaverDisplayName(
            currentMembers   = emptyMap(),
            leaverId         = "u-1",
            authDisplayName  = "",
        )
        assertEquals("Unknown", name)
    }

    @Test
    fun `leaver name falls back to Unknown when auth name is null`() {
        val name = TripBusinessRules.resolveLeaverDisplayName(
            currentMembers   = emptyMap(),
            leaverId         = "u-1",
            authDisplayName  = null,
        )
        assertEquals("Unknown", name)
    }
}
