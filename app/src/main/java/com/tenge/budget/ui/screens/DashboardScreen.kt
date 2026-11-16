package com.tenge.budget.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tenge.budget.ui.components.BudgetProgressBar
import com.tenge.budget.ui.components.MonthSelector
import com.tenge.budget.ui.components.SummaryCard
import com.tenge.budget.ui.components.TransactionRow
import com.tenge.budget.util.formatTenge
import com.tenge.budget.viewmodel.DashboardViewModel
import com.tenge.budget.viewmodel.GoalsViewModel

/**
 * Landing screen: a month selector, that month's income / expenses / net, total
 * assets across accounts, the month's budgets rolled up, a goals summary and
 * recent activity. Everything is read from Room-backed ViewModel state, so
 * changes made on any other screen appear here instantly.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    goalsViewModel: GoalsViewModel,
    onSeeAllTransactions: () -> Unit,
    onOpenTransaction: (Long) -> Unit,
    onOpenAccounts: () -> Unit,
    onOpenBudgets: () -> Unit,
    onOpenGoals: () -> Unit,
    onAddTransaction: () -> Unit
) {
    val month by viewModel.month.collectAsStateWithLifecycle()
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val totalAssets by viewModel.totalAssets.collectAsStateWithLifecycle()
    val hasForeign by viewModel.hasForeignCurrencyAccounts.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val budgets by viewModel.budgetOverview.collectAsStateWithLifecycle()
    val recent by viewModel.recent.collectAsStateWithLifecycle()
    val goals by goalsViewModel.overview.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Tenge Budget") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTransaction) { Icon(Icons.Filled.Add, contentDescription = "Add transaction") }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = innerPadding.calculateTopPadding() + 4.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "month") { MonthSelector(month, onPrevious = viewModel::previousMonth, onNext = viewModel::nextMonth) }

            item(key = "summary") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryCard("Income", summary.income, MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                    SummaryCard("Expenses", summary.expenses, MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
                    SummaryCard(
                        "Net", summary.net,
                        if (summary.net >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item(key = "assets") {
                Card(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenAccounts),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Total assets", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        if (accounts.isEmpty()) {
                            Text(
                                "No accounts yet — tap to create one.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                formatTenge(totalAssets),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "${accounts.size} account(s)" + if (hasForeign) " · other currencies not included in totals" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item(key = "budgets") {
                if (budgets.budgetCount == 0) {
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenBudgets),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Budgets", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                "No budgets for this month — tap to set spending limits.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        BudgetProgressBar(
                            spent = budgets.totalSpent,
                            budget = budgets.totalLimit,
                            title = "Budgets (${budgets.budgetCount})",
                            modifier = Modifier.clickable(onClick = onOpenBudgets)
                        )
                        if (budgets.exceededCount > 0) {
                            Text(
                                "${budgets.exceededCount} budget(s) exceeded this month",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            item(key = "goals") {
                Card(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenGoals),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Savings goals", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        if (goals.activeCount + goals.pausedCount + goals.completedCount == 0) {
                            Text(
                                "No goals yet — tap to create your first one.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                "${formatTenge(goals.totalSaved)} saved of ${formatTenge(goals.totalTarget)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "${goals.activeCount} active · ${goals.completedCount} completed",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item(key = "recent-header") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Recent activity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    TextButton(onClick = onSeeAllTransactions) { Text("See all") }
                }
            }

            val recentItems = recent
            if (recentItems != null && recentItems.isEmpty()) {
                item(key = "recent-empty") {
                    Text(
                        "No transactions yet — tap + to add your first one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (recentItems != null) {
                items(recentItems, key = { it.transaction.id }) { item ->
                    TransactionRow(item = item, onClick = { onOpenTransaction(item.transaction.id) })
                }
            }
        }
    }
}
