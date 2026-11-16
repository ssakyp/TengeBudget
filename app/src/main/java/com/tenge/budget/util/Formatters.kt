package com.tenge.budget.util

import java.util.Locale

private fun symbolFor(currency: String): String = when (currency.uppercase()) {
    "KZT" -> "₸"
    "USD" -> "$"
    "EUR" -> "€"
    "RUB" -> "₽"
    else -> currency.uppercase()
}

/** Formats an amount with its currency symbol; shows cents only when there are any. */
fun formatMoney(amount: Double, currency: String): String {
    val pattern = if (amount % 1.0 == 0.0) "%,.0f" else "%,.2f"
    return "${String.format(Locale.US, pattern, amount)} ${symbolFor(currency)}"
}

/** Shortcut for the base currency (used by budgets, goals and summaries). */
fun formatTenge(amount: Double): String = formatMoney(amount, "KZT")

/** Rounds to 2 decimals so repeated additions never drift. */
fun round2(value: Double): Double = Math.round(value * 100.0) / 100.0
