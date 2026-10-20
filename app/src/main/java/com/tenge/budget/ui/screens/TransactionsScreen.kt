package com.tenge.budget.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tenge.budget.BudgetViewModel
import com.tenge.budget.data.Category
import com.tenge.budget.data.TxType
import com.tenge.budget.ui.components.SelectableChip
import com.tenge.budget.ui.components.TransactionRow

private val weight: Any
    get() {
        TODO()
    }

/**
 * Full transaction list with search and filtering — the screen that makes
 * the two "meaningful interaction" requirements (searching, filtering)
 * concrete. Search text and the active filters live in the ViewModel, not
 * local state, so switching tabs and coming back (or rotating the device)
 * doesn't reset them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(viewModel: BudgetViewModel, onOpenTransaction: (String) -> Unit) {
    val transactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val typeFilter by viewModel.typeFilter.collectAsStateWithLifecycle()
    val categoryFilter by viewModel.categoryFilter.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Transactions") }) }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(top = innerPadding.calculateTopPadding())) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = viewModel::setQuery,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search by title…") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SelectableChip("All", typeFilter == null) { viewModel.setTypeFilter(null) }
                    SelectableChip("Income", typeFilter == TxType.INCOME) { viewModel.setTypeFilter(TxType.INCOME) }
                    SelectableChip("Expense", typeFilter == TxType.EXPENSE) { viewModel.setTypeFilter(TxType.EXPENSE) }
                }
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        SelectableChip("All categories", categoryFilter == null) { viewModel.setCategoryFilter(null) }
                    }
                    items(Category.entries.toList()) { c ->
                        SelectableChip(c.label, categoryFilter == c) { viewModel.setCategoryFilter(c) }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (transactions.isEmpty()) {
                    item {
                        Text(
                            "No transactions match your filters.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 24.dp)
                        )
                    }
                } else {
                    items(transactions, key = { it.id }) { tx ->
                        TransactionRow(transaction = tx, onClick = { onOpenTransaction(tx.id) })
                    }
                }
            }
        }
    }
}
