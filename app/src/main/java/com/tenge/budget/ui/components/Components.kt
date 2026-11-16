package com.tenge.budget.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tenge.budget.data.ContributionKind
import com.tenge.budget.data.GoalStatus
import com.tenge.budget.data.TxType
import com.tenge.budget.data.local.Contribution
import com.tenge.budget.data.local.GoalWithProgress
import com.tenge.budget.data.local.TransactionWithInfo
import com.tenge.budget.util.formatMoney
import com.tenge.budget.util.formatTenge
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")

/** One totals tile (balance / income / expense). Stateless; reused three times on the Dashboard. */
@Composable
fun SummaryCard(label: String, amount: Double, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatTenge(amount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

/**
 * A single tappable chip for picking one option out of a short list —
 * reused by the type filter, the category filter, and the type/category
 * pickers on the Add screen.
 */
@Composable
fun SelectableChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    FilterChip(
        modifier = modifier,
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

/**
 * One row in a transaction list, reused on the Dashboard, the Transactions
 * screen and an account's history. The amount is shown in the account's
 * currency; transfers show "from → to" and are not colored as gain/loss.
 */
@Composable
fun TransactionRow(item: TransactionWithInfo, onClick: () -> Unit, modifier: Modifier = Modifier) {
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
    val subtitle = when (tx.type) {
        TxType.TRANSFER -> "${item.accountName} → ${item.targetAccountName ?: "?"}"
        else -> "${item.accountName} · ${item.categoryName ?: "No category"}"
    } + " · ${tx.date.format(dateFormatter)}"

    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(tx.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                "$sign${formatMoney(tx.amount, item.currency)}",
                style = MaterialTheme.typography.bodyMedium,
                color = amountColor,
                fontWeight = FontWeight.SemiBold
            )
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * Progress bar for the monthly budget goal, shown on the Dashboard. The
 * over-budget color flip is the one bit of "business logic" baked into a
 * UI component — intentional, since it's purely a presentation rule (how
 * to color a percentage), not application state.
 */
@Composable
fun BudgetProgressBar(spent: Double, budget: Double, modifier: Modifier = Modifier, title: String = "Monthly budget") {
    val fraction = if (budget <= 0.0) 0f else (spent / budget).toFloat().coerceIn(0f, 1f)
    val overBudget = spent > budget
    val barColor = if (overBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "${formatTenge(spent)} / ${formatTenge(budget)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = barColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                if (overBudget) "Over budget by ${formatTenge(spent - budget)}" else "${(fraction * 100).toInt()}% of the budget used",
                style = MaterialTheme.typography.labelSmall,
                color = if (overBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Centered message used whenever a list has nothing to show. Optionally offers one action. */
@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

/** Reusable "are you sure?" dialog for every destructive action in the app. */
@Composable
fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Delete") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** Small colored label for a goal's status. */
@Composable
fun GoalStatusBadge(status: GoalStatus, modifier: Modifier = Modifier) {
    val (container, content) = when (status) {
        GoalStatus.ACTIVE -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        GoalStatus.PAUSED -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        GoalStatus.COMPLETED -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
    }
    Text(
        status.label,
        modifier = modifier.clip(RoundedCornerShape(50)).background(container).padding(horizontal = 10.dp, vertical = 3.dp),
        style = MaterialTheme.typography.labelSmall,
        color = content,
        fontWeight = FontWeight.SemiBold
    )
}

/** One goal in the Goals list: name, status, progress bar, and saved/target amounts. Stateless. */
@Composable
fun GoalCard(goal: GoalWithProgress, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    goal.goal.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                GoalStatusBadge(goal.goal.status)
            }
            LinearProgressIndicator(
                progress = { goal.fraction },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "${formatTenge(goal.savedAmount)} of ${formatTenge(goal.goal.targetAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${(goal.fraction * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private val historyFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm")

/** One line of a goal's history (deposit or withdrawal). */
@Composable
fun ContributionRow(contribution: Contribution, modifier: Modifier = Modifier) {
    val isDeposit = contribution.kind == ContributionKind.DEPOSIT
    val color = if (isDeposit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val whenText = Instant.ofEpochMilli(contribution.timestamp).atZone(ZoneId.systemDefault()).format(historyFormatter)
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(contribution.kind.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(whenText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            "${if (isDeposit) "+" else "-"}${formatTenge(contribution.amount)}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}
