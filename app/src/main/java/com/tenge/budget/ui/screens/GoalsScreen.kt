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
import com.tenge.budget.ui.components.EmptyState
import com.tenge.budget.ui.components.GoalCard
import com.tenge.budget.util.formatTenge
import com.tenge.budget.viewmodel.GoalsViewModel

/** List of all savings goals with an overview of totals and a button to create a new one. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    viewModel: GoalsViewModel,
    onOpenGoal: (Long) -> Unit,
    onCreateGoal: () -> Unit
) {
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    val overview by viewModel.overview.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Savings goals") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateGoal) { Icon(Icons.Filled.Add, contentDescription = "New goal") }
        }
    ) { innerPadding ->
        val list = goals
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = innerPadding.calculateTopPadding() + 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (list != null && list.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        title = "No savings goals yet",
                        message = "Set a target, then add money towards it step by step and watch the progress.",
                        actionLabel = "Create a goal",
                        onAction = onCreateGoal
                    )
                }
            } else if (list != null) {
                item(key = "overview") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "${formatTenge(overview.totalSaved)} saved",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("${overview.activeCount} active", style = MaterialTheme.typography.bodySmall)
                                Text("${overview.pausedCount} paused", style = MaterialTheme.typography.bodySmall)
                                Text("${overview.completedCount} completed", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                items(list, key = { it.goal.id }) { goal ->
                    GoalCard(goal = goal, onClick = { onOpenGoal(goal.goal.id) })
                }
            }
        }
    }
}
