package com.domedemok.travelplanner.data.repository.rules

import com.domedemok.travelplanner.data.model.Expense
import com.domedemok.travelplanner.data.model.ExpenseCategory

/**
 * Pure decision logic for [com.domedemok.travelplanner.data.repository.ExpenseRepository].
 *
 * Two non-obvious invariants live here:
 *  - On *add*, the client-supplied `date` is *overridden* with the current
 *    timestamp so the sort order matches the moment of creation.
 *  - On *update*, the `date` field is *omitted* so an edit doesn't shuffle
 *    the expense to the top of the list.
 *
 * Both rules are easy to break accidentally on either platform, so we encode
 * them here once.
 */
object ExpenseBusinessRules {

    /**
     * Field-shape for `trips/{tripId}/expenses/{id}` on insert.
     *
     * @param creationTimestampMs Server-side wall-clock value to overwrite
     *                            whatever was on the incoming [Expense.date].
     */
    fun buildAddPayload(expense: Expense, creationTimestampMs: Long): Map<String, Any> = mapOf(
        "title"    to expense.title,
        "amount"   to expense.amount,
        "category" to expense.category.name,
        "paidBy"   to expense.paidBy,
        "debts"    to expense.debts,
        "date"     to creationTimestampMs,
    )

    /**
     * Field-shape for `trips/{tripId}/expenses/{id}` on update.
     *
     * Intentionally omits `date` so the list's chronological order is stable
     * across edits — see class KDoc.
     */
    fun buildUpdatePayload(expense: Expense): Map<String, Any> = mapOf(
        "title"    to expense.title,
        "amount"   to expense.amount,
        "category" to expense.category.name,
        "paidBy"   to expense.paidBy,
        "debts"    to expense.debts,
    )

    /**
     * Parses a stored category name back to [ExpenseCategory]. An unknown or
     * corrupt value resolves to [ExpenseCategory.OTHER] rather than throwing —
     * one bad row shouldn't take down the whole list.
     */
    fun parseCategory(rawCategory: String?): ExpenseCategory =
        runCatching { ExpenseCategory.valueOf(rawCategory.orEmpty()) }
            .getOrDefault(ExpenseCategory.OTHER)
}
