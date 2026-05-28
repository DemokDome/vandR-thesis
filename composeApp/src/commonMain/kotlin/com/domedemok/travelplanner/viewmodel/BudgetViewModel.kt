package com.domedemok.travelplanner.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domedemok.travelplanner.data.model.Expense
import com.domedemok.travelplanner.data.model.ExpenseCategory
import com.domedemok.travelplanner.data.repository.AuthRepository
import com.domedemok.travelplanner.data.repository.ExpenseRepository
import com.domedemok.travelplanner.util.SecurityValidator
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.round

data class DebtOperation(
    val debtorId: String,
    val creditorId: String,
    val amount: Double,
)

/**
 * Per-user budget figures for a trip.
 *  - [totalPaid]: amount this user fronted (paid up-front for the group).
 *  - [totalShare]: this user's share of all expenses (their portion of the splits).
 *  - [netBalance]: positive = others owe them, negative = they owe others.
 */
data class UserBudgetSummary(
    val totalPaid: Double = 0.0,
    val totalShare: Double = 0.0,
    val netBalance: Double = 0.0,
)

data class BudgetUiState(
    val isLoading: Boolean = false,
    val expenses: List<Expense> = emptyList(),
    val totalSpent: Double = 0.0,
    val expensesByCategory: Map<ExpenseCategory, Double> = emptyMap(),
    val debts: List<DebtOperation> = emptyList(),
    val userSummaries: Map<String, UserBudgetSummary> = emptyMap(),
    val currentUserId: String? = null,
    /** Currently selected member whose detail panel is shown. */
    val selectedUserId: String? = null,

    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val error: String? = null,
)

