package com.tenge.budget.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tenge.budget.TengeApp
import com.tenge.budget.ui.screens.AccountDetailScreen
import com.tenge.budget.ui.screens.AccountFormScreen
import com.tenge.budget.ui.screens.AccountsScreen
import com.tenge.budget.ui.screens.BudgetsScreen
import com.tenge.budget.ui.screens.CategoriesScreen
import com.tenge.budget.ui.screens.DashboardScreen
import com.tenge.budget.ui.screens.GoalDetailScreen
import com.tenge.budget.ui.screens.GoalFormScreen
import com.tenge.budget.ui.screens.GoalsScreen
import com.tenge.budget.ui.screens.TransactionDetailScreen
import com.tenge.budget.ui.screens.TransactionFormScreen
import com.tenge.budget.ui.screens.TransactionsScreen
import com.tenge.budget.viewmodel.AppViewModelProvider
import com.tenge.budget.viewmodel.AccountDetailViewModel
import com.tenge.budget.viewmodel.AccountFormViewModel
import com.tenge.budget.viewmodel.AccountsViewModel
import com.tenge.budget.viewmodel.BudgetsViewModel
import com.tenge.budget.viewmodel.CategoriesViewModel
import com.tenge.budget.viewmodel.DashboardViewModel
import com.tenge.budget.viewmodel.GoalDetailViewModel
import com.tenge.budget.viewmodel.GoalFormViewModel
import com.tenge.budget.viewmodel.GoalsViewModel
import com.tenge.budget.viewmodel.NavArgs
import com.tenge.budget.viewmodel.TransactionDetailViewModel
import com.tenge.budget.viewmodel.TransactionFormViewModel
import com.tenge.budget.viewmodel.TransactionsViewModel

private object Routes {
    // bottom-bar tabs
    const val DASHBOARD = "dashboard"
    const val TRANSACTIONS = "transactions"
    const val ACCOUNTS = "accounts"
    const val BUDGETS = "budgets"
    const val GOALS = "goals"

    // pushed screens
    const val CATEGORIES = "categories"
    const val TX_NEW = "transaction_new"
    const val TX_DETAIL = "transaction/{${NavArgs.ID}}"
    const val TX_EDIT = "transaction_edit/{${NavArgs.ID}}"
    const val ACCOUNT_NEW = "account_new"
    const val ACCOUNT_DETAIL = "account/{${NavArgs.ID}}"
    const val ACCOUNT_EDIT = "account_edit/{${NavArgs.ID}}"
    const val GOAL_NEW = "goal_new"
    const val GOAL_DETAIL = "goal/{${NavArgs.ID}}"
    const val GOAL_EDIT = "goal_edit/{${NavArgs.ID}}"

    fun txDetail(id: Long) = "transaction/$id"
    fun txEdit(id: Long) = "transaction_edit/$id"
    fun accountDetail(id: Long) = "account/$id"
    fun accountEdit(id: Long) = "account_edit/$id"
    fun goalDetail(id: Long) = "goal/$id"
    fun goalEdit(id: Long) = "goal_edit/$id"
}

private data class BottomDestination(val route: String, val label: String, val icon: ImageVector)

private val bottomDestinations = listOf(
    BottomDestination(Routes.DASHBOARD, "Home", Icons.Filled.Home),
    BottomDestination(Routes.TRANSACTIONS, "History", Icons.AutoMirrored.Filled.List),
    BottomDestination(Routes.ACCOUNTS, "Accounts", Icons.Filled.AccountBalanceWallet),
    BottomDestination(Routes.BUDGETS, "Budgets", Icons.Filled.PieChart),
    BottomDestination(Routes.GOALS, "Goals", Icons.Filled.Savings)
)

private val idArgument = listOf(navArgument(NavArgs.ID) { type = NavType.LongType })

private fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Hosts every screen. The list-level ViewModels (dashboard, transactions,
 * accounts, budgets, goals) are created here, scoped to the Activity, and shared by all tabs, so a
 * change on one screen is reflected on the others straight from Room.
 * Screen-specific ViewModels (detail / form) are created inside each
 * destination and scoped to its back-stack entry, which is also where the
 * navigation argument reaches them through SavedStateHandle.
 */
