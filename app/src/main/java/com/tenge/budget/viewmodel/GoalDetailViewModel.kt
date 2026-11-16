package com.tenge.budget.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tenge.budget.data.ContributionKind
import com.tenge.budget.data.local.Contribution
import com.tenge.budget.data.local.GoalWithProgress
import com.tenge.budget.data.repository.ContributionResult
import com.tenge.budget.data.repository.GoalRepository
import com.tenge.budget.ui.SnackbarMessenger
import com.tenge.budget.util.formatTenge
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Statistics derived from a goal's history. */
data class GoalStats(
    val depositCount: Int,
    val withdrawalCount: Int,
    val totalDeposited: Double,
    val totalWithdrawn: Double,
    val largestDeposit: Double,
    val averageDeposit: Double
)

sealed interface GoalDetailUiState {
    data object Loading : GoalDetailUiState
    data object NotFound : GoalDetailUiState
    data class Found(
        val goal: GoalWithProgress,
        val history: List<Contribution>,
        val stats: GoalStats
    ) : GoalDetailUiState
}

/** The "Add money / Withdraw" dialog, held in the ViewModel so it survives rotation. */
data class ContributionDialogState(
    val kind: ContributionKind,
    val amount: String = "",
    val error: String? = null,
    val isSubmitting: Boolean = false
)

class GoalDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: GoalRepository,
    private val messenger: SnackbarMessenger
) : ViewModel() {

    private val goalId: Long = checkNotNull(savedStateHandle.get<Long>(NavArgs.ID))

    val uiState: StateFlow<GoalDetailUiState> = combine(
        repository.observeGoal(goalId),
        repository.observeContributions(goalId)
    ) { goal, history ->
        if (goal == null) GoalDetailUiState.NotFound
        else GoalDetailUiState.Found(goal, history, statsOf(history))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GoalDetailUiState.Loading)

    private val _dialog = MutableStateFlow<ContributionDialogState?>(null)
    val dialog: StateFlow<ContributionDialogState?> = _dialog.asStateFlow()

    private val _deleted = Channel<Unit>(Channel.BUFFERED)
    val deleted: Flow<Unit> = _deleted.receiveAsFlow()

    fun openDialog(kind: ContributionKind) { _dialog.value = ContributionDialogState(kind) }
    fun dismissDialog() { _dialog.value = null }

    fun onDialogAmountChange(value: String) {
        var seenDot = false
        val cleaned = buildString {
            for (c in value) {
                if (c.isDigit()) append(c)
                else if (c == '.' && !seenDot) { seenDot = true; append(c) }
            }
        }.take(12)
        _dialog.update { it?.copy(amount = cleaned, error = null) }
    }

    fun confirmDialog() {
        val d = _dialog.value ?: return
        if (d.isSubmitting) return
        val amount = d.amount.toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            _dialog.update { it?.copy(error = "Enter an amount greater than zero.") }
            return
        }
        _dialog.update { it?.copy(isSubmitting = true) }
        viewModelScope.launch {
            when (val result = repository.addContribution(goalId, d.kind, amount)) {
                is ContributionResult.Success -> {
                    _dialog.value = null
                    messenger.post(
                        when {
                            result.goalCompleted -> "Goal reached! 🎉"
                            d.kind == ContributionKind.DEPOSIT -> "Added ${formatTenge(amount)}"
                            else -> "Withdrew ${formatTenge(amount)}"
                        }
                    )
                }
                is ContributionResult.Rejected ->
                    _dialog.update { it?.copy(isSubmitting = false, error = result.reason) }
            }
        }
    }

    fun setPaused(paused: Boolean) {
        viewModelScope.launch {
            repository.setPaused(goalId, paused)
            messenger.post(if (paused) "Goal paused" else "Goal resumed")
        }
    }

    fun deleteGoal() {
        val name = (uiState.value as? GoalDetailUiState.Found)?.goal?.goal?.name ?: return
        viewModelScope.launch {
            repository.deleteGoal(goalId)
            messenger.post("Deleted goal \"$name\"")
            _deleted.send(Unit)
        }
    }

    private fun statsOf(history: List<Contribution>): GoalStats {
        val deposits = history.filter { it.kind == ContributionKind.DEPOSIT }
        val withdrawals = history.filter { it.kind == ContributionKind.WITHDRAWAL }
        return GoalStats(
            depositCount = deposits.size,
            withdrawalCount = withdrawals.size,
            totalDeposited = deposits.sumOf { it.amount },
            totalWithdrawn = withdrawals.sumOf { it.amount },
            largestDeposit = deposits.maxOfOrNull { it.amount } ?: 0.0,
            averageDeposit = if (deposits.isEmpty()) 0.0 else deposits.sumOf { it.amount } / deposits.size
        )
    }
}
