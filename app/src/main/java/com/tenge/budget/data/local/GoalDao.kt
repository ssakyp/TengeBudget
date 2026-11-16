package com.tenge.budget.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {

    @Query(
        """
        SELECT g.*, COALESCE(SUM(CASE WHEN c.kind = 'DEPOSIT' THEN c.amount ELSE -c.amount END), 0) AS savedAmount
        FROM savings_goals g
        LEFT JOIN contributions c ON c.goalId = g.id
        GROUP BY g.id
        ORDER BY g.id DESC
        """
    )
    fun observeGoals(): Flow<List<GoalWithProgress>>

    @Query(
        """
        SELECT g.*, COALESCE(SUM(CASE WHEN c.kind = 'DEPOSIT' THEN c.amount ELSE -c.amount END), 0) AS savedAmount
        FROM savings_goals g
        LEFT JOIN contributions c ON c.goalId = g.id
        WHERE g.id = :id
        GROUP BY g.id
        """
    )
    fun observeGoal(id: Long): Flow<GoalWithProgress?>

    @Query(
        """
        SELECT g.*, COALESCE(SUM(CASE WHEN c.kind = 'DEPOSIT' THEN c.amount ELSE -c.amount END), 0) AS savedAmount
        FROM savings_goals g
        LEFT JOIN contributions c ON c.goalId = g.id
        WHERE g.id = :id
        GROUP BY g.id
        """
    )
    suspend fun getGoal(id: Long): GoalWithProgress?

    @Query("SELECT * FROM contributions WHERE goalId = :goalId ORDER BY timestamp DESC, id DESC")
    fun observeContributions(goalId: Long): Flow<List<Contribution>>

    @Insert
    suspend fun insertGoal(goal: SavingsGoal): Long

    @Update
    suspend fun updateGoal(goal: SavingsGoal)

    @Delete
    suspend fun deleteGoal(goal: SavingsGoal)

    @Insert
    suspend fun insertContribution(contribution: Contribution): Long
}
