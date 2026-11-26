package com.tenge.budget.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tenge.budget.data.GoalStatus
import com.tenge.budget.data.local.GoalWithProgress
import com.tenge.budget.data.repository.GoalRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class GoalsOverview(
    val activeCount: Int = 0,
    val pausedCount: Int = 0,
    val completedCount: Int = 0,
    val totalSaved: Double = 0.0,
    val totalTarget: Double = 0.0
)

/** List + statistics across all savings goals; shared by the Goals tab and the Dashboard card. */
class GoalsViewModel(repository: GoalRepository) : ViewModel() {

    /** Active first, then paused, then completed; newest first inside each group. `null` = still loading. */
    val goals: StateFlow<List<GoalWithProgress>?> = repository.observeGoals()
        .map<List<GoalWithProgress>, List<GoalWithProgress>?> { list ->
            list.sortedWith(compareBy<GoalWithProgress> { it.goal.status.ordinal }.thenByDescending { it.goal.id })
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val overview: StateFlow<GoalsOverview> = goals
        .map { list ->
            val items = list.orEmpty()
            GoalsOverview(
                activeCount = items.count { it.goal.status == GoalStatus.ACTIVE },
                pausedCount = items.count { it.goal.status == GoalStatus.PAUSED },
                completedCount = items.count { it.goal.status == GoalStatus.COMPLETED },
                totalSaved = items.sumOf { it.savedAmount },
                totalTarget = items.sumOf { it.goal.targetAmount }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GoalsOverview())
}
