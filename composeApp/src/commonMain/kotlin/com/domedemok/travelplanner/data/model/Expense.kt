package com.domedemok.travelplanner.data.model

import kotlinx.serialization.Serializable

/**
 * A single expense recorded against a trip's shared budget.
 *
 * [debts] maps each indebted member's user ID to the amount they owe back to
 * [paidBy]. The payer themselves is **not** included in this map.
 */
@Serializable
data class Expense(
    val id:       String          = "",
    val title:    String          = "",
    val amount:   Double          = 0.0,
    val category: ExpenseCategory = ExpenseCategory.OTHER,
    val paidBy:   String          = "",                  // Firebase Auth UID
    val debts:    Map<String, Double> = emptyMap(),      // userId → amount owed
    val date:     Long            = 0L,
)

/**
 * Display name is the English fallback shown when the i18n layer has no
 * translation for the category — UI code should prefer the localised string.
 */
enum class ExpenseCategory(val displayName: String) {
    FOOD_AND_DRINK         ("Food & Drink"),
    TRANSPORTATION         ("Transportation"),
    ACCOMMODATION          ("Accommodation"),
    ACTIVITIES_AND_TICKETS ("Activities & Tickets"),
    SHOPPING               ("Shopping"),
    OTHER                  ("Other"),
}
