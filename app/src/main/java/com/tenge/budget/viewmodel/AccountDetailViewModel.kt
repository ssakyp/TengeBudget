package com.tenge.budget.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tenge.budget.data.local.Account
import com.tenge.budget.data.local.TransactionWithInfo
import com.tenge.budget.data.repository.AccountRepository
import com.tenge.budget.data.repository.OpResult
import com.tenge.budget.data.repository.TransactionRepository
import com.tenge.budget.ui.SnackbarMessenger
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AccountDetailUiState {
    data object Loading : AccountDetailUiState
    data object NotFound : AccountDetailUiState
    data class Found(val account: Account, val transactions: List<TransactionWithInfo>) : AccountDetailUiState
}

class AccountDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: AccountRepository,
    transactions: TransactionRepository,
    private val messenger: SnackbarMessenger
) : ViewModel() {

    private val id: Long = checkNotNull(savedStateHandle.get<Long>(NavArgs.ID))

    /** The account and every transaction that touches it (as source or as transfer destination). */
    val uiState: StateFlow<AccountDetailUiState> = combine(repository.observe(id), transactions.observeAll()) { account, all ->
        if (account == null) AccountDetailUiState.NotFound
        else AccountDetailUiState.Found(
            account,
            all.filter { it.transaction.accountId == id || it.transaction.targetAccountId == id }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountDetailUiState.Loading)

    private val _deleted = Channel<Unit>(Channel.BUFFERED)
    val deleted: Flow<Unit> = _deleted.receiveAsFlow()

    /** An account that still has transactions can't be deleted — the user is told why. */
    fun delete() {
        val name = (uiState.value as? AccountDetailUiState.Found)?.account?.name ?: return
        viewModelScope.launch {
            when (val result = repository.delete(id)) {
                OpResult.Success -> {
                    messenger.post("Deleted account \"$name\"")
                    _deleted.send(Unit)
                }
                is OpResult.Rejected -> messenger.post(result.reason)
            }
        }
    }
}
