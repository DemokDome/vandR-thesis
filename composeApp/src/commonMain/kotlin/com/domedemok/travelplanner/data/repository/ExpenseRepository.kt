package com.domedemok.travelplanner.data.repository

import com.domedemok.travelplanner.data.model.Expense
import kotlinx.coroutines.flow.Flow

/**
 * Per-trip expense tracking. The flow yields expenses sorted newest-first;
 * the underlying `debts` map (see [Expense]) is excluded from the payer.
 */
interface ExpenseRepository {
    fun getExpensesFlow(tripId: String): Flow<List<Expense>>
    suspend fun addExpense(tripId: String, expense: Expense): Result<Unit>
    suspend fun deleteExpense(tripId: String, expenseId: String): Result<Unit>

    /** Updates everything except the original creation [Expense.date] (kept stable for sort order). */
    suspend fun updateExpense(tripId: String, expense: Expense): Result<Unit>
}
