package com.fitness.app.ui.feature

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitness.app.data.ExerciseRepository
import com.fitness.app.data.local.PlanItemEntity
import com.fitness.app.i18n.displayName
import com.fitness.app.i18n.subtitle
import com.fitness.app.ui.common.EmptyState
import com.fitness.app.ui.common.LocalAssetImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanDetailScreen(
    repo: ExerciseRepository,
    planId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onPick: () -> Unit,
    onOpenExercise: (String) -> Unit
) {
    val planState = repo.observePlan(planId).collectAsStateWithLifecycle(initialValue = null)
    val pw = planState.value
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(pw?.plan?.name ?: "训练计划", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = "编辑")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onPick,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("添加动作") }
            )
        }
    ) { inner ->
        if (pw == null) {
            Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) {
                Text("加载中…")
            }
            return
        }

        if (pw.items.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.FitnessCenter,
                title = "计划还没有动作",
                subtitle = "点击右下角按钮从动作库中挑选动作加入此计划",
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
                if (pw.plan.note.isNotBlank()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text(
                                text = pw.plan.note,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
                items(pw.items, key = { it.id }) { item ->
                    val ex = repo.byId(item.exerciseId)
                    PlanItemRow(
                        item = item,
                        exerciseName = ex?.displayName() ?: "未知动作",
                        exerciseSubtitle = ex?.subtitle() ?: "",
                        imagePath = ex?.image ?: "",
                        onClick = { ex?.let { onOpenExercise(it.id) } },
                        onDelete = {
                            scope.launch { repo.deleteItem(item.id) }
                        },
                        onUpdateSets = { newSets ->
                            scope.launch {
                                repo.updateItem(item.copy(sets = newSets.coerceAtLeast(1)))
                            }
                        },
                        onUpdateReps = { newReps ->
                            scope.launch {
                                repo.updateItem(item.copy(reps = newReps))
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlanItemRow(
    item: PlanItemEntity,
    exerciseName: String,
    exerciseSubtitle: String,
    imagePath: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onUpdateSets: (Int) -> Unit,
    onUpdateReps: (String) -> Unit
) {
    var setsText by remember(item.id, item.sets) { mutableStateOf(item.sets.toString()) }
    var repsText by remember(item.id, item.reps) { mutableStateOf(item.reps) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    if (imagePath.isNotBlank()) {
                        LocalAssetImage(
                            path = imagePath,
                            contentDescription = exerciseName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exerciseName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Text(
                        text = exerciseSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "移除",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = setsText,
                    onValueChange = { v ->
                        setsText = v.filter { it.isDigit() }
                        setsText.toIntOrNull()?.let(onUpdateSets)
                    },
                    label = { Text("组数") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = repsText,
                    onValueChange = { v ->
                        repsText = v
                        onUpdateReps(v)
                    },
                    label = { Text("次数/时长") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
