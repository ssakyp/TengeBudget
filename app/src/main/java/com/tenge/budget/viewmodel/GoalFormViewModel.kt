package com.tenge.budget.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tenge.budget.data.repository.GoalRepository
import com.tenge.budget.data.repository.GoalSaveResult
import com.tenge.budget.ui.SnackbarMessenger
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GoalFormState(
    val name: String = "",
    val target: String = "",
    val nameError: String? = null,
    val targetError: String? = null,
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val notFound: Boolean = false,
    val isSaving: Boolean = false
)

/** Create / edit form for a savings goal (edit when an `id` argument is present). */
class GoalFormViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: GoalRepository,
    private val messenger: SnackbarMessenger
) : ViewModel() {

    private val editingId: Long? = savedStateHandle.get<Long>(NavArgs.ID)

    private val _state = MutableStateFlow(GoalFormState(isEditing = editingId != null, isLoading = editingId != null))
    val state: StateFlow<GoalFormState> = _state.asStateFlow()

    private val _saved = Channel<Unit>(Channel.BUFFERED)
    val saved: Flow<Unit> = _saved.receiveAsFlow()

    init {
        if (editingId != null) {
            viewModelScope.launch {
                val g = repository.getGoal(editingId)
                _state.update {
                    if (g == null) it.copy(isLoading = false, notFound = true)
                    else it.copy(
                        isLoading = false,
                        name = g.goal.name,
                        target = if (g.goal.targetAmount % 1.0 == 0.0) g.goal.targetAmount.toLong().toString() else g.goal.targetAmount.toString()
                    )
                }
            }
        }
    }

    fun onNameChange(value: String) = _state.update { it.copy(name = value.take(MAX_NAME + 1), nameError = null) }

    fun onTargetChange(value: String) {
        var seenDot = false
        val cleaned = buildString {
            for (c in value) {
                if (c.isDigit()) append(c)
                else if (c == '.' && !seenDot) { seenDot = true; append(c) }
            }
        }.take(12)
        _state.update { it.copy(target = cleaned, targetError = null) }
    }

    fun submit() {
        val s = _state.value
        if (s.isSaving || s.isLoading) return

        val name = s.name.trim()
        val target = s.target.toDoubleOrNull()
        val nameError = when {
            name.isEmpty() -> "Give your goal a name."
            name.length > MAX_NAME -> "Name can be at most $MAX_NAME characters."
            else -> null
        }
        val targetError = when {
            target == null -> "Enter a target amount."
            target <= 0.0 -> "Target must be greater than zero."
            target > MAX_TARGET -> "Target is too large."
            else -> null
        }
        if (nameError != null || targetError != null || target == null) {
            _state.update { it.copy(nameError = nameError, targetError = targetError) }
            return
        }

        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            if (editingId == null) {
                repository.createGoal(name, target)
                messenger.post("Goal \"$name\" created")
                _state.value = GoalFormState()
                _saved.send(Unit)
            } else {
                when (val result = repository.updateGoal(editingId, name, target)) {
                    GoalSaveResult.Success -> {
                        messenger.post("Goal updated")
                        _state.update { it.copy(isSaving = false) }
                        _saved.send(Unit)
                    }
                    is GoalSaveResult.Rejected -> _state.update { it.copy(isSaving = false, targetError = result.reason) }
                }
            }
        }
    }

    private companion object {
        const val MAX_NAME = 40
        const val MAX_TARGET = 1_000_000_000.0
    }
}
