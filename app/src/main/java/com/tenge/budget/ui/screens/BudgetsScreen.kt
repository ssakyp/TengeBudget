package com.tenge.budget.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tenge.budget.data.BudgetStatus
import com.tenge.budget.data.local.Category
import com.tenge.budget.ui.components.BudgetCard
import com.tenge.budget.ui.components.ChipOption
import com.tenge.budget.ui.components.ConfirmDeleteDialog
import com.tenge.budget.ui.components.EmptyState
import com.tenge.budget.ui.components.MonthSelector
import com.tenge.budget.ui.components.OptionChips
import com.tenge.budget.viewmodel.BudgetEditorState
import com.tenge.budget.viewmodel.BudgetsViewModel

/**
 * Monthly budgets per expense category with live progress. A budget bar moves
 * as soon as a matching expense is logged anywhere in the app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(viewModel: BudgetsViewModel, onOpenCategories: () -> Unit) {
    val month by viewModel.month.collectAsStateWithLifecycle()
    val budgets by viewModel.budgets.collectAsStateWithLifecycle()
    val categories by viewModel.expenseCategories.collectAsStateWithLifecycle()
    val editor by viewModel.editor.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<BudgetStatus?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budgets") },
                actions = {
                    IconButton(onClick = onOpenCategories) { Icon(Icons.Filled.Category, contentDescription = "Manage categories") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::openCreate) { Icon(Icons.Filled.Add, contentDescription = "New budget") }
        }
    ) { innerPadding ->
        val list = budgets
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = innerPadding.calculateTopPadding() + 4.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "month") { MonthSelector(month, onPrevious = viewModel::previousMonth, onNext = viewModel::nextMonth) }

            if (list != null && list.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        title = "No budgets this month",
                        message = "Set a spending limit for a category and watch it fill up as you log expenses.",
                        actionLabel = "Add budget",
                        onAction = viewModel::openCreate
                    )
                }
            } else if (list != null) {
                items(list, key = { it.budget.id }) { status ->
                    BudgetCard(
                        status = status,
                        onEdit = { viewModel.openEdit(status) },
                        onDelete = { pendingDelete = status }
                    )
                }
            }
        }
    }

    pendingDelete?.let { status ->
        ConfirmDeleteDialog(
            title = "Delete this budget?",
            message = "The budget for \"${status.categoryName}\" will be removed. You can undo right after.",
            onConfirm = {
                viewModel.delete(status)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }

    editor?.let { e ->
        BudgetEditorDialog(
            state = e,
            categories = categories,
            onCategoryChange = viewModel::onCategoryChange,
            onLimitChange = viewModel::onLimitChange,
            onSave = viewModel::save,
            onDismiss = viewModel::dismissEditor
        )
    }
}

@Composable
private fun BudgetEditorDialog(
    state: BudgetEditorState,
    categories: List<Category>,
    onCategoryChange: (Long) -> Unit,
    onLimitChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (state.id == null) "New budget" else "Edit budget") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Category", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (categories.isEmpty()) {
                    Text(
                        "No expense categories yet — add one under Categories first.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    OptionChips(
                        options = categories.map { ChipOption(it.id, it.name) },
                        selectedId = state.categoryId,
                        onSelect = { id -> if (id != null) onCategoryChange(id) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                state.categoryError?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                OutlinedTextField(
                    value = state.limit,
                    onValueChange = onLimitChange,
                    label = { Text("Monthly limit (₸)") },
                    isError = state.limitError != null,
                    supportingText = state.limitError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                state.error?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = { Button(onClick = onSave, enabled = !state.isSaving) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
