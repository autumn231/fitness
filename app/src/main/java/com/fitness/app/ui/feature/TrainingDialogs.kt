package com.fitness.app.ui.feature

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fitness.app.data.ExerciseRepository
import com.fitness.app.data.local.WorkoutSessionEntity
import com.fitness.app.data.local.WorkoutSetEntity
import com.fitness.app.i18n.displayName
import com.fitness.app.ui.common.BorderedCard
import com.fitness.app.ui.common.ExerciseCard
import com.fitness.app.ui.viewmodel.SessionSummary

@Composable
fun SessionSummaryDialog(
    summary: SessionSummary,
    canSaveAsPlan: Boolean,
    onSaveAsPlan: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var showNameInput by remember { mutableStateOf(false) }
    var planName by remember {
        mutableStateOf(
            "${summary.session.dateKey} 训练"
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("训练完成 🎉") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("时长：${formatClock(summary.durationSec)}")
                Text("总容量：${formatKg(summary.volumeKg)} kg")
                Text("完成组数：${summary.doneSets} / ${summary.totalSets} 组")
                summary.prs.forEach { pr ->
                    Text(
                        text = "🏆 $pr",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (canSaveAsPlan) {
                    if (showNameInput) {
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = planName,
                            onValueChange = { planName = it.take(30) },
                            label = { Text("计划名称") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        TextButton(onClick = { showNameInput = true }) {
                            Text("把这次训练存为计划模板")
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (showNameInput) {
                TextButton(
                    onClick = { if (planName.isNotBlank()) onSaveAsPlan(planName.trim()) },
                    enabled = planName.isNotBlank()
                ) { Text("保存计划") }
            } else {
                TextButton(onClick = onDismiss) { Text("完成") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

@Composable
fun HistoryRow(
    session: WorkoutSessionEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    BorderedCard(onClick = onClick) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.planName.ifBlank { "自由训练" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${session.dateKey} · 时长 ${session.durationSec / 60} 分钟",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "删除记录",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun HistoryDetailDialog(
    repo: ExerciseRepository,
    session: WorkoutSessionEntity,
    onDismiss: () -> Unit
) {
    var sets by remember { mutableStateOf<List<WorkoutSetEntity>>(emptyList()) }
    LaunchedEffect(session.id) { sets = repo.sessionSets(session.id) }
    val grouped = sets.groupBy { it.exerciseId }
        .entries.sortedBy { e -> e.value.minOf { it.sortOrder } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(session.planName.ifBlank { "自由训练" }) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${session.dateKey} · 时长 ${formatClock(session.durationSec)} · 总容量 ${
                        formatKg(sets.filter { it.done }.sumOf { it.weightKg * it.reps })
                    } kg",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                grouped.forEach { (_, exSets) ->
                    Column {
                        Text(
                            text = exSets.first().exerciseName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        exSets.sortedBy { it.setIndex }.forEach { s ->
                            Text(
                                text = "第 ${s.setIndex + 1} 组：${formatKg(s.weightKg)} kg × ${s.reps}${if (s.done) " ✓" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

@Composable
fun ExercisePickerDialog(
    repo: ExerciseRepository,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var addedNames by remember { mutableStateOf<List<String>>(emptyList()) }
    val results = remember(query) {
        val q = query.trim().lowercase()
        val all = repo.all()
        if (q.isBlank()) all.take(30)
        else all.filter {
            it.displayName().lowercase().contains(q) || it.name.lowercase().contains(q)
        }.take(30)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加动作") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("搜索动作") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                if (addedNames.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "已添加：${addedNames.joinToString("、")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier.heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(results, key = { it.id }) { ex ->
                        ExerciseCard(
                            exercise = ex,
                            onClick = {
                                onPick(ex.id)
                                addedNames = addedNames + ex.displayName()
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("完成") }
        }
    )
}
