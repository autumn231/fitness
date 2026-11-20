package com.fitness.app.ui.feature

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fitness.app.data.ExerciseRepository
import com.fitness.app.i18n.displayName
import com.fitness.app.i18n.subtitle
import com.fitness.app.ui.common.EmptyState
import com.fitness.app.ui.nav.Destinations
import com.fitness.app.ui.viewmodel.TrainingViewModel
import com.fitness.app.ui.viewmodel.repoViewModelFactory

/**
 * 训练页（Strong 式组表执行）：
 * - 无进行中会话：选计划 → 开始训练（或空白训练），下方是训练历史
 * - 有进行中会话：按动作分组的组表——上次成绩内联、重量/次数预填上次值、
 *   打勾即落库并自动开始组间歇；会话存数据库，切 Tab/杀进程都能恢复
 * - 完成训练出总结页（时长/总容量/完成组数/PR 高亮），自由训练可反存为计划
 *
 * 状态与计时在 TrainingViewModel（第 3 期迁出），本文件只做渲染与事件转发；
 * 组表/计时卡/对话框分别在 TrainingSetTable / TrainingCards / TrainingDialogs。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingScreen(
    repo: ExerciseRepository,
    onNavigate: (String) -> Unit
) {
    val appContext = LocalContext.current.applicationContext
    val vm: TrainingViewModel = viewModel(
        factory = repoViewModelFactory { TrainingViewModel(repo, appContext) }
    )

    val plans by vm.plans.collectAsStateWithLifecycle()
    val session = vm.activeSession.collectAsStateWithLifecycle().value
    val allSessions by vm.allSessions.collectAsStateWithLifecycle()
    val sets by vm.sets.collectAsStateWithLifecycle()
    val prevMap by vm.prevMap.collectAsStateWithLifecycle()
    val selectedPlanId by vm.selectedPlanId.collectAsStateWithLifecycle()
    val totalSeconds by vm.totalSeconds.collectAsStateWithLifecycle()
    val timerRunning by vm.timerRunning.collectAsStateWithLifecycle()
    val restTotal by vm.restTotal.collectAsStateWithLifecycle()
    val restRemaining by vm.restRemaining.collectAsStateWithLifecycle()
    val restRunning by vm.restRunning.collectAsStateWithLifecycle()
    val showPicker by vm.showPicker.collectAsStateWithLifecycle()
    val summary by vm.summary.collectAsStateWithLifecycle()
    val historyDetail by vm.historyDetail.collectAsStateWithLifecycle()
    val confirmAbandon by vm.confirmAbandon.collectAsStateWithLifecycle()
    val confirmDeleteHistory by vm.confirmDeleteHistory.collectAsStateWithLifecycle()

    val currentPlan = plans.firstOrNull { it.plan.id == selectedPlanId }

    val groupedSets = remember(sets) {
        sets.groupBy { it.exerciseId }
            .entries.sortedBy { e -> e.value.minOf { it.sortOrder } }
            .map { (exerciseId, list) -> exerciseId to list.sortedBy { it.setIndex } }
    }
    val doneCount = sets.count { it.done }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("训练", fontWeight = FontWeight.SemiBold) },
                actions = {
                    TextButton(onClick = { onNavigate(Destinations.Plans.route) }) {
                        Text("管理计划")
                    }
                }
            )
        }
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (session == null) {
                // ---------- 未开始：选计划 + 开始 ----------
                item {
                    PlanSelector(
                        plans = plans,
                        current = currentPlan,
                        onSelect = { vm.selectPlan(it.plan.id) }
                    )
                }
                if (currentPlan == null) {
                    item {
                        EmptyState(
                            icon = Icons.Filled.PlayArrow,
                            title = "还没有可选的计划",
                            subtitle = "可以先创建计划，也可以直接开始一次空白训练",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    item {
                        PlanPreviewCard(
                            plan = currentPlan,
                            exerciseName = { repo.byId(it)?.displayName() ?: "未知动作" },
                            exerciseSub = { repo.byId(it)?.subtitle() ?: "" }
                        )
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { vm.startFromPlan() },
                            enabled = currentPlan != null && currentPlan.items.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("按计划开始训练")
                        }
                        OutlinedButton(
                            onClick = { vm.startFree() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("空白训练（自由加动作）")
                        }
                    }
                }

                // ---------- 历史 ----------
                val history = allSessions.filter { it.endedAt != null }
                item {
                    Text(
                        text = "训练历史",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                if (history.isEmpty()) {
                    item {
                        Text(
                            text = "还没有完成的训练。完成第一次训练后，这里会留下记录。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(history, key = { it.id }) { s ->
                        HistoryRow(
                            session = s,
                            onClick = { vm.openHistoryDetail(s) },
                            onDelete = { vm.requestDeleteHistory(s) }
                        )
                    }
                }
            } else {
                // ---------- 进行中：组表执行 ----------
                item {
                    TotalTimeCard(
                        totalSeconds = totalSeconds,
                        running = timerRunning,
                        completed = doneCount,
                        total = sets.size,
                        unitLabel = "组",
                        onToggle = { vm.toggleTimer() },
                        onReset = { vm.resetTimer() }
                    )
                }
                item {
                    RestTimerCard(
                        total = restTotal,
                        remaining = restRemaining,
                        running = restRunning,
                        onPreset = { vm.restPreset(it) },
                        onToggle = { vm.toggleRest() },
                        onReset = { vm.resetRest() }
                    )
                }

                if (groupedSets.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Filled.Add,
                            title = "还没有动作",
                            subtitle = "点下方「添加动作」开始记录",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                groupedSets.forEach { (exerciseId, exSets) ->
                    item(key = "ex_$exerciseId") {
                        val ex = repo.byId(exerciseId)
                        ExerciseSetCard(
                            name = ex?.displayName() ?: exSets.first().exerciseName,
                            subtitle = ex?.subtitle() ?: "",
                            imagePath = ex?.image ?: "",
                            sets = exSets,
                            previous = prevMap[exerciseId] ?: emptyList(),
                            onOpenDetail = {
                                onNavigate(Destinations.Exercise.create(exerciseId))
                            },
                            onUpdateSet = { vm.updateSet(it) },
                            onToggleDone = { vm.toggleDone(it) },
                            onAddSet = { vm.addSet(exerciseId) },
                            onRemoveExercise = { vm.removeExercise(exerciseId) }
                        )
                    }
                }

                item {
                    OutlinedButton(
                        onClick = { vm.setShowPicker(true) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("添加动作")
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { vm.finish() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("完成训练")
                        }
                        TextButton(
                            onClick = { vm.setConfirmAbandon(true) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("放弃这次训练", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    // ---- 添加动作 ----
    if (showPicker && session != null) {
        ExercisePickerDialog(
            repo = repo,
            onDismiss = { vm.setShowPicker(false) },
            onPick = { exerciseId -> vm.addExercise(exerciseId) }
        )
    }

    // ---- 完成总结 ----
    summary?.let { s ->
        SessionSummaryDialog(
            summary = s,
            canSaveAsPlan = s.session.planId == null,
            onSaveAsPlan = { name -> vm.saveSummaryAsPlan(name) },
            onDismiss = { vm.dismissSummary() }
        )
    }

    // ---- 历史详情 ----
    historyDetail?.let { s ->
        HistoryDetailDialog(
            repo = repo,
            session = s,
            onDismiss = { vm.dismissHistoryDetail() }
        )
    }

    if (confirmAbandon && session != null) {
        AlertDialog(
            onDismissRequest = { vm.setConfirmAbandon(false) },
            title = { Text("放弃这次训练？") },
            text = { Text("这次训练的全部组记录都会删除，无法恢复。") },
            confirmButton = {
                TextButton(onClick = { vm.confirmAbandonSession() }) {
                    Text("放弃", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { vm.setConfirmAbandon(false) }) { Text("继续训练") }
            }
        )
    }

    confirmDeleteHistory?.let { s ->
        AlertDialog(
            onDismissRequest = { vm.dismissDeleteHistory() },
            title = { Text("删除这条训练记录？") },
            text = { Text("删除后，这次训练的组记录与日历完成标记都会消失。") },
            confirmButton = {
                TextButton(onClick = { vm.confirmDeleteHistorySession() }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { vm.dismissDeleteHistory() }) { Text("取消") }
            }
        )
    }
}
