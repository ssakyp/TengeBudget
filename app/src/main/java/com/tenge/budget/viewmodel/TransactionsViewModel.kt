package com.tenge.budget.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tenge.budget.data.TxType
import com.tenge.budget.data.local.Account
import com.tenge.budget.data.local.Category
import com.tenge.budget.data.local.TransactionWithInfo
import com.tenge.budget.data.repository.AccountRepository
import com.tenge.budget.data.repository.CategoryRepository
import com.tenge.budget.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Full transaction list with search and type / account / category filters. `null` list = still loading. */
class TransactionsViewModel(
    repository: TransactionRepository,
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository
) : ViewModel() {

    private val all: StateFlow<List<TransactionWithInfo>?> = repository.observeAll()
        .map<List<TransactionWithInfo>, List<TransactionWithInfo>?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val accounts: StateFlow<List<Account>> = accountRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categories: StateFlow<List<Category>> = categoryRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _typeFilter = MutableStateFlow<TxType?>(null)
    val typeFilter: StateFlow<TxType?> = _typeFilter

    private val _accountFilter = MutableStateFlow<Long?>(null)
    val accountFilter: StateFlow<Long?> = _accountFilter

    private val _categoryFilter = MutableStateFlow<Long?>(null)
    val categoryFilter: StateFlow<Long?> = _categoryFilter

    val hasAnyTransactions: StateFlow<Boolean?> = all
        .map { it?.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val filteredTransactions: StateFlow<List<TransactionWithInfo>?> =
        combine(all, _query, _typeFilter, _accountFilter, _categoryFilter) { list, query, type, accountId, categoryId ->
            list?.filter { item ->
                val tx = item.transaction
                (type == null || tx.type == type) &&
                    (accountId == null || tx.accountId == accountId || tx.targetAccountId == accountId) &&
                    (categoryId == null || tx.categoryId == categoryId) &&
                    (query.isBlank() || tx.title.contains(query.trim(), ignoreCase = true))
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setQuery(text: String) { _query.value = text }
    fun setTypeFilter(type: TxType?) { _typeFilter.value = type }
    fun setAccountFilter(id: Long?) { _accountFilter.value = id }
    fun setCategoryFilter(id: Long?) { _categoryFilter.value = id }
    fun clearFilters() {
        _query.value = ""
        _typeFilter.value = null
        _accountFilter.value = null
        _categoryFilter.value = null
    }
}
