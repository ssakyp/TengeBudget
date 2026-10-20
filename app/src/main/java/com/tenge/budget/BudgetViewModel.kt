package com.tenge.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tenge.budget.data.Category
import com.tenge.budget.data.Summary
import com.tenge.budget.data.Transaction
import com.tenge.budget.data.TxType
import com.tenge.budget.data.sampleTransactions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * The single source of truth for the whole app. One instance is created
 * where [com.tenge.budget.ui.navigation.AppNavHost] is hosted and handed down to
 * every screen, so every destination reads and writes the same state —
 * state hoisting at the navigation-graph level instead of per-screen. Since
 * it's a [ViewModel], it survives configuration changes (rotation) and
 * back/forward navigation alike; only the UI tree gets recreated.
 *
 * Filtering and totals are plain Kotlin [combine]/[map] pipelines over the
 * underlying list — nothing is duplicated or kept manually in sync.
 */
class BudgetViewModel : ViewModel() {

    private val _transactions = MutableStateFlow(sampleTransactions())
    val transactions: StateFlow<List<Transaction>> = _transactions

    /** Simple fixed monthly spending goal the Dashboard tracks progress against — the feature that gives the app a purpose beyond plain CRUD. */
    val monthlyBudget: Double = 150_000.0

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _typeFilter = MutableStateFlow<TxType?>(null)
    val typeFilter: StateFlow<TxType?> = _typeFilter

    private val _categoryFilter = MutableStateFlow<Category?>(null)
    val categoryFilter: StateFlow<Category?> = _categoryFilter

    /** Full list, newest first — used by the Dashboard's "recent" preview. */
    val recentTransactions: StateFlow<List<Transaction>> = _transactions
        .map { it.sortedByDescending { tx -> tx.date }.take(5) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** The Transactions screen's list: everything, narrowed by search text, type, and category. */
    val filteredTransactions: StateFlow<List<Transaction>> =
        combine(_transactions, _query, _typeFilter, _categoryFilter) { all, query, type, category ->
            all.filter { tx ->
                (type == null || tx.type == type) &&
                    (category == null || tx.category == category) &&
                    (query.isBlank() || tx.title.contains(query, ignoreCase = true))
            }.sortedByDescending { it.date }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Income/expense/balance over *all* transactions, regardless of the active filter. */
    val summary: StateFlow<Summary> = _transactions
        .map { list ->
            Summary(
                income = list.filter { it.type == TxType.INCOME }.sumOf { it.amount },
                expense = list.filter { it.type == TxType.EXPENSE }.sumOf { it.amount }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Summary(0.0, 0.0))

    fun addTransaction(title: String, amount: Double, type: TxType, category: Category, note: String) {
        _transactions.update { it + Transaction(title = title, amount = amount, type = type, category = category, note = note) }
    }

    fun deleteTransaction(id: String) {
        _transactions.update { list -> list.filterNot { it.id == id } }
    }

    fun findById(id: String): Transaction? = _transactions.value.find { it.id == id }

    fun setQuery(text: String) { _query.value = text }
    fun setTypeFilter(type: TxType?) { _typeFilter.value = type }
    fun setCategoryFilter(category: Category?) { _categoryFilter.value = category }
}
