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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tenge.budget.data.TxType
import com.tenge.budget.ui.components.ChipOption
import com.tenge.budget.ui.components.EmptyState
import com.tenge.budget.ui.components.OptionChips
import com.tenge.budget.ui.components.SelectableChip
import com.tenge.budget.ui.components.TransactionRow
import com.tenge.budget.viewmodel.TransactionsViewModel

/**
 * Full transaction list with search and filters (type, account, category).
 * The filters live in the ViewModel, so switching tabs or rotating the device
 * doesn't reset them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel,
    onOpenTransaction: (Long) -> Unit,
    onAddTransaction: () -> Unit,
    onOpenCategories: () -> Unit
) {
    val transactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val hasAny by viewModel.hasAnyTransactions.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val typeFilter by viewModel.typeFilter.collectAsStateWithLifecycle()
    val accountFilter by viewModel.accountFilter.collectAsStateWithLifecycle()
    val categoryFilter by viewModel.categoryFilter.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transactions") },
                actions = {
                    IconButton(onClick = onOpenCategories) { Icon(Icons.Filled.Category, contentDescription = "Manage categories") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTransaction) { Icon(Icons.Filled.Add, contentDescription = "Add transaction") }
        }
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
                    TxType.entries.forEach { type ->
                        SelectableChip(type.label, typeFilter == type) { viewModel.setTypeFilter(type) }
                    }
                }
                if (accounts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OptionChips(
                        options = listOf(ChipOption(null, "All accounts")) + accounts.map { ChipOption(it.id, it.name) },
                        selectedId = accountFilter,
                        onSelect = viewModel::setAccountFilter
                    )
                }
                if (categories.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OptionChips(
                        options = listOf(ChipOption(null, "All categories")) + categories.map { ChipOption(it.id, it.name) },
                        selectedId = categoryFilter,
                        onSelect = viewModel::setCategoryFilter
                    )
                }
            }

            val list = transactions
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (list != null && list.isEmpty()) {
                    item(key = "empty") {
                        if (hasAny == false) {
                            EmptyState(
                                title = "No transactions yet",
                                message = "Log your first income, expense or transfer to start tracking.",
                                actionLabel = "Add transaction",
                                onAction = onAddTransaction
                            )
                        } else {
                            EmptyState(
                                title = "Nothing matches",
                                message = "No transactions match your search or filters.",
                                actionLabel = "Clear filters",
                                onAction = viewModel::clearFilters
                            )
                        }
                    }
                } else if (list != null) {
                    items(list, key = { it.transaction.id }) { item ->
                        TransactionRow(item = item, onClick = { onOpenTransaction(item.transaction.id) })
                    }
                }
            }
        }
    }
}