@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val dashboardViewModel: DashboardViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val transactionsViewModel: TransactionsViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val accountsViewModel: AccountsViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val budgetsViewModel: BudgetsViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val goalsViewModel: GoalsViewModel = viewModel(factory = AppViewModelProvider.Factory)

    // One snackbar host for the whole app: feedback and "Undo" survive the screen that triggered them.
    val snackbarHostState = remember { SnackbarHostState() }
    val messenger = (LocalContext.current.applicationContext as TengeApp).container.messenger
    LaunchedEffect(messenger) {
        messenger.events.collect { event ->
            val result = snackbarHostState.showSnackbar(
                message = event.message,
                actionLabel = event.actionLabel,
                duration = if (event.actionLabel != null) SnackbarDuration.Long else SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) event.onAction?.invoke()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = backStackEntry?.destination
            NavigationBar {
                bottomDestinations.forEach { dest ->
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true,
                        onClick = { navController.switchTab(dest.route) },
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        label = { Text(dest.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.DASHBOARD,
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    goalsViewModel = goalsViewModel,
                    onSeeAllTransactions = { navController.switchTab(Routes.TRANSACTIONS) },
                    onOpenTransaction = { id -> navController.navigate(Routes.txDetail(id)) },
                    onOpenAccounts = { navController.switchTab(Routes.ACCOUNTS) },
                    onOpenBudgets = { navController.switchTab(Routes.BUDGETS) },
                    onOpenGoals = { navController.switchTab(Routes.GOALS) },
                    onAddTransaction = { navController.navigate(Routes.TX_NEW) }
                )
            }
            composable(Routes.TRANSACTIONS) {
                TransactionsScreen(
                    viewModel = transactionsViewModel,
                    onOpenTransaction = { id -> navController.navigate(Routes.txDetail(id)) },
                    onAddTransaction = { navController.navigate(Routes.TX_NEW) },
                    onOpenCategories = { navController.navigate(Routes.CATEGORIES) }
                )
            }
            composable(Routes.TX_NEW) {
                val formViewModel: TransactionFormViewModel = viewModel(factory = AppViewModelProvider.Factory)
                TransactionFormScreen(
                    viewModel = formViewModel,
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                    onCreateAccount = { navController.navigate(Routes.ACCOUNT_NEW) }
                )
            }
            composable(Routes.TX_DETAIL, arguments = idArgument) {
                val detailViewModel: TransactionDetailViewModel = viewModel(factory = AppViewModelProvider.Factory)
                TransactionDetailScreen(
                    viewModel = detailViewModel,
                    onBack = { navController.popBackStack() },
                    onEdit = { id -> navController.navigate(Routes.txEdit(id)) },
                    onDeleted = { navController.popBackStack() }
                )
            }
            composable(Routes.TX_EDIT, arguments = idArgument) {
                val formViewModel: TransactionFormViewModel = viewModel(factory = AppViewModelProvider.Factory)
                TransactionFormScreen(
                    viewModel = formViewModel,
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                    onCreateAccount = { navController.navigate(Routes.ACCOUNT_NEW) }
                )
            }
            composable(Routes.ACCOUNTS) {
                AccountsScreen(
                    viewModel = accountsViewModel,
                    onOpenAccount = { id -> navController.navigate(Routes.accountDetail(id)) },
                    onCreateAccount = { navController.navigate(Routes.ACCOUNT_NEW) }
                )
            }
            composable(Routes.ACCOUNT_NEW) {
                val formViewModel: AccountFormViewModel = viewModel(factory = AppViewModelProvider.Factory)
                AccountFormScreen(
                    viewModel = formViewModel,
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.ACCOUNT_DETAIL, arguments = idArgument) {
                val detailViewModel: AccountDetailViewModel = viewModel(factory = AppViewModelProvider.Factory)
                AccountDetailScreen(
                    viewModel = detailViewModel,
                    onBack = { navController.popBackStack() },
                    onEdit = { id -> navController.navigate(Routes.accountEdit(id)) },
                    onOpenTransaction = { id -> navController.navigate(Routes.txDetail(id)) },
                    onDeleted = { navController.popBackStack() }
                )
            }
            composable(Routes.ACCOUNT_EDIT, arguments = idArgument) {
                val formViewModel: AccountFormViewModel = viewModel(factory = AppViewModelProvider.Factory)
                AccountFormScreen(
                    viewModel = formViewModel,
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.BUDGETS) {
                BudgetsScreen(
                    viewModel = budgetsViewModel,
                    onOpenCategories = { navController.navigate(Routes.CATEGORIES) }
                )
            }
            composable(Routes.CATEGORIES) {
                val categoriesViewModel: CategoriesViewModel = viewModel(factory = AppViewModelProvider.Factory)
                CategoriesScreen(
                    viewModel = categoriesViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.GOALS) {
                GoalsScreen(
                    viewModel = goalsViewModel,
                    onOpenGoal = { id -> navController.navigate(Routes.goalDetail(id)) },
                    onCreateGoal = { navController.navigate(Routes.GOAL_NEW) }
                )
            }
            composable(Routes.GOAL_NEW) {
                val formViewModel: GoalFormViewModel = viewModel(factory = AppViewModelProvider.Factory)
                GoalFormScreen(
                    viewModel = formViewModel,
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.GOAL_DETAIL, arguments = idArgument) {
                val detailViewModel: GoalDetailViewModel = viewModel(factory = AppViewModelProvider.Factory)
                GoalDetailScreen(
                    viewModel = detailViewModel,
                    onBack = { navController.popBackStack() },
                    onEdit = { id -> navController.navigate(Routes.goalEdit(id)) },
                    onDeleted = { navController.popBackStack() }
                )
            }
            composable(Routes.GOAL_EDIT, arguments = idArgument) {
                val formViewModel: GoalFormViewModel = viewModel(factory = AppViewModelProvider.Factory)
                GoalFormScreen(
                    viewModel = formViewModel,
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
