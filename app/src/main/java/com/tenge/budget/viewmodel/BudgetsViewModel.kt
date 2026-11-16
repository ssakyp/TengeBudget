package com.tenge.budget.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tenge.budget.data.BudgetStatus
import com.tenge.budget.data.TxType
import com.tenge.budget.data.local.Category
import com.tenge.budget.data.repository.BudgetRepository
import com.tenge.budget.data.repository.CategoryRepository
import com.tenge.budget.data.repository.OpResult
import com.tenge.budget.data.repository.TransactionRepository
import com.tenge.budget.ui.SnackbarMessenger
import com.tenge.budget.util.spentByCategory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth

/** Create/edit dialog for a budget. The period is fixed when the dialog opens (the month shown on screen). */
data class BudgetEditorState(
    val id: Long? = null,
    val categoryId: Long? = null,
    val limit: String = "",
    val month: Int,
    val year: Int,
    val categoryError: String? = null,
    val limitError: String? = null,
    val error: String? = null,
    val isSaving: Boolean = false
)

/**
 * Monthly budgets for the selected month. "Spent" is calculated live from the
 * month's EXPENSE transactions in that budget's category, so logging a
 * transaction anywhere moves the progress bars here immediately.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BudgetsViewModel(
    private val repository: BudgetRepository,
    transactions: TransactionRepository,
    categoryRepository: CategoryRepository,
    private val messenger: SnackbarMessenger
) : ViewModel() {

    private val _month = MutableStateFlow(YearMonth.now())
    val month: StateFlow<YearMonth> = _month

    fun previousMonth() = _month.update { it.minusMonths(1) }
    fun nextMonth() = _month.update { it.plusMonths(1) }

    /** `null` = still loading. */
    val budgets: StateFlow<List<BudgetStatus>?> = _month
        .flatMapLatest { month ->
            combine(repository.observeForMonth(month.monthValue, month.year), transactions.observeAll()) { list, txs ->
                val spent = spentByCategory(txs, month)
                list.map { BudgetStatus(it.budget, it.categoryName, spent[it.budget.categoryId] ?: 0.0) }
            }
        }
        .map<List<BudgetStatus>, List<BudgetStatus>?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val expenseCategories: StateFlow<List<Category>> = categoryRepository.observeAll()
        .map { list -> list.filter { it.type == TxType.EXPENSE } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _editor = MutableStateFlow<BudgetEditorState?>(null)
    val editor: StateFlow<BudgetEditorState?> = _editor.asStateFlow()

    fun openCreate() {
        val m = _month.value
        _editor.value = BudgetEditorState(month = m.monthValue, year = m.year)
    }

    fun openEdit(status: BudgetStatus) {
        val b = status.budget
        _editor.value = BudgetEditorState(
            id = b.id, categoryId = b.categoryId, limit = b.limitAmount.toInputString(), month = b.month, year = b.year
        )
    }

    fun dismissEditor() { _editor.value = null }
    fun onCategoryChange(id: Long) = _editor.update { it?.copy(categoryId = id, categoryError = null, error = null) }
    fun onLimitChange(value: String) = _editor.update { it?.copy(limit = cleanAmountInput(value), limitError = null, error = null) }

    fun save() {
        val e = _editor.value ?: return
        if (e.isSaving) return
        val limit = e.limit.toDoubleOrNull()
        val categoryError = if (e.categoryId == null) "Choose a category." else null
        val limitError = when {
            limit == null -> "Enter a limit."
            limit <= 0.0 -> "Limit must be greater than zero."
            limit > MAX_LIMIT -> "Limit is too large."
            else -> null
        }
        if (categoryError != null || limitError != null || limit == null || e.categoryId == null) {
            _editor.update { it?.copy(categoryError = categoryError, limitError = limitError) }
            return
        }
        _editor.update { it?.copy(isSaving = true) }
        viewModelScope.launch {
            val result = if (e.id == null) repository.create(e.categoryId, limit, e.month, e.year)
            else repository.update(e.id, e.categoryId, limit, e.month, e.year)
            when (result) {
                OpResult.Success -> {
                    _editor.value = null
                    messenger.post(if (e.id == null) "Budget added" else "Budget updated")
                }
                is OpResult.Rejected -> _editor.update { it?.copy(isSaving = false, error = result.reason) }
            }
        }
    }

    fun delete(status: BudgetStatus) {
        val budget = status.budget
        viewModelScope.launch {
            repository.delete(budget)
            messenger.post(
                message = "Deleted budget for \"${status.categoryName}\"",
                actionLabel = "Undo",
                onAction = { repository.create(budget.categoryId, budget.limitAmount, budget.month, budget.year) }
            )
        }
    }

    private companion object {
        const val MAX_LIMIT = 1_000_000_000.0
    }
}
