package com.tenge.budget.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tenge.budget.data.TxType
import com.tenge.budget.ui.components.ConfirmDeleteDialog
import com.tenge.budget.ui.components.EmptyState
import com.tenge.budget.util.formatMoney
import com.tenge.budget.viewmodel.TransactionDetailUiState
import com.tenge.budget.viewmodel.TransactionDetailViewModel
import java.time.format.DateTimeFormatter

private val fullDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")

/**
 * Every field of one transaction, with Edit and Delete. The id arrives as a
 * Navigation Compose argument and is read by the ViewModel from SavedStateHandle.
 * Deleting reverses the transaction's effect on the account balance(s).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    viewModel: TransactionDetailViewModel,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onDeleted: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(viewModel) { viewModel.deleted.collect { onDeleted() } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transaction") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    val found = uiState as? TransactionDetailUiState.Found
                    if (found != null) {
                        IconButton(onClick = { onEdit(found.item.transaction.id) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit transaction")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        when (val state = uiState) {
            TransactionDetailUiState.Loading -> Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) { CircularProgressIndicator() }

            TransactionDetailUiState.NotFound -> EmptyState(
                title = "Transaction not found",
                message = "It may have been deleted.",
                modifier = Modifier.padding(innerPadding),
                actionLabel = "Go back",
                onAction = onBack
            )

            is TransactionDetailUiState.Found -> {
                val item = state.item
                val tx = item.transaction
                val amountColor = when (tx.type) {
                    TxType.INCOME -> MaterialTheme.colorScheme.primary
                    TxType.EXPENSE -> MaterialTheme.colorScheme.error
                    TxType.TRANSFER -> MaterialTheme.colorScheme.onSurface
                }
                val sign = when (tx.type) {
                    TxType.INCOME -> "+"
                    TxType.EXPENSE -> "-"
                    TxType.TRANSFER -> ""
                }

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
                            Text(tx.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(
                                "$sign${formatMoney(tx.amount, item.currency)}",
                                style = MaterialTheme.typography.displaySmall,
                                color = amountColor,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    DetailRow(label = "Type", value = tx.type.label)
                    if (tx.type == TxType.TRANSFER) {
                        DetailRow(label = "From account", value = item.accountName)
                        DetailRow(label = "To account", value = item.targetAccountName ?: "—")
                    } else {
                        DetailRow(label = "Account", value = item.accountName)
                        DetailRow(label = "Category", value = item.categoryName ?: "None")
                    }
                    DetailRow(label = "Date", value = tx.date.format(fullDateFormatter))
                    if (tx.note.isNotBlank()) {
                        DetailRow(label = "Note", value = tx.note)
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
                    ConfirmDeleteDialog(
                        title = "Delete this transaction?",
                        message = "\"${tx.title}\" will be removed and the account balance adjusted back. You can undo right after.",
                        onConfirm = {
                            showDeleteConfirm = false
                            viewModel.delete()
                        },
                        onDismiss = { showDeleteConfirm = false }
                    )
                }
            }
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
