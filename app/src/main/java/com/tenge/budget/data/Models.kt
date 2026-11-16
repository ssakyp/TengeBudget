package com.tenge.budget.data

import com.tenge.budget.data.local.Budget

/** INCOME adds to an account, EXPENSE subtracts from it, TRANSFER moves money between two accounts. */
enum class TxType(val label: String) {
    INCOME("Income"),
    EXPENSE("Expense"),
    TRANSFER("Transfer")
}

/** Currency that totals, summaries and budgets are calculated in. Other-currency accounts are shown but not summed. */
const val BASE_CURRENCY = "KZT"

/** Income / expenses for one month (base currency only). */
data class PeriodSummary(val income: Double = 0.0, val expenses: Double = 0.0) {
    val net: Double get() = income - expenses
}

/** A monthly category budget together with how much has been spent against it. */
data class BudgetStatus(val budget: Budget, val categoryName: String, val spent: Double) {
    val remaining: Double get() = budget.limitAmount - spent
    val exceeded: Boolean get() = spent > budget.limitAmount
    val fraction: Float
        get() = if (budget.limitAmount <= 0.0) 0f else (spent / budget.limitAmount).toFloat().coerceIn(0f, 1f)
}

/** Lifecycle of a savings goal. See [com.tenge.budget.data.repository.GoalRepository] for the transition rules. */
enum class GoalStatus(val label: String) {
    ACTIVE("Active"),
    PAUSED("Paused"),
    COMPLETED("Completed")
}

/** A contribution either moves money into a goal or takes it back out. */
enum class ContributionKind(val label: String) {
    DEPOSIT("Deposit"),
    WITHDRAWAL("Withdrawal")
}
