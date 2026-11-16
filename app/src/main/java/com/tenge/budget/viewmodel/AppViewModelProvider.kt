package com.tenge.budget.viewmodel

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tenge.budget.TengeApp
import com.tenge.budget.di.AppContainer

/** Navigation argument keys shared by the routes and the ViewModels that read them from SavedStateHandle. */
object NavArgs {
    const val ID = "id"
}

private fun CreationExtras.container(): AppContainer = (this[APPLICATION_KEY] as TengeApp).container

/** One factory for every ViewModel; each `initializer` is only run when that class is requested. */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            val c = container()
            DashboardViewModel(c.transactionRepository, c.accountRepository, c.budgetRepository)
        }
        initializer {
            val c = container()
            TransactionsViewModel(c.transactionRepository, c.accountRepository, c.categoryRepository)
        }
        initializer {
            val c = container()
            TransactionDetailViewModel(createSavedStateHandle(), c.transactionRepository, c.messenger)
        }
        initializer {
            val c = container()
            TransactionFormViewModel(
                createSavedStateHandle(), c.transactionRepository, c.accountRepository, c.categoryRepository, c.messenger
            )
        }
        initializer { AccountsViewModel(container().accountRepository) }
        initializer {
            val c = container()
            AccountDetailViewModel(createSavedStateHandle(), c.accountRepository, c.transactionRepository, c.messenger)
        }
        initializer {
            val c = container()
            AccountFormViewModel(createSavedStateHandle(), c.accountRepository, c.messenger)
        }
        initializer {
            val c = container()
            CategoriesViewModel(c.categoryRepository, c.messenger)
        }
        initializer {
            val c = container()
            BudgetsViewModel(c.budgetRepository, c.transactionRepository, c.categoryRepository, c.messenger)
        }
        initializer { GoalsViewModel(container().goalRepository) }
        initializer {
            val c = container()
            GoalDetailViewModel(createSavedStateHandle(), c.goalRepository, c.messenger)
        }
        initializer {
            val c = container()
            GoalFormViewModel(createSavedStateHandle(), c.goalRepository, c.messenger)
        }
    }
}
