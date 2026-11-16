package com.tenge.budget.di

import android.content.Context
import com.tenge.budget.data.local.AppDatabase
import com.tenge.budget.data.repository.AccountRepository
import com.tenge.budget.data.repository.BudgetRepository
import com.tenge.budget.data.repository.CategoryRepository
import com.tenge.budget.data.repository.GoalRepository
import com.tenge.budget.data.repository.TransactionRepository
import com.tenge.budget.ui.SnackbarMessenger

/** Hand-rolled dependency container: one database, one repository per feature, one shared messenger. */
class AppContainer(context: Context) {
    private val database: AppDatabase = AppDatabase.create(context)

    val accountRepository = AccountRepository(database.accountDao())
    val categoryRepository = CategoryRepository(database.categoryDao())
    val budgetRepository = BudgetRepository(database)
    val transactionRepository = TransactionRepository(database)
    val goalRepository = GoalRepository(database)
    val messenger = SnackbarMessenger()
}