class BudgetViewModel(
    private val expenseRepository: ExpenseRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetUiState())
    val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()

    private var budgetJob: Job? = null

    /**
     * [memberIds] is passed in (rather than derived) so users with zero expenses still
     * get a row in [BudgetUiState.userSummaries] — without it, members who haven't
     * spent yet would silently disappear from the per-user breakdown.
     */
    fun loadExpenses(tripId: String, memberIds: List<String>) {
        budgetJob?.cancel()
        budgetJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val currentUserId = authRepository.getCurrentUser()?.id

            // Default the detail panel to the current user when they're a trip member;
            // otherwise pick the first member so the panel isn't empty.
            val initialSelectedUser = _uiState.value.selectedUserId
                ?: if (memberIds.contains(currentUserId)) currentUserId else memberIds.firstOrNull()

            _uiState.value = _uiState.value.copy(
                currentUserId  = currentUserId,
                selectedUserId = initialSelectedUser,
            )

            expenseRepository.getExpensesFlow(tripId)
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load expenses"
                    )
                }
                .collect { expensesList ->
                    val total = expensesList.sumOf { it.amount }

                    val byCategory = expensesList.groupBy { it.category }
                        .mapValues { entry -> entry.value.sumOf { it.amount } }

                    val summaries = memberIds.associateWith { UserBudgetSummary() }.toMutableMap()

                    expensesList.forEach { expense ->
                        val payerStat = summaries[expense.paidBy] ?: UserBudgetSummary()
                        summaries[expense.paidBy] = payerStat.copy(
                            totalPaid  = payerStat.totalPaid  + expense.amount,
                            netBalance = payerStat.netBalance + expense.amount,
                        )
                        expense.debts.forEach { (userId, share) ->
                            val debtorStat = summaries[userId] ?: UserBudgetSummary()
                            summaries[userId] = debtorStat.copy(
                                totalShare = debtorStat.totalShare + share,
                                netBalance = debtorStat.netBalance - share,
                            )
                        }
                    }

                    val roundedSummaries = summaries.mapValues { (_, s) ->
                        s.copy(
                            totalPaid  = round2(s.totalPaid),
                            totalShare = round2(s.totalShare),
                            netBalance = round2(s.netBalance),
                        )
                    }

                    val optimalDebts = calculateOptimalDebts(expensesList)

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        expenses  = expensesList.sortedByDescending { it.date },
                        totalSpent = total,
                        expensesByCategory = byCategory,
                        debts = optimalDebts,
                        userSummaries = roundedSummaries
                    )
                }
        }
    }

    fun selectUser(userId: String) {
        _uiState.value = _uiState.value.copy(selectedUserId = userId)
    }

    fun addExpense(tripId: String, expense: Expense) {
        val safe = validateAndSanitiseExpense(expense, sumTolerance = 0.02) ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            expenseRepository.addExpense(tripId, safe)
                .onSuccess { _uiState.value = _uiState.value.copy(isSaving = false) }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(isSaving = false, error = e.message ?: "Failed to save expense")
                }
        }
    }

    fun updateExpense(tripId: String, expense: Expense) {
        val safe = validateAndSanitiseExpense(expense, sumTolerance = 0.05) ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            expenseRepository.updateExpense(tripId, safe)
                .onSuccess { _uiState.value = _uiState.value.copy(isSaving = false) }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(isSaving = false, error = e.message ?: "Failed to update expense")
                }
        }
    }

    /**
     * Sanitises and validates [expense] for save/update. Returns the cleaned expense
     * on success, or null after publishing an error to the UI on failure.
     *
     * [sumTolerance] differs between add (strict 0.02) and update (looser 0.05) to
     * accommodate already-rounded debt values being re-loaded from Firestore.
     */
    private fun validateAndSanitiseExpense(expense: Expense, sumTolerance: Double): Expense? {
        val cleanTitle = SecurityValidator.sanitize(expense.title)
        SecurityValidator.validateExpenseTitle(cleanTitle)?.let {
            _uiState.value = _uiState.value.copy(error = it)
            return null
        }
        SecurityValidator.validateExpenseAmount(expense.amount)?.let {
            _uiState.value = _uiState.value.copy(error = it)
            return null
        }
        if (abs(expense.debts.values.sum() - expense.amount) > sumTolerance) {
            _uiState.value = _uiState.value.copy(error = "The sum must add up to the total amount!")
            return null
        }
        return expense.copy(title = cleanTitle)
    }

    private fun round2(value: Double): Double = round(value * 100) / 100.0

    fun deleteExpense(tripId: String, expenseId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeleting = true, error = null)
            expenseRepository.deleteExpense(tripId, expenseId)
                .onSuccess { _uiState.value = _uiState.value.copy(isDeleting = false) }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(isDeleting = false, error = e.message ?: "Failed to delete expense")
                }
        }
    }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }

    private fun calculateOptimalDebts(expenses: List<Expense>): List<DebtOperation> {
        val netBalances = mutableMapOf<String, Double>()
        for (expense in expenses) {
            netBalances[expense.paidBy] = (netBalances[expense.paidBy] ?: 0.0) + expense.amount
            for ((userId, debtAmount) in expense.debts) {
                netBalances[userId] = (netBalances[userId] ?: 0.0) - debtAmount
            }
        }

        val roundedBalances = netBalances.mapValues { round2(it.value) }
        val debtors = roundedBalances.filter { it.value < -0.01 }.map { Pair(it.key, abs(it.value)) }.toMutableList()
        val creditors = roundedBalances.filter { it.value > 0.01 }.map { Pair(it.key, it.value) }.toMutableList()

        val transactions = mutableListOf<DebtOperation>()
        var dIndex = 0
        var cIndex = 0

        while (dIndex < debtors.size && cIndex < creditors.size) {
            val debtor = debtors[dIndex]
            val creditor = creditors[cIndex]

            val settleAmount        = min(debtor.second, creditor.second)
            val roundedSettleAmount = round2(settleAmount)

            if (roundedSettleAmount > 0) {
                transactions.add(DebtOperation(debtorId = debtor.first, creditorId = creditor.first, amount = roundedSettleAmount))
            }

            debtors[dIndex] = debtor.copy(second = debtor.second - settleAmount)
            creditors[cIndex] = creditor.copy(second = creditor.second - settleAmount)

            if (debtors[dIndex].second < 0.01) dIndex++
            if (creditors[cIndex].second < 0.01) cIndex++
        }
        return transactions
    }
}