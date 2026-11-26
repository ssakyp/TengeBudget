package com.tenge.budget.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tenge.budget.data.TxType
import com.tenge.budget.data.local.Category
import com.tenge.budget.data.repository.CategoryRepository
import com.tenge.budget.data.repository.OpResult
import com.tenge.budget.ui.SnackbarMessenger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The create/edit dialog; `id == null` means "new category". */
data class CategoryEditorState(
    val id: Long? = null,
    val name: String = "",
    val type: TxType = TxType.EXPENSE,
    val nameError: String? = null,
    val error: String? = null,
    val isSaving: Boolean = false
)

class CategoriesViewModel(
    private val repository: CategoryRepository,
    private val messenger: SnackbarMessenger
) : ViewModel() {

    val categories: StateFlow<List<Category>?> = repository.observeAll()
        .map<List<Category>, List<Category>?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _editor = MutableStateFlow<CategoryEditorState?>(null)
    val editor: StateFlow<CategoryEditorState?> = _editor.asStateFlow()

    fun openCreate() { _editor.value = CategoryEditorState() }
    fun openEdit(category: Category) { _editor.value = CategoryEditorState(category.id, category.name, category.type) }
    fun dismissEditor() { _editor.value = null }

    fun onNameChange(value: String) = _editor.update { it?.copy(name = value.take(MAX_NAME + 1), nameError = null, error = null) }
    fun onTypeChange(type: TxType) = _editor.update { it?.copy(type = type, error = null) }

    fun save() {
        val e = _editor.value ?: return
        if (e.isSaving) return
        val name = e.name.trim()
        val nameError = when {
            name.isEmpty() -> "Enter a name."
            name.length > MAX_NAME -> "Name can be at most $MAX_NAME characters."
            else -> null
        }
        if (nameError != null) {
            _editor.update { it?.copy(nameError = nameError) }
            return
        }
        _editor.update { it?.copy(isSaving = true) }
        viewModelScope.launch {
            val result = if (e.id == null) repository.create(name, e.type) else repository.update(e.id, name, e.type)
            when (result) {
                OpResult.Success -> {
                    _editor.value = null
                    messenger.post(if (e.id == null) "Category added" else "Category updated")
                }
                is OpResult.Rejected -> _editor.update { it?.copy(isSaving = false, error = result.reason) }
            }
        }
    }

    /** Categories that are used by transactions or budgets can't be deleted — the reason is shown. */
    fun delete(category: Category) {
        viewModelScope.launch {
            when (val result = repository.delete(category.id)) {
                OpResult.Success -> messenger.post("Deleted \"${category.name}\"")
                is OpResult.Rejected -> messenger.post(result.reason)
            }
        }
    }

    private companion object {
        const val MAX_NAME = 40
    }
}
