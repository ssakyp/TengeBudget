package com.tenge.budget.data.repository

import androidx.room.withTransaction
import com.tenge.budget.data.TxType
import com.tenge.budget.data.local.AppDatabase
import com.tenge.budget.data.local.Budget
import com.tenge.budget.data.local.BudgetWithCategory
import kotlinx.coroutines.flow.Flow

/** Monthly budgets: EXPENSE categories only, one budget per category and month. */
class BudgetRepository(private val db: AppDatabase) {

    private val dao = db.budgetDao()
    private val categories = db.categoryDao()

    fun observeForMonth(month: Int, year: Int): Flow<List<BudgetWithCategory>> = dao.observeForMonth(month, year)

    suspend fun create(categoryId: Long, limit: Double, month: Int, year: Int): OpResult = db.withTransaction {
        val problem = validate(categoryId, limit, month, year, ignoreId = null)
        if (problem != null) return@withTransaction OpResult.Rejected(problem)
        dao.insert(Budget(categoryId = categoryId, limitAmount = limit, month = month, year = year))
        OpResult.Success
    }

    suspend fun update(id: Long, categoryId: Long, limit: Double, month: Int, year: Int): OpResult = db.withTransaction {
        val existing = dao.getById(id) ?: return@withTransaction OpResult.Rejected("This budget no longer exists.")
        val problem = validate(categoryId, limit, month, year, ignoreId = id)
        if (problem != null) return@withTransaction OpResult.Rejected(problem)
        dao.update(existing.copy(categoryId = categoryId, limitAmount = limit, month = month, year = year))
        OpResult.Success
    }

    suspend fun delete(budget: Budget) {
        dao.delete(budget)
    }

    private suspend fun validate(categoryId: Long, limit: Double, month: Int, year: Int, ignoreId: Long?): String? {
        if (limit <= 0.0) return "Limit must be greater than zero."
        if (month !in 1..12) return "Month must be between 1 and 12."
        val category = categories.getById(categoryId) ?: return "This category no longer exists."
        if (category.type != TxType.EXPENSE) return "Budgets can only be set for expense categories."
        val duplicate = dao.findByPeriod(categoryId, month, year)
        if (duplicate != null && duplicate.id != ignoreId) {
            return "\"${category.name}\" already has a budget for this month."
        }
        return null
    }
}
