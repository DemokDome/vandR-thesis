package com.domedemok.travelplanner.data.repository

import com.domedemok.travelplanner.data.model.Expense
import com.domedemok.travelplanner.data.remote.FirestoreCollections
import com.domedemok.travelplanner.data.repository.rules.ExpenseBusinessRules
import com.domedemok.travelplanner.util.getCurrentTimestamp
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.CollectionReference
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * On-the-wire shape of an [Expense] for the JS Firestore client.
 *
 * `date` is a Double rather than Long because Kotlin Long is a two-Int struct
 * in JS that the Firebase SDK rejects. The mirroring read narrows back to Long.
 */
@kotlinx.serialization.Serializable
data class ExpenseDto(
    val title:    String,
    val amount:   Double,
    val category: String,
    val paidBy:   String,
    val debts:    Map<String, Double>,
    val date:     Double,
)

class ExpenseRepositoryImpl : ExpenseRepository {


    private val firestore = Firebase.firestore

    private fun expensesRef(tripId: String): CollectionReference =
        firestore.collection(FirestoreCollections.TRIPS).document(tripId).collection(FirestoreCollections.EXPENSES)

    override fun getExpensesFlow(tripId: String): Flow<List<Expense>> =
        expensesRef(tripId)
            .orderBy("date", Direction.DESCENDING)
            .snapshots
            .map { snapshot -> snapshot.documents.mapNotNull { parseExpenseFromDoc(it) } }

    override suspend fun addExpense(tripId: String, expense: Expense): Result<Unit> = runCatching {
        val nowMs = getCurrentTimestamp()
        // JS Firestore needs Double for the 64-bit `date` Long — patch the shared
        // payload after the rule applies its creation-timestamp invariant.
        val payload = ExpenseBusinessRules.buildAddPayload(expense, creationTimestampMs = nowMs)
            .toMutableMap().apply { put("date", nowMs.toDouble()) }
        expensesRef(tripId).add(payload)
    }

    override suspend fun deleteExpense(tripId: String, expenseId: String): Result<Unit> = runCatching {
        expensesRef(tripId).document(expenseId).delete()
    }

    override suspend fun updateExpense(tripId: String, expense: Expense): Result<Unit> = runCatching {
        // `date` is intentionally omitted — keep the original creation timestamp
        // so list ordering doesn't shuffle on every edit.
        expensesRef(tripId).document(expense.id).updateFields {
            "title"    to expense.title
            "amount"   to expense.amount
            "category" to expense.category.name
            "paidBy"   to expense.paidBy
            "debts"    to expense.debts
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun parseExpenseFromDoc(doc: DocumentSnapshot): Expense? = try {
        val data = doc.data<ExpenseDto>()
        Expense(
            id       = doc.id,
            title    = data.title,
            amount   = data.amount,
            category = ExpenseBusinessRules.parseCategory(data.category),
            paidBy   = data.paidBy,
            debts    = data.debts,
            date     = data.date.toLong(),
        )
    } catch (_: Exception) { null }
}
