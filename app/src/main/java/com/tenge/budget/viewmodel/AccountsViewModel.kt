package com.tenge.budget.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tenge.budget.data.local.Account
import com.tenge.budget.data.repository.AccountRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** The accounts list plus the combined balance per currency. `null` = still loading. */
class AccountsViewModel(repository: AccountRepository) : ViewModel() {

    val accounts: StateFlow<List<Account>?> = repository.observeAll()
        .map<List<Account>, List<Account>?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Total balance for each currency, e.g. {KZT=390000.0, USD=4200.5}. */
    val totalsByCurrency: StateFlow<Map<String, Double>> = accounts
        .map { list -> list.orEmpty().groupBy { it.currency }.mapValues { (_, v) -> v.sumOf { it.balance } } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
}
