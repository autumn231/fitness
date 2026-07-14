package com.fitness.app.ui.feature

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitness.app.data.ExerciseRepository
import com.fitness.app.data.local.PlanWithItems
import com.fitness.app.ui.common.EmptyState
import com.fitness.app.ui.nav.Destinations

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanScreen(
    repo: ExerciseRepository,
    onNavigate: (String) -> Unit
) {
    val plansState = repo.observePlans().collectAsStateWithLifecycle(initialValue = emptyList())
    val plans = plansState.value

    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text("训练计划", fontWeight = FontWeight.SemiBold) }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigate(Destinations.PlanEditor.create(0L)) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("新建计划") }
            )
        }
    ) { inner ->
        if (plans.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.FitnessCenter,
                title = "还没有训练计划",
                subtitle = "点击右下角按钮新建一个属于你的训练计划",
                modifier = Modifier.padding(inner)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(plans, key = { it.plan.id }) { pw ->
                    PlanRow(planWithItems = pw) {
                        onNavigate(Destinations.PlanDetail.create(pw.plan.id))
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanRow(planWithItems: PlanWithItems, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = planWithItems.plan.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (planWithItems.plan.note.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = planWithItems.plan.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.FitnessCenter,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 6.dp)
                )
                Text(
                    text = "共 ${planWithItems.items.size} 个动作",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
