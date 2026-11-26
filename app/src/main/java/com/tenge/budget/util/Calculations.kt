package com.tenge.budget.util

import com.tenge.budget.data.BASE_CURRENCY
import com.tenge.budget.data.TxType
import com.tenge.budget.data.local.TransactionWithInfo
import java.time.YearMonth

/** Base-currency expenses per category id for one month — what budgets are measured against. */
fun spentByCategory(transactions: List<TransactionWithInfo>, month: YearMonth): Map<Long, Double> =
    transactions
        .filter {
            it.transaction.type == TxType.EXPENSE &&
                it.currency == BASE_CURRENCY &&
                it.transaction.categoryId != null &&
                YearMonth.from(it.transaction.date) == month
        }
        .groupBy { it.transaction.categoryId!! }
        .mapValues { (_, items) -> items.sumOf { it.transaction.amount } }
