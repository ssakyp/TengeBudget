package com.tenge.budget.data.repository

import com.tenge.budget.data.TxType
import com.tenge.budget.data.local.Category
import com.tenge.budget.data.local.CategoryDao
import kotlinx.coroutines.flow.Flow

class CategoryRepository(private val dao: CategoryDao) {

    fun observeAll(): Flow<List<Category>> = dao.observeAll()

    suspend fun create(name: String, type: TxType): OpResult {
        if (type == TxType.TRANSFER) return OpResult.Rejected("A category is either income or expense.")
        dao.insert(Category(name = name.trim(), type = type))
        return OpResult.Success
    }

    suspend fun update(id: Long, name: String, type: TxType): OpResult {
        val category = dao.getById(id) ?: return OpResult.Rejected("This category no longer exists.")
        if (type != category.type && isInUse(id)) {
            return OpResult.Rejected("The type can't change because the category is used by transactions or budgets.")
        }
        dao.update(category.copy(name = name.trim(), type = type))
        return OpResult.Success
    }

    suspend fun delete(id: Long): OpResult {
        val category = dao.getById(id) ?: return OpResult.Success
        if (isInUse(id)) {
            return OpResult.Rejected("\"${category.name}\" is used by transactions or budgets and can't be deleted.")
        }
        dao.delete(category)
        return OpResult.Success
    }

    private suspend fun isInUse(id: Long): Boolean = dao.countTransactions(id) > 0 || dao.countBudgets(id) > 0
}
