package com.tenge.budget.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tenge.budget.data.repository.AccountRepository
import com.tenge.budget.data.repository.OpResult
import com.tenge.budget.ui.SnackbarMessenger
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccountFormState(
    val name: String = "",
    val balance: String = "0",
    val currency: String = "KZT",
    val nameError: String? = null,
    val balanceError: String? = null,
    val currencyError: String? = null,
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val notFound: Boolean = false,
    val isSaving: Boolean = false
)

/**
 * Create / edit an account. The balance can be entered only when creating
 * (as the opening balance); when editing it is read-only because from then on
 * only transactions change it.
 */
class AccountFormViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: AccountRepository,
    private val messenger: SnackbarMessenger
) : ViewModel() {

    private val editingId: Long? = savedStateHandle.get<Long>(NavArgs.ID)

    private val _state = MutableStateFlow(AccountFormState(isEditing = editingId != null, isLoading = editingId != null))
    val state: StateFlow<AccountFormState> = _state.asStateFlow()

    private val _saved = Channel<Unit>(Channel.BUFFERED)
    val saved: Flow<Unit> = _saved.receiveAsFlow()

    init {
        if (editingId != null) {
            viewModelScope.launch {
                val a = repository.get(editingId)
                _state.update {
                    if (a == null) it.copy(isLoading = false, notFound = true)
                    else it.copy(isLoading = false, name = a.name, balance = a.balance.toInputString(), currency = a.currency)
                }
            }
        }
    }

    fun onNameChange(value: String) = _state.update { it.copy(name = value.take(MAX_NAME + 1), nameError = null) }
    fun onBalanceChange(value: String) = _state.update { it.copy(balance = cleanAmountInput(value), balanceError = null) }
    fun onCurrencyChange(value: String) =
        _state.update { it.copy(currency = value.filter { c -> c.isLetter() }.take(3).uppercase(), currencyError = null) }

    fun submit() {
        val s = _state.value
        if (s.isSaving || s.isLoading) return

        val name = s.name.trim()
        val balance = if (s.isEditing) 0.0 else s.balance.toDoubleOrNull()
        val nameError = when {
            name.isEmpty() -> "Give the account a name."
            name.length > MAX_NAME -> "Name can be at most $MAX_NAME characters."
            else -> null
        }
        val balanceError = when {
            balance == null -> "Enter an opening balance (0 is fine)."
            balance < 0.0 -> "Balance can't be negative."
            balance > MAX_BALANCE -> "Balance is too large."
            else -> null
        }
        val currencyError = if (!Regex("^[A-Z]{3}$").matches(s.currency)) "Use a 3-letter code, e.g. KZT." else null
        if (nameError != null || balanceError != null || currencyError != null || balance == null) {
            _state.update { it.copy(nameError = nameError, balanceError = balanceError, currencyError = currencyError) }
            return
        }

        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val result = if (editingId == null) repository.create(name, balance, s.currency)
            else repository.update(editingId, name, s.currency)
            when (result) {
                OpResult.Success -> {
                    messenger.post(if (editingId == null) "Account \"$name\" created" else "Account updated")
                    _state.update { it.copy(isSaving = false) }
                    _saved.send(Unit)
                }
                is OpResult.Rejected -> _state.update { it.copy(isSaving = false, currencyError = result.reason) }
            }
        }
    }

    private companion object {
        const val MAX_NAME = 40
        const val MAX_BALANCE = 1_000_000_000_000.0
    }
}
