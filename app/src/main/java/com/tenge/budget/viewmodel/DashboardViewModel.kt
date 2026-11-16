package com.tenge.budget.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tenge.budget.data.BASE_CURRENCY
import com.tenge.budget.data.PeriodSummary
import com.tenge.budget.data.TxType
import com.tenge.budget.data.local.Account
import com.tenge.budget.data.local.TransactionWithInfo
import com.tenge.budget.data.repository.AccountRepository
import com.tenge.budget.data.repository.BudgetRepository
import com.tenge.budget.data.repository.TransactionRepository
import com.tenge.budget.util.spentByCategory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.YearMonth

/** All budgets of the selected month added together. */
data class BudgetOverview(
    val budgetCount: Int = 0,
    val totalLimit: Double = 0.0,
    val totalSpent: Double = 0.0,
    val exceededCount: Int = 0
)

/**
 * The Dashboard: a month selector, that month's income / expenses / net,
 * total assets over all accounts, a budget roll-up and recent activity.
 * Everything is derived from Room flows.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    transactions: TransactionRepository,
    accountRepository: AccountRepository,
    budgets: BudgetRepository
) : ViewModel() {

    private val _month = MutableStateFlow(YearMonth.now())
    val month: StateFlow<YearMonth> = _month

    fun previousMonth() = _month.update { it.minusMonths(1) }
    fun nextMonth() = _month.update { it.plusMonths(1) }

    private val allTransactions: StateFlow<List<TransactionWithInfo>?> = transactions.observeAll()
        .map<List<TransactionWithInfo>, List<TransactionWithInfo>?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val recent: StateFlow<List<TransactionWithInfo>?> = allTransactions
        .map { it?.take(5) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val summary: StateFlow<PeriodSummary> = combine(allTransactions, _month) { list, month ->
        val inMonth = list.orEmpty().filter {
            it.currency == BASE_CURRENCY && YearMonth.from(it.transaction.date) == month
        }
        PeriodSummary(
            income = inMonth.filter { it.transaction.type == TxType.INCOME }.sumOf { it.transaction.amount },
            expenses = inMonth.filter { it.transaction.type == TxType.EXPENSE }.sumOf { it.transaction.amount }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PeriodSummary())

    val accounts: StateFlow<List<Account>> = accountRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Sum of all base-currency account balances. */
    val totalAssets: StateFlow<Double> = accounts
        .map { list -> list.filter { it.currency == BASE_CURRENCY }.sumOf { it.balance } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    val hasForeignCurrencyAccounts: StateFlow<Boolean> = accounts
        .map { list -> list.any { it.currency != BASE_CURRENCY } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val budgetOverview: StateFlow<BudgetOverview> = _month
        .flatMapLatest { month ->
            combine(budgets.observeForMonth(month.monthValue, month.year), allTransactions) { list, txs ->
                val spent = spentByCategory(txs.orEmpty(), month)
                BudgetOverview(
                    budgetCount = list.size,
                    totalLimit = list.sumOf { it.budget.limitAmount },
                    totalSpent = list.sumOf { spent[it.budget.categoryId] ?: 0.0 },
                    exceededCount = list.count { (spent[it.budget.categoryId] ?: 0.0) > it.budget.limitAmount }
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BudgetOverview())
}
