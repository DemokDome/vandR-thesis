package com.domedemok.travelplanner.data.repository.rules

import com.domedemok.travelplanner.data.model.Expense
import com.domedemok.travelplanner.data.model.ExpenseCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExpenseBusinessRulesTest {

    private val sampleExpense = Expense(
        id       = "e-1",
        title    = "Dinner",
        amount   = 42.5,
        category = ExpenseCategory.FOOD_AND_DRINK,
        paidBy   = "u-1",
        debts    = mapOf("u-1" to 0.0, "u-2" to 21.25),
        date     = 1_700_000_000_000L,  // should be IGNORED on add
    )

    // ── buildAddPayload ─────────────────────────────────────────────────────

    @Test
    fun `add payload overrides expense date with creation timestamp`() {
        val payload = ExpenseBusinessRules.buildAddPayload(
            expense              = sampleExpense,
            creationTimestampMs  = 9_999L,
        )
        assertEquals(9_999L, payload["date"])
        // The original expense's date must NOT leak through.
        assertFalse(payload["date"] == 1_700_000_000_000L)
    }

    @Test
    fun `add payload includes the six expected fields`() {
        val payload = ExpenseBusinessRules.buildAddPayload(sampleExpense, 9_999L)
        assertEquals(
            setOf("title", "amount", "category", "paidBy", "debts", "date"),
            payload.keys,
        )
    }

    @Test
    fun `add payload writes category as enum name string`() {
        val payload = ExpenseBusinessRules.buildAddPayload(sampleExpense, 9_999L)
        assertEquals("FOOD_AND_DRINK", payload["category"])
    }

    // ── buildUpdatePayload ──────────────────────────────────────────────────

    @Test
    fun `update payload omits date to preserve sort order`() {
        val payload = ExpenseBusinessRules.buildUpdatePayload(sampleExpense)
        assertFalse("date" in payload.keys)
    }

    @Test
    fun `update payload includes every editable field except date`() {
        val payload = ExpenseBusinessRules.buildUpdatePayload(sampleExpense)
        assertEquals(
            setOf("title", "amount", "category", "paidBy", "debts"),
            payload.keys,
        )
    }

    // ── parseCategory ───────────────────────────────────────────────────────

    @Test
    fun `known category name parses to the enum`() {
        assertEquals(ExpenseCategory.FOOD_AND_DRINK,           ExpenseBusinessRules.parseCategory("FOOD_AND_DRINK"))
        assertEquals(ExpenseCategory.TRANSPORTATION, ExpenseBusinessRules.parseCategory("TRANSPORTATION"))
    }

    @Test
    fun `unknown category name falls back to OTHER`() {
        assertEquals(ExpenseCategory.OTHER, ExpenseBusinessRules.parseCategory("CRYPTO_FLOPS"))
    }

    @Test
    fun `null category falls back to OTHER`() {
        assertEquals(ExpenseCategory.OTHER, ExpenseBusinessRules.parseCategory(null))
    }

    @Test
    fun `empty category falls back to OTHER`() {
        assertEquals(ExpenseCategory.OTHER, ExpenseBusinessRules.parseCategory(""))
    }

    // ── debts payload pass-through ──────────────────────────────────────────

    @Test
    fun `debts map is preserved verbatim in both add and update payloads`() {
        val addPayload    = ExpenseBusinessRules.buildAddPayload(sampleExpense, 1L)
        val updatePayload = ExpenseBusinessRules.buildUpdatePayload(sampleExpense)
        @Suppress("UNCHECKED_CAST")
        val addDebts    = addPayload["debts"]    as Map<String, Double>
        @Suppress("UNCHECKED_CAST")
        val updateDebts = updatePayload["debts"] as Map<String, Double>
        assertTrue(addDebts == sampleExpense.debts)
        assertTrue(updateDebts == sampleExpense.debts)
    }
}
