package com.tenge.budget.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tenge.budget.ui.components.AccountCard
import com.tenge.budget.ui.components.EmptyState
import com.tenge.budget.util.formatMoney
import com.tenge.budget.viewmodel.AccountsViewModel

/** All accounts with their balances and a total per currency. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    viewModel: AccountsViewModel,
    onOpenAccount: (Long) -> Unit,
    onCreateAccount: () -> Unit
) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val totals by viewModel.totalsByCurrency.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Accounts") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateAccount) { Icon(Icons.Filled.Add, contentDescription = "New account") }
        }
    ) { innerPadding ->
        val list = accounts
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = innerPadding.calculateTopPadding() + 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (list != null && list.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        title = "No accounts yet",
                        message = "Add the places your money lives — a bank card, cash, a brokerage — then record transactions against them.",
                        actionLabel = "Create account",
                        onAction = onCreateAccount
                    )
                }
            } else if (list != null) {
                item(key = "totals") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Total balance", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            totals.forEach { (currency, sum) ->
                                Text(
                                    formatMoney(sum, currency),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                items(list, key = { it.id }) { account ->
                    AccountCard(account = account, onClick = { onOpenAccount(account.id) })
                }
            }
        }
    }
}
