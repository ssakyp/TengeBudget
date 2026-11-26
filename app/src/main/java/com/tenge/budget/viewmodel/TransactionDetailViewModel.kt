package com.tenge.budget.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tenge.budget.data.local.TransactionWithInfo
import com.tenge.budget.data.repository.OpResult
import com.tenge.budget.data.repository.TransactionRepository
import com.tenge.budget.ui.SnackbarMessenger
import com.tenge.budget.util.formatMoney
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface TransactionDetailUiState {
    data object Loading : TransactionDetailUiState
    data object NotFound : TransactionDetailUiState
    data class Found(val item: TransactionWithInfo) : TransactionDetailUiState
}

class TransactionDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: TransactionRepository,
    private val messenger: SnackbarMessenger
) : ViewModel() {

    private val id: Long = checkNotNull(savedStateHandle.get<Long>(NavArgs.ID))

    val uiState: StateFlow<TransactionDetailUiState> = repository.observe(id)
        .map { if (it == null) TransactionDetailUiState.NotFound else TransactionDetailUiState.Found(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionDetailUiState.Loading)

    private val _deleted = Channel<Unit>(Channel.BUFFERED)
    /** Fires once the row is really gone, so the screen can navigate back. */
    val deleted: Flow<Unit> = _deleted.receiveAsFlow()

    /** Deleting reverses the transaction's effect on the account balance(s); it can be refused (and then nothing changes). */
    fun delete() {
        val item = (uiState.value as? TransactionDetailUiState.Found)?.item ?: return
        val tx = item.transaction
        viewModelScope.launch {
            when (val result = repository.delete(tx)) {
                OpResult.Success -> {
                    messenger.post(
                        message = "Deleted \"${tx.title}\" (${formatMoney(tx.amount, item.currency)})",
                        actionLabel = "Undo",
                        onAction = {
                            val restored = repository.restore(tx)
                            if (restored is OpResult.Rejected) messenger.post(restored.reason)
                        }
                    )
                    _deleted.send(Unit)
                }
                is OpResult.Rejected -> messenger.post(result.reason)
            }
        }
    }
}
