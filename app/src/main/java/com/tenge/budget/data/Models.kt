package com.tenge.budget.data

import java.time.LocalDate
import java.util.UUID

/** Whether an entry adds money to the balance or takes money away from it. */
enum class TxType { INCOME, EXPENSE }

/**
 * A fixed category list keeps the app focused on budgeting itself rather
 * than on category CRUD. Each one knows which [TxType] it usually belongs
 * to, so the "Add" screen can default sensibly when a category is tapped.
 */
enum class Category(val label: String, val defaultType: TxType) {
    SALARY("Salary", TxType.INCOME),
    FREELANCE("Freelance", TxType.INCOME),
    FOOD("Food", TxType.EXPENSE),
    TRANSPORT("Transport", TxType.EXPENSE),
    SHOPPING("Shopping", TxType.EXPENSE),
    BILLS("Bills", TxType.EXPENSE),
    ENTERTAINMENT("Entertainment", TxType.EXPENSE),
    OTHER("Other", TxType.EXPENSE)
}

/** One logged transaction. Plain Kotlin data class — no Android/Compose dependency here on purpose. */
data class Transaction(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val amount: Double,
    val type: TxType,
    val category: Category,
    val date: LocalDate = LocalDate.now(),
    val note: String = ""
)

/** Totals derived from the current transaction list — recomputed, never stored. */
data class Summary(val income: Double, val expense: Double) {
    val balance: Double get() = income - expense
}

/** A couple of weeks of starter data so every screen has something to show on first launch. */
fun sampleTransactions(): List<Transaction> {
    val today = LocalDate.now()
    return listOf(
        Transaction(title = "October salary", amount = 350_000.0, type = TxType.INCOME, category = Category.SALARY, date = today.minusDays(12)),
        Transaction(title = "Grocery run", amount = 18_500.0, type = TxType.EXPENSE, category = Category.FOOD, date = today.minusDays(10)),
        Transaction(title = "Taxi to campus", amount = 2_200.0, type = TxType.EXPENSE, category = Category.TRANSPORT, date = today.minusDays(9)),
        Transaction(title = "Logo design gig", amount = 60_000.0, type = TxType.INCOME, category = Category.FREELANCE, date = today.minusDays(7)),
        Transaction(title = "Phone bill", amount = 4_500.0, type = TxType.EXPENSE, category = Category.BILLS, date = today.minusDays(5)),
        Transaction(title = "New headphones", amount = 22_000.0, type = TxType.EXPENSE, category = Category.SHOPPING, date = today.minusDays(3)),
        Transaction(title = "Movie night", amount = 6_000.0, type = TxType.EXPENSE, category = Category.ENTERTAINMENT, date = today.minusDays(1))
    )
}
