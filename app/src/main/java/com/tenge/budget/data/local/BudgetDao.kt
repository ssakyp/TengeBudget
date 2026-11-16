package com.tenge.budget.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query(
        """
        SELECT b.*, c.name AS categoryName
        FROM budgets b INNER JOIN categories c ON c.id = b.categoryId
        WHERE b.month = :month AND b.year = :year
        ORDER BY c.name COLLATE NOCASE
        """
    )
    fun observeForMonth(month: Int, year: Int): Flow<List<BudgetWithCategory>>

    @Query("SELECT * FROM budgets WHERE id = :id")
    suspend fun getById(id: Long): Budget?

    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId AND month = :month AND year = :year LIMIT 1")
    suspend fun findByPeriod(categoryId: Long, month: Int, year: Int): Budget?

    @Insert
    suspend fun insert(budget: Budget): Long

    @Update
    suspend fun update(budget: Budget)

    @Delete
    suspend fun delete(budget: Budget)
}
