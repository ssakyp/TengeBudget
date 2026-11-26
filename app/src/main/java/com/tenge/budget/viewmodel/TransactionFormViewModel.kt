package com.tenge.budget.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tenge.budget.data.TxType
import com.tenge.budget.data.local.Account
import com.tenge.budget.data.local.Category
import com.tenge.budget.data.local.Transaction
import com.tenge.budget.data.repository.AccountRepository
import com.tenge.budget.data.repository.CategoryRepository
import com.tenge.budget.data.repository.OpResult
import com.tenge.budget.data.repository.TransactionRepository
import com.tenge.budget.ui.SnackbarMessenger
import com.tenge.budget.util.round2
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TransactionFormState(
    val title: String = "",
    val amount: String = "",
    val note: String = "",
    val type: TxType = TxType.EXPENSE,
    val accountId: Long? = null,
    val targetAccountId: Long? = null,
    val categoryId: Long? = null,
    val date: LocalDate = LocalDate.now(),
    val titleError: String? = null,
    val amountError: String? = null,
    val accountError: String? = null,
    val targetError: String? = null,
    /** A rule enforced by the repository (e.g. "Not enough balance in …"). */
    val formError: String? = null,
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val notFound: Boolean = false,
    val isSaving: Boolean = false
)

/**
 * Backs both "Add" (no `id` argument) and "Edit" (`id` argument). Every
 * transaction needs an account; a transfer also needs a different destination
 * account. The draft lives here so it survives rotation.
 */
class TransactionFormViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: TransactionRepository,
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository,
    private val messenger: SnackbarMessenger
) : ViewModel() {

    private val editingId: Long? = savedStateHandle.get<Long>(NavArgs.ID)
    private var original: Transaction? = null

    /** `null` while loading, so the screen can tell "no accounts yet" from "not loaded yet". */
    val accounts: StateFlow<List<Account>?> = accountRepository.observeAll()
        .map<List<Account>, List<Account>?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val categories: StateFlow<List<Category>> = categoryRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _state = MutableStateFlow(TransactionFormState(isEditing = editingId != null, isLoading = editingId != null))
    val state: StateFlow<TransactionFormState> = _state.asStateFlow()

    private val _saved = Channel<Unit>(Channel.BUFFERED)
    /** Fires after a successful save so the screen can navigate away. */
    val saved: Flow<Unit> = _saved.receiveAsFlow()

    init {
        viewModelScope.launch {
            if (editingId != null) {
                val tx = repository.get(editingId)
                original = tx
                _state.update {
                    if (tx == null) it.copy(isLoading = false, notFound = true)
                    else it.copy(
                        isLoading = false,
                        title = tx.title,
                        amount = tx.amount.toInputString(),
                        note = tx.note,
                        type = tx.type,
                        accountId = tx.accountId,
                        targetAccountId = tx.targetAccountId,
                        categoryId = tx.categoryId,
                        date = tx.date
                    )
                }
            } else {
                // New transaction: pre-select the first account so the common case is one tap less.
                val first = accountRepository.observeAll().first().firstOrNull()
                _state.update { if (it.accountId == null) it.copy(accountId = first?.id) else it }
            }
        }
    }

    fun onTitleChange(value: String) = _state.update { it.copy(title = value.take(MAX_TITLE + 1), titleError = null, formError = null) }
    fun onAmountChange(value: String) = _state.update { it.copy(amount = cleanAmountInput(value), amountError = null, formError = null) }
    fun onNoteChange(value: String) = _state.update { it.copy(note = value.take(MAX_NOTE)) }
    fun onDateChange(date: LocalDate) = _state.update { it.copy(date = date) }

    /** Switching type clears the category (categories are type-specific) and, unless it's a transfer, the destination. */
    fun onTypeChange(type: TxType) = _state.update {
        if (it.type == type) it
        else it.copy(
            type = type,
            categoryId = null,
            targetAccountId = if (type == TxType.TRANSFER) it.targetAccountId else null,
            targetError = null,
            formError = null
        )
    }

    fun onAccountChange(id: Long) = _state.update {
        it.copy(
            accountId = id,
            targetAccountId = if (it.targetAccountId == id) null else it.targetAccountId,
            accountError = null,
            formError = null
        )
    }

    fun onTargetAccountChange(id: Long) = _state.update { it.copy(targetAccountId = id, targetError = null, formError = null) }
    fun onCategoryChange(id: Long?) = _state.update { it.copy(categoryId = id, formError = null) }

    fun submit() {
        val s = _state.value
        if (s.isSaving || s.isLoading) return

        val title = s.title.trim()
        val amount = s.amount.toDoubleOrNull()
        val titleError = when {
            title.isEmpty() -> "Enter a title."
            title.length > MAX_TITLE -> "Title can be at most $MAX_TITLE characters."
            else -> null
        }
        val amountError = when {
            amount == null -> "Enter a valid amount."
            amount <= 0.0 -> "Amount must be greater than zero."
            amount > MAX_AMOUNT -> "Amount is too large."
            else -> null
        }
        val accountError = if (s.accountId == null) "Choose an account." else null
        val targetError = when {
            s.type != TxType.TRANSFER -> null
            s.targetAccountId == null -> "Choose a destination account."
            s.targetAccountId == s.accountId -> "Destination must differ from the source."
            else -> null
        }
        if (titleError != null || amountError != null || accountError != null || targetError != null ||
            amount == null || s.accountId == null
        ) {
            _state.update {
                it.copy(titleError = titleError, amountError = amountError, accountError = accountError, targetError = targetError)
            }
            return
        }

        _state.update { it.copy(isSaving = true, formError = null) }
        viewModelScope.launch {
            val tx = Transaction(
                id = original?.id ?: 0,
                accountId = s.accountId,
                categoryId = if (s.type == TxType.TRANSFER) null else s.categoryId,
                targetAccountId = if (s.type == TxType.TRANSFER) s.targetAccountId else null,
                title = title,
                amount = round2(amount),
                type = s.type,
                date = s.date,
                note = s.note.trim()
            )
            val result = if (original != null) repository.update(tx) else repository.add(tx)
            when (result) {
                OpResult.Success -> {
                    messenger.post(if (original != null) "Changes saved" else "Transaction added")
                    _state.update { it.copy(isSaving = false) }
                    _saved.send(Unit)
                }
                is OpResult.Rejected -> _state.update { it.copy(isSaving = false, formError = result.reason) }
            }
        }
    }

    private companion object {
        const val MAX_TITLE = 60
        const val MAX_NOTE = 200
        const val MAX_AMOUNT = 1_000_000_000.0
    }
}
