package com.tenge.budget.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.tenge.budget.BudgetViewModel
import com.tenge.budget.ui.components.BudgetProgressBar
import com.tenge.budget.ui.components.SummaryCard
import com.tenge.budget.ui.components.TransactionRow

private val weight: Any
    get() {
        TODO()
    }

/**
 * Landing screen: the three totals, the monthly-budget progress feature,
 * and a short preview of the most recent activity. Everything here is read
 * from [BudgetViewModel] via `collectAsStateWithLifecycle` — the screen
 * itself holds no state of its own.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: BudgetViewModel,
    onSeeAllTransactions: () -> Unit,
    onOpenTransaction: (String) -> Unit
) {
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val recent by viewModel.recentTransactions.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Tenge Budget") }) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = innerPadding.calculateTopPadding() + 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryCard("Balance", summary.balance, MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                    SummaryCard("Income", summary.income, MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                    SummaryCard("Expense", summary.expense, MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
                }
            }

            item { BudgetProgressBar(spent = summary.expense, budget = viewModel.monthlyBudget) }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Recent activity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    TextButton(onClick = onSeeAllTransactions) { Text("See all") }
                }
            }

            if (recent.isEmpty()) {
                item {
                    Text(
                        "No transactions yet — add your first one from the Add tab.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(recent, key = { it.id }) { tx ->
                    TransactionRow(transaction = tx, onClick = { onOpenTransaction(tx.id) })
                }
            }
        }
    }
}
