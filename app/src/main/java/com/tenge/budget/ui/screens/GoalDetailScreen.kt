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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tenge.budget.data.ContributionKind
import com.tenge.budget.data.GoalStatus
import com.tenge.budget.ui.components.ConfirmDeleteDialog
import com.tenge.budget.ui.components.ContributionRow
import com.tenge.budget.ui.components.EmptyState
import com.tenge.budget.ui.components.GoalStatusBadge
import com.tenge.budget.util.formatTenge
import com.tenge.budget.viewmodel.ContributionDialogState
import com.tenge.budget.viewmodel.GoalDetailUiState
import com.tenge.budget.viewmodel.GoalDetailViewModel

/**
 * One savings goal: progress, the status workflow (active / paused / completed),
 * add-money and withdraw actions, statistics, and the full contribution history.
 * Buttons are enabled or disabled from the goal's status, mirroring the rules
 * that the repository enforces.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailScreen(
    viewModel: GoalDetailViewModel,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onDeleted: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dialog by viewModel.dialog.collectAsStateWithLifecycle()
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(viewModel) { viewModel.deleted.collect { onDeleted() } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Goal") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    val found = uiState as? GoalDetailUiState.Found
                    if (found != null) {
                        IconButton(onClick = { onEdit(found.goal.goal.id) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit goal")
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete goal", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        when (val state = uiState) {
            GoalDetailUiState.Loading -> Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) { CircularProgressIndicator() }

            GoalDetailUiState.NotFound -> EmptyState(
                title = "Goal not found",
                message = "It may have been deleted.",
                modifier = Modifier.padding(innerPadding),
                actionLabel = "Go back",
                onAction = onBack
            )

            is GoalDetailUiState.Found -> {
                val goal = state.goal
                val status = goal.goal.status
                val canDeposit = status == GoalStatus.ACTIVE
                val canWithdraw = status != GoalStatus.PAUSED && goal.savedAmount > 0.0

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp,
                        top = innerPadding.calculateTopPadding() + 12.dp, bottom = 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item(key = "progress") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        goal.goal.name,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                                    )
                                    GoalStatusBadge(status)
                                }
                                Text(
                                    formatTenge(goal.savedAmount),
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                LinearProgressIndicator(
                                    progress = { goal.fraction },
                                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                                Text(
                                    when (status) {
                                        GoalStatus.COMPLETED -> "Target of ${formatTenge(goal.goal.targetAmount)} reached 🎉"
                                        else -> "${(goal.fraction * 100).toInt()}% of ${formatTenge(goal.goal.targetAmount)} · ${formatTenge(goal.remaining)} to go"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    item(key = "actions") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { viewModel.openDialog(ContributionKind.DEPOSIT) },
                                    enabled = canDeposit,
                                    modifier = Modifier.weight(1f)
                                ) { Text("Add money") }
                                OutlinedButton(
                                    onClick = { viewModel.openDialog(ContributionKind.WITHDRAWAL) },
                                    enabled = canWithdraw,
                                    modifier = Modifier.weight(1f)
                                ) { Text("Withdraw") }
                            }
                            when (status) {
                                GoalStatus.ACTIVE -> TextButton(
                                    onClick = { viewModel.setPaused(true) },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Pause goal") }

                                GoalStatus.PAUSED -> {
                                    Text(
                                        "This goal is paused — deposits and withdrawals are locked.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    TextButton(
                                        onClick = { viewModel.setPaused(false) },
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text("Resume goal") }
                                }

                                GoalStatus.COMPLETED -> Text(
                                    "Completed. Withdraw money to reopen it, or raise the target by editing the goal.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    item(key = "stats") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Statistics", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                StatRow("Deposits", "${state.stats.depositCount} · ${formatTenge(state.stats.totalDeposited)}")
                                StatRow("Withdrawals", "${state.stats.withdrawalCount} · ${formatTenge(state.stats.totalWithdrawn)}")
                                StatRow("Largest deposit", formatTenge(state.stats.largestDeposit))
                                StatRow("Average deposit", formatTenge(state.stats.averageDeposit))
                                StatRow("Created", goal.goal.createdAt.toString())
                            }
                        }
                    }

                    item(key = "history-title") {
                        Text("History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    if (state.history.isEmpty()) {
                        item(key = "history-empty") {
                            Text(
                                "No activity yet — tap “Add money” to make your first deposit.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(state.history, key = { it.id }) { entry ->
                            Column {
                                ContributionRow(entry)
                                HorizontalDivider()
                            }
                        }
                    }
                }

                if (showDeleteConfirm) {
                    ConfirmDeleteDialog(
                        title = "Delete this goal?",
                        message = "\"${goal.goal.name}\" and its ${state.history.size} history entries will be permanently removed.",
                        onConfirm = {
                            showDeleteConfirm = false
                            viewModel.deleteGoal()
                        },
                        onDismiss = { showDeleteConfirm = false }
                    )
                }

                dialog?.let { d ->
                    ContributionDialog(
                        state = d,
                        hint = if (d.kind == ContributionKind.DEPOSIT) {
                            "Up to ${formatTenge(goal.remaining)}"
                        } else {
                            "Up to ${formatTenge(goal.savedAmount)}"
                        },
                        onAmountChange = viewModel::onDialogAmountChange,
                        onConfirm = viewModel::confirmDialog,
                        onDismiss = viewModel::dismissDialog
                    )
                }
            }
        }
    }
}

@Composable
private fun ContributionDialog(
    state: ContributionDialogState,
    hint: String,
    onAmountChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (state.kind == ContributionKind.DEPOSIT) "Add money" else "Withdraw money") },
        text = {
            OutlinedTextField(
                value = state.amount,
                onValueChange = onAmountChange,
                label = { Text("Amount (₸)") },
                isError = state.error != null,
                supportingText = { Text(state.error ?: hint) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = !state.isSubmitting) {
                Text(if (state.kind == ContributionKind.DEPOSIT) "Add" else "Withdraw")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
