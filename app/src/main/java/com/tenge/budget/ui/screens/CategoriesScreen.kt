package com.tenge.budget.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tenge.budget.data.TxType
import com.tenge.budget.data.local.Category
import com.tenge.budget.ui.components.ConfirmDeleteDialog
import com.tenge.budget.ui.components.EmptyState
import com.tenge.budget.ui.components.SelectableChip
import com.tenge.budget.viewmodel.CategoriesViewModel
import com.tenge.budget.viewmodel.CategoryEditorState

/**
 * Manage income and expense categories. Create / edit happen in a dialog
 * whose state lives in the ViewModel; categories in use can't be deleted or
 * change type (the repository explains why).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(viewModel: CategoriesViewModel, onBack: () -> Unit) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val editor by viewModel.editor.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<Category?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categories") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::openCreate) { Icon(Icons.Filled.Add, contentDescription = "New category") }
        }
    ) { innerPadding ->
        val list = categories
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = innerPadding.calculateTopPadding() + 8.dp, bottom = 96.dp)
        ) {
            if (list != null && list.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        title = "No categories yet",
                        message = "Categories group your income and expenses and are what budgets are set on.",
                        actionLabel = "Add category",
                        onAction = viewModel::openCreate
                    )
                }
            } else if (list != null) {
                listOf(TxType.EXPENSE, TxType.INCOME).forEach { type ->
                    val group = list.filter { it.type == type }
                    item(key = "header-${type.name}") {
                        Text(
                            "${type.label} categories",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                        )
                    }
                    if (group.isEmpty()) {
                        item(key = "none-${type.name}") {
                            Text(
                                "None yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    items(group, key = { it.id }) { category ->
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(category.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                IconButton(onClick = { viewModel.openEdit(category) }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Edit ${category.name}")
                                }
                                IconButton(onClick = { pendingDelete = category }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete ${category.name}", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { category ->
        ConfirmDeleteDialog(
            title = "Delete category?",
            message = "\"${category.name}\" will be removed. Categories used by transactions or budgets can't be deleted.",
            onConfirm = {
                viewModel.delete(category)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }

    editor?.let { e ->
        CategoryEditorDialog(
            state = e,
            onNameChange = viewModel::onNameChange,
            onTypeChange = viewModel::onTypeChange,
            onSave = viewModel::save,
            onDismiss = viewModel::dismissEditor
        )
    }
}

@Composable
private fun CategoryEditorDialog(
    state: CategoryEditorState,
    onNameChange: (String) -> Unit,
    onTypeChange: (TxType) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (state.id == null) "New category" else "Edit category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = onNameChange,
                    label = { Text("Name") },
                    isError = state.nameError != null,
                    supportingText = state.nameError?.let { { Text(it) } },
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SelectableChip("Expense", state.type == TxType.EXPENSE) { onTypeChange(TxType.EXPENSE) }
                    SelectableChip("Income", state.type == TxType.INCOME) { onTypeChange(TxType.INCOME) }
                }
                state.error?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = { Button(onClick = onSave, enabled = !state.isSaving) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
