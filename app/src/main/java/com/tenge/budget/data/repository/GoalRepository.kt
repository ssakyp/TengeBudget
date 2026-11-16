package com.tenge.budget.data.repository

import androidx.room.withTransaction
import com.tenge.budget.data.ContributionKind
import com.tenge.budget.data.GoalStatus
import com.tenge.budget.data.local.AppDatabase
import com.tenge.budget.data.local.Contribution
import com.tenge.budget.data.local.GoalWithProgress
import com.tenge.budget.data.local.SavingsGoal
import com.tenge.budget.util.formatTenge
import kotlinx.coroutines.flow.Flow

sealed interface ContributionResult {
    data class Success(val goalCompleted: Boolean) : ContributionResult
    data class Rejected(val reason: String) : ContributionResult
}

sealed interface GoalSaveResult {
    data object Success : GoalSaveResult
    data class Rejected(val reason: String) : GoalSaveResult
}

/**
 * Savings goals with a small state machine:
 *
 *  ACTIVE ──deposit reaches target──▶ COMPLETED
 *  COMPLETED ──withdraw / raise target──▶ ACTIVE
 *  ACTIVE ⇄ PAUSED (manual; a paused goal accepts no deposits or withdrawals)
 *
 * Every rule is enforced here, inside a single database transaction, so the
 * UI can never leave a goal in an inconsistent state.
 */
class GoalRepository(private val db: AppDatabase) {

    private val dao = db.goalDao()

    fun observeGoals(): Flow<List<GoalWithProgress>> = dao.observeGoals()
    fun observeGoal(id: Long): Flow<GoalWithProgress?> = dao.observeGoal(id)
    fun observeContributions(goalId: Long): Flow<List<Contribution>> = dao.observeContributions(goalId)
    suspend fun getGoal(id: Long): GoalWithProgress? = dao.getGoal(id)

    suspend fun createGoal(name: String, target: Double) {
        dao.insertGoal(SavingsGoal(name = name, targetAmount = target))
    }

    suspend fun updateGoal(id: Long, name: String, target: Double): GoalSaveResult = db.withTransaction {
        val current = dao.getGoal(id) ?: return@withTransaction GoalSaveResult.Rejected("This goal no longer exists.")
        if (target < current.savedAmount - EPS) {
            return@withTransaction GoalSaveResult.Rejected(
                "Target can't be lower than the ${formatTenge(current.savedAmount)} already saved."
            )
        }
        dao.updateGoal(
            current.goal.copy(name = name, targetAmount = target, status = statusFor(current.goal.status, current.savedAmount, target))
        )
        GoalSaveResult.Success
    }

    suspend fun deleteGoal(id: Long) {
        dao.getGoal(id)?.let { dao.deleteGoal(it.goal) }
    }

    suspend fun setPaused(id: Long, paused: Boolean) {
        db.withTransaction {
            val current = dao.getGoal(id)
            if (current != null) {
                val newStatus = if (paused) {
                    GoalStatus.PAUSED
                } else {
                    statusFor(GoalStatus.ACTIVE, current.savedAmount, current.goal.targetAmount)
                }
                if (newStatus != current.goal.status) dao.updateGoal(current.goal.copy(status = newStatus))
            }
        }
    }

    suspend fun addContribution(goalId: Long, kind: ContributionKind, amount: Double): ContributionResult = db.withTransaction {
        val current = dao.getGoal(goalId)
            ?: return@withTransaction ContributionResult.Rejected("This goal no longer exists.")
        val goal = current.goal

        val problem: String? = when {
            goal.status == GoalStatus.PAUSED -> "Resume the goal before changing its balance."
            kind == ContributionKind.DEPOSIT && goal.status == GoalStatus.COMPLETED -> "This goal is already complete."
            kind == ContributionKind.DEPOSIT && amount > current.remaining + EPS ->
                "Only ${formatTenge(current.remaining)} left to reach this goal."
            kind == ContributionKind.WITHDRAWAL && amount > current.savedAmount + EPS ->
                "Only ${formatTenge(current.savedAmount)} is saved in this goal."
            else -> null
        }
        if (problem != null) return@withTransaction ContributionResult.Rejected(problem)

        dao.insertContribution(Contribution(goalId = goalId, kind = kind, amount = amount))

        val newSaved = if (kind == ContributionKind.DEPOSIT) current.savedAmount + amount else current.savedAmount - amount
        val newStatus = statusFor(goal.status, newSaved, goal.targetAmount)
        if (newStatus != goal.status) dao.updateGoal(goal.copy(status = newStatus))

        ContributionResult.Success(goalCompleted = newStatus == GoalStatus.COMPLETED && goal.status != GoalStatus.COMPLETED)
    }

    /** Re-evaluates the status after the saved amount or the target changed. Paused stays paused. */
    private fun statusFor(old: GoalStatus, saved: Double, target: Double): GoalStatus = when {
        old == GoalStatus.PAUSED -> GoalStatus.PAUSED
        saved >= target - EPS -> GoalStatus.COMPLETED
        else -> GoalStatus.ACTIVE
    }

    private companion object {
        const val EPS = 0.005
    }
}
