package com.domedemok.travelplanner.data.repository

import com.domedemok.travelplanner.data.model.Expense
import com.domedemok.travelplanner.data.remote.FirestoreCollections
import com.domedemok.travelplanner.data.repository.rules.ExpenseBusinessRules
import com.domedemok.travelplanner.util.getCurrentTimestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ExpenseRepositoryImpl(
    private val firestore: FirebaseFirestore,
) : ExpenseRepository {

    private fun expensesRef(tripId: String): CollectionReference =
        firestore.collection(FirestoreCollections.TRIPS).document(tripId).collection(FirestoreCollections.EXPENSES)

    override fun getExpensesFlow(tripId: String): Flow<List<Expense>> = callbackFlow {
        val listener = expensesRef(tripId)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                if (snapshot != null) {
                    trySend(snapshot.documents.mapNotNull { parseExpense(it) })
                }
            }
        awaitClose { listener.remove() }
    }

    override suspend fun addExpense(tripId: String, expense: Expense): Result<Unit> = runCatching {
        expensesRef(tripId)
            .add(ExpenseBusinessRules.buildAddPayload(expense, creationTimestampMs = getCurrentTimestamp()))
            .await()
    }

    override suspend fun deleteExpense(tripId: String, expenseId: String): Result<Unit> = runCatching {
        expensesRef(tripId).document(expenseId).delete().await()
    }

    override suspend fun updateExpense(tripId: String, expense: Expense): Result<Unit> = runCatching {
        expensesRef(tripId).document(expense.id)
            .update(ExpenseBusinessRules.buildUpdatePayload(expense))
            .await()
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun parseExpense(doc: DocumentSnapshot): Expense? = try {
        val debtsRaw = doc.get("debts") as? Map<*, *> ?: emptyMap<Any, Any>()
        val debts = debtsRaw.mapKeys { it.key.toString() }
            .mapValues { (it.value as? Number)?.toDouble() ?: 0.0 }

        Expense(
            id       = doc.id,
            title    = doc.getString("title")  ?: "",
            amount   = doc.getDouble("amount") ?: 0.0,
            category = ExpenseBusinessRules.parseCategory(doc.getString("category")),
            paidBy   = doc.getString("paidBy") ?: "",
            debts    = debts,
            date     = doc.getLong("date")     ?: 0L,
        )
    } catch (_: Exception) { null }
}
