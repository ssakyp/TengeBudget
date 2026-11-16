package com.tenge.budget.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tenge.budget.data.TxType
import com.tenge.budget.ui.components.ChipOption
import com.tenge.budget.ui.components.EmptyState
import com.tenge.budget.ui.components.OptionChips
import com.tenge.budget.ui.components.SelectableChip
import com.tenge.budget.viewmodel.TransactionFormViewModel
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val dateFieldFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")

/**
 * One form for creating and editing income, expense and transfer transactions.
 * The screen only renders [TransactionFormViewModel.state] and forwards events;
 * validation and saving happen in the ViewModel / repository.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormScreen(
    viewModel: TransactionFormViewModel,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    onCreateAccount: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(viewModel) { viewModel.saved.collect { onSaved() } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Edit transaction" else "Add transaction") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { innerPadding ->
        val accountList = accounts
        when {
            state.isLoading || accountList == null -> Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) { CircularProgressIndicator() }

            state.notFound -> EmptyState(
                title = "Transaction not found",
                message = "It may have been deleted.",
                modifier = Modifier.padding(innerPadding),
                actionLabel = "Go back",
                onAction = onBack
            )

            accountList.isEmpty() -> EmptyState(
                title = "Create an account first",
                message = "Every transaction belongs to an account, such as a bank card or cash.",
                modifier = Modifier.padding(innerPadding),
                actionLabel = "Create account",
                onAction = onCreateAccount
            )

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FieldLabel("Type")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TxType.entries.forEach { type ->
                        SelectableChip(type.label, state.type == type) { viewModel.onTypeChange(type) }
                    }
                }

                OutlinedTextField(
                    value = state.title,
                    onValueChange = viewModel::onTitleChange,
                    label = { Text("Title") },
                    isError = state.titleError != null,
                    supportingText = state.titleError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.amount,
                    onValueChange = viewModel::onAmountChange,
                    label = { Text("Amount") },
                    isError = state.amountError != null,
                    supportingText = state.amountError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                FieldLabel(if (state.type == TxType.TRANSFER) "From account" else "Account")
                OptionChips(
                    options = accountList.map { ChipOption(it.id, "${it.name} (${it.currency})") },
                    selectedId = state.accountId,
                    onSelect = { id -> if (id != null) viewModel.onAccountChange(id) }
                )
                state.accountError?.let { ErrorText(it) }

                if (state.type == TxType.TRANSFER) {
                    FieldLabel("To account")
                    OptionChips(
                        options = accountList.filter { it.id != state.accountId }.map { ChipOption(it.id, "${it.name} (${it.currency})") },
                        selectedId = state.targetAccountId,
                        onSelect = { id -> if (id != null) viewModel.onTargetAccountChange(id) }
                    )
                    state.targetError?.let { ErrorText(it) }
                } else {
                    FieldLabel("Category (optional)")
                    OptionChips(
                        options = listOf(ChipOption(null, "None")) +
                            categories.filter { it.type == state.type }.map { ChipOption(it.id, it.name) },
                        selectedId = state.categoryId,
                        onSelect = viewModel::onCategoryChange
                    )
                }

                FieldLabel("Date")
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(state.date.format(dateFieldFormatter))
                }

                OutlinedTextField(
                    value = state.note,
                    onValueChange = viewModel::onNoteChange,
                    label = { Text("Note (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                state.formError?.let { ErrorText(it) }

                Button(
                    onClick = viewModel::submit,
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (state.isEditing) "Save changes" else "Add transaction")
                }
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        viewModel.onDateChange(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ErrorText(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
}
