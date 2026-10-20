package com.tenge.budget.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tenge.budget.BudgetViewModel
import com.tenge.budget.ui.screens.AddTransactionScreen
import com.tenge.budget.ui.screens.DashboardScreen
import com.tenge.budget.ui.screens.TransactionDetailScreen
import com.tenge.budget.ui.screens.TransactionsScreen

private object Routes {
    const val DASHBOARD = "dashboard"
    const val TRANSACTIONS = "transactions"
    const val ADD = "add"
    const val DETAIL = "detail/{id}"
    fun detail(id: String) = "detail/$id"
}

private data class BottomDestination(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val bottomDestinations = listOf(
    BottomDestination(Routes.DASHBOARD, "Dashboard", Icons.Filled.Home),
    BottomDestination(Routes.TRANSACTIONS, "Transactions", Icons.Filled.List),
    BottomDestination(Routes.ADD, "Add", Icons.Filled.Add)
)

/**
 * Hosts all 4 screens behind one [BudgetViewModel] instance. Creating the
 * ViewModel here (with the default `viewModel()` factory, scoped to the
 * hosting Activity) means every screen below shares the exact same state —
 * adding a transaction on the Add screen is reflected on the Dashboard and
 * Transactions screen immediately, and the whole thing survives rotation
 * automatically because the ViewModel outlives the recreated Activity.
 */
@Composable
fun AppNavHost(viewModel: BudgetViewModel = viewModel()) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination
            NavigationBar {
                bottomDestinations.forEach { dest ->
                    NavigationBarItem(
                        selected = currentRoute?.hierarchy?.any { it.route == dest.route } == true,
                        onClick = {
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
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
                    viewModel = viewModel,
                    onSeeAllTransactions = {
                        navController.navigate(Routes.TRANSACTIONS) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    },
                    onOpenTransaction = { id -> navController.navigate(Routes.detail(id)) }
                )
            }
            composable(Routes.TRANSACTIONS) {
                TransactionsScreen(
                    viewModel = viewModel,
                    onOpenTransaction = { id -> navController.navigate(Routes.detail(id)) }
                )
            }
            composable(Routes.ADD) {
                AddTransactionScreen(
                    viewModel = viewModel,
                    onDone = {
                        navController.navigate(Routes.TRANSACTIONS) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Routes.DETAIL) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id").orEmpty()
                TransactionDetailScreen(
                    viewModel = viewModel,
                    transactionId = id,
                    onBack = { navController.popBackStack() },
                    onDeleted = { navController.popBackStack() }
                )
            }
        }
    }
}
