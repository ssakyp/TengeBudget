package com.tenge.budget.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tenge.budget.BudgetViewModel
import com.tenge.budget.data.Category
import com.tenge.budget.data.TxType
import com.tenge.budget.ui.components.SelectableChip

/**
 * Standalone "Add" form reached from the bottom nav. Its draft fields use
 * `rememberSaveable` — they should survive rotation (so a half-typed entry
 * isn't lost if the phone turns) but have no reason to live in the
 * ViewModel before the person actually submits, which is exactly the
 * use case `rememberSaveable` (as opposed to `remember` or [BudgetViewModel])
 * is for. The validation error message is transient UI feedback, so it
 * only needs plain `remember`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(viewModel: BudgetViewModel, onDone: () -> Unit) {
    var title by rememberSaveable { mutableStateOf("") }
    var amountText by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf(TxType.EXPENSE) }
    var category by rememberSaveable { mutableStateOf(Category.FOOD) }
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Add transaction") }) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it; error = null },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' }; error = null },
                label = { Text("Amount (₸)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Text("Type", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row {
                SelectableChip("Income", type == TxType.INCOME, modifier = Modifier.padding(end = 8.dp)) { type = TxType.INCOME }
                SelectableChip("Expense", type == TxType.EXPENSE) { type = TxType.EXPENSE }
            }

            Text("Category", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            CategoryChipGrid(selected = category, onSelected = { category = it; type = it.defaultType })

            if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    when {
                        title.isBlank() -> error = "Enter a title."
                        amount == null || amount <= 0.0 -> error = "Enter a valid amount."
                        else -> {
                            viewModel.addTransaction(title.trim(), amount, type, category, note.trim())
                            title = ""
                            amountText = ""
                            note = ""
                            error = null
                            onDone()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add Transaction")
            }
        }
    }
}

@Composable
private fun CategoryChipGrid(selected: Category, onSelected: (Category) -> Unit) {
    val entries = Category.entries.toList()
    val rows = entries.chunked(4)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { c -> SelectableChip(c.label, selected == c, onClick = { onSelected(c) }) }
            }
        }
    }
}
