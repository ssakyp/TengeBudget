package com.tenge.budget.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tenge.budget.BudgetViewModel
import com.tenge.budget.data.TxType
import com.tenge.budget.ui.components.formatTenge
import java.time.format.DateTimeFormatter

private val fullDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")

/**
 * Shows every field of one transaction, reached by tapping a row on the
 * Dashboard or Transactions screen. Demonstrates passing a value through
 * Navigation Compose (the transaction's id as a path argument) and reading
 * it back out of the shared ViewModel rather than re-fetching or
 * duplicating the data.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    viewModel: BudgetViewModel,
    transactionId: String,
    onBack: () -> Unit,
    onDeleted: () -> Unit
) {
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val transaction = transactions.find { it.id == transactionId }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transaction") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (transaction == null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)
            ) {
                Text("This transaction was deleted.", style = MaterialTheme.typography.bodyMedium)
            }
            return@Scaffold
        }

        val amountColor = if (transaction.type == TxType.INCOME) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        val sign = if (transaction.type == TxType.INCOME) "+" else "-"

        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.Start) {
                    Text(transaction.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("$sign${formatTenge(transaction.amount)}", style = MaterialTheme.typography.displaySmall, color = amountColor, fontWeight = FontWeight.ExtraBold)
                }
            }

            DetailRow(label = "Type", value = if (transaction.type == TxType.INCOME) "Income" else "Expense")
            DetailRow(label = "Category", value = transaction.category.label)
            DetailRow(label = "Date", value = transaction.date.format(fullDateFormatter))
            if (transaction.note.isNotBlank()) {
                DetailRow(label = "Note", value = transaction.note)
            }

            OutlinedButton(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete transaction")
            }
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Delete this transaction?") },
                text = { Text("This can't be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteTransaction(transaction.id)
                            showDeleteConfirm = false
                            onDeleted()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
