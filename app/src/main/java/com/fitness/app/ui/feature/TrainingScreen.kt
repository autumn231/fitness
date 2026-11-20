package com.fitness.app.ui.feature

import android.content.Context
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitness.app.data.ExerciseRepository
import com.fitness.app.data.local.PlanWithItems
import com.fitness.app.data.local.WorkoutSessionEntity
import com.fitness.app.data.local.WorkoutSetEntity
import com.fitness.app.i18n.displayName
import com.fitness.app.i18n.subtitle
import com.fitness.app.ui.common.EmptyState
import com.fitness.app.ui.common.ExerciseCard
import com.fitness.app.ui.common.LocalAssetImage
import com.fitness.app.ui.nav.Destinations
import com.fitness.app.ui.theme.CardShape
import com.fitness.app.ui.theme.ImageShape
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 训练页（Strong 式组表执行）：
 * - 无进行中会话：选计划 → 开始训练（或空白训练），下方是训练历史
 * - 有进行中会话：按动作分组的组表——上次成绩内联、重量/次数预填上次值、
 *   打勾即落库并自动开始组间歇；会话存数据库，切 Tab/杀进程都能恢复
 * - 完成训练出总结页（时长/总容量/完成组数/PR 高亮），自由训练可反存为计划
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingScreen(
    repo: ExerciseRepository,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val plans by repo.observePlans().collectAsStateWithLifecycle(initialValue = emptyList())
    val activeSession by repo.observeActiveSession()
        .collectAsStateWithLifecycle(initialValue = null)
    val allSessions by repo.observeSessions().collectAsStateWithLifecycle(initialValue = emptyList())

    var selectedPlanId by rememberSaveable { mutableLongStateOf(0L) }
    LaunchedEffect(plans) {
        val requested = repo.consumeRequestedPlan()
        when {
            requested != null && plans.any { it.plan.id == requested } -> selectedPlanId = requested
            plans.isNotEmpty() && plans.none { it.plan.id == selectedPlanId } ->
                selectedPlanId = plans.first().plan.id
        }
    }
    val currentPlan = plans.firstOrNull { it.plan.id == selectedPlanId }

    val session = activeSession
    val setsFlow = remember(session?.id) { repo.observeSets(session?.id ?: -1L) }
    val sets by setsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    // Previous 数据：会话内每个动作的上次组记录
    var prevMap by remember { mutableStateOf<Map<String, List<WorkoutSetEntity>>>(emptyMap()) }
    LaunchedEffect(session?.id, sets.map { it.exerciseId }.distinct()) {
        val s = session ?: return@LaunchedEffect
        val ids = sets.map { it.exerciseId }.distinct()
        if (ids.isEmpty()) return@LaunchedEffect
        val map = mutableMapOf<String, List<WorkoutSetEntity>>()
        ids.forEach { id -> map[id] = repo.previousSets(id, s.id) }
        prevMap = map
    }

    // ---- 总用时（绑定会话：从 startedAt 初始化，暂停/继续沿用旧机制） ----
    var timerRunning by rememberSaveable { mutableStateOf(false) }
    var accumulated by rememberSaveable { mutableLongStateOf(0L) }
    var startedAtUi by rememberSaveable { mutableLongStateOf(0L) }
    var totalSeconds by rememberSaveable { mutableLongStateOf(0L) }
    var timerSessionId by rememberSaveable { mutableLongStateOf(-1L) }

    LaunchedEffect(session?.id) {
        val s = session
        if (s != null && timerSessionId != s.id) {
            timerSessionId = s.id
            accumulated = ((System.currentTimeMillis() - s.startedAt) / 1000).coerceAtLeast(0)
            totalSeconds = accumulated
            startedAtUi = SystemClock.elapsedRealtime()
            timerRunning = true
        }
        if (s == null) {
            timerSessionId = -1L
            timerRunning = false
            accumulated = 0L
            totalSeconds = 0L
        }
    }
    LaunchedEffect(timerRunning) {
        if (!timerRunning) return@LaunchedEffect
        while (timerRunning) {
            totalSeconds = accumulated + (SystemClock.elapsedRealtime() - startedAtUi) / 1000
            delay(200L)
        }
    }

    // ---- 组间歇 ----
    var restTotal by rememberSaveable { mutableIntStateOf(90) }
    var restRemaining by rememberSaveable { mutableIntStateOf(0) }
    var restRunning by rememberSaveable { mutableStateOf(false) }
    var restEndAt by rememberSaveable { mutableLongStateOf(0L) }
    LaunchedEffect(restRunning, restEndAt) {
        if (!restRunning) return@LaunchedEffect
        while (restRunning) {
            val remainMs = restEndAt - SystemClock.elapsedRealtime()
            restRemaining = ((remainMs + 999) / 1000).toInt().coerceAtLeast(0)
            if (remainMs <= 0L) {
                restRunning = false
                runCatching { vibrateOnce(context) }
                break
            }
            delay(200L)
        }
    }
    fun startRest() {
        restRemaining = restTotal
        restEndAt = SystemClock.elapsedRealtime() + restTotal * 1000L
        restRunning = true
    }

    // ---- 对话框状态 ----
    var showPicker by remember { mutableStateOf(false) }
    var summary by remember { mutableStateOf<SessionSummary?>(null) }
    var historyDetail by remember { mutableStateOf<WorkoutSessionEntity?>(null) }
    var confirmAbandon by remember { mutableStateOf(false) }
    var confirmDeleteHistory by remember { mutableStateOf<WorkoutSessionEntity?>(null) }

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
                        onSelect = { selectedPlanId = it.plan.id }
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
                            onClick = {
                                val plan = currentPlan ?: return@Button
                                scope.launch { repo.startSessionFromPlan(plan) }
                            },
                            enabled = currentPlan != null && currentPlan.items.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("按计划开始训练")
                        }
                        OutlinedButton(
                            onClick = { scope.launch { repo.startFreeSession() } },
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
                            onClick = { historyDetail = s },
                            onDelete = { confirmDeleteHistory = s }
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
                        onToggle = {
                            if (timerRunning) {
                                accumulated = totalSeconds
                                timerRunning = false
                            } else {
                                startedAtUi = SystemClock.elapsedRealtime()
                                timerRunning = true
                            }
                        },
                        onReset = {
                            accumulated =
                                ((System.currentTimeMillis() - session.startedAt) / 1000)
                                    .coerceAtLeast(0)
                            totalSeconds = accumulated
                            startedAtUi = SystemClock.elapsedRealtime()
                            timerRunning = true
                        }
                    )
                }
                item {
                    RestTimerCard(
                        total = restTotal,
                        remaining = restRemaining,
                        running = restRunning,
                        onPreset = { secs ->
                            restTotal = secs
                            restRemaining = secs
                            restEndAt = SystemClock.elapsedRealtime() + secs * 1000L
                            restRunning = true
                        },
                        onToggle = {
                            if (restRunning) {
                                restRunning = false
                            } else {
                                if (restRemaining <= 0) restRemaining = restTotal
                                restEndAt = SystemClock.elapsedRealtime() + restRemaining * 1000L
                                restRunning = true
                            }
                        },
                        onReset = {
                            restRunning = false
                            restRemaining = 0
                        }
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
                            onUpdateSet = { updated ->
                                scope.launch { repo.updateSet(updated) }
                            },
                            onToggleDone = { set ->
                                scope.launch {
                                    repo.updateSet(set.copy(done = !set.done))
                                    if (!set.done) startRest()
                                }
                            },
                            onAddSet = {
                                scope.launch { repo.addSetToExercise(session.id, exerciseId) }
                            },
                            onRemoveExercise = {
                                scope.launch {
                                    repo.removeExerciseFromSession(session.id, exerciseId)
                                }
                            }
                        )
                    }
                }

                item {
                    OutlinedButton(
                        onClick = { showPicker = true },
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
                            onClick = {
                                scope.launch {
                                    summary = buildSummary(repo, session, sets, totalSeconds)
                                    repo.finishSession(session, totalSeconds)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("完成训练")
                        }
                        TextButton(
                            onClick = { confirmAbandon = true },
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
            onDismiss = { showPicker = false },
            onPick = { exerciseId ->
                scope.launch { repo.addExerciseToSession(session.id, exerciseId) }
            }
        )
    }

    // ---- 完成总结 ----
    summary?.let { s ->
        SessionSummaryDialog(
            summary = s,
            canSaveAsPlan = s.session.planId == null,
            onSaveAsPlan = { name ->
                scope.launch {
                    repo.saveSessionAsPlan(s.session, name)
                    summary = null
                }
            },
            onDismiss = { summary = null }
        )
    }

    // ---- 历史详情 ----
    historyDetail?.let { s ->
        HistoryDetailDialog(
            repo = repo,
            session = s,
            onDismiss = { historyDetail = null }
        )
    }

    if (confirmAbandon && session != null) {
        AlertDialog(
            onDismissRequest = { confirmAbandon = false },
            title = { Text("放弃这次训练？") },
            text = { Text("这次训练的全部组记录都会删除，无法恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { repo.deleteSession(session.id) }
                    confirmAbandon = false
                }) { Text("放弃", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmAbandon = false }) { Text("继续训练") }
            }
        )
    }

    confirmDeleteHistory?.let { s ->
        AlertDialog(
            onDismissRequest = { confirmDeleteHistory = null },
            title = { Text("删除这条训练记录？") },
            text = { Text("删除后，这次训练的组记录与日历完成标记都会消失。") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { repo.deleteSession(s.id) }
                    confirmDeleteHistory = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteHistory = null }) { Text("取消") }
            }
        )
    }
}

/** 训练总结数据（完成时算一次，含 PR 判定）。 */
private data class SessionSummary(
    val session: WorkoutSessionEntity,
    val durationSec: Long,
    val volumeKg: Double,
    val doneSets: Int,
    val totalSets: Int,
    val prs: List<String>
)

private suspend fun buildSummary(
    repo: ExerciseRepository,
    session: WorkoutSessionEntity,
    sets: List<WorkoutSetEntity>,
    durationSec: Long
): SessionSummary {
    val done = sets.filter { it.done }
    val volume = done.sumOf { it.weightKg * it.reps }
    val prs = mutableListOf<String>()
    sets.groupBy { it.exerciseId }.forEach { (exerciseId, exSets) ->
        val best = exSets.filter { it.done }.maxOfOrNull { it.weightKg } ?: 0.0
        if (best > 0) {
            val prevBest = repo.bestWeightBefore(exerciseId, session.id) ?: 0.0
            if (prevBest > 0 && best > prevBest) {
                val name = exSets.first().exerciseName
                prs.add("$name 新纪录 ${formatKg(best)} kg（原 ${formatKg(prevBest)} kg）")
            }
        }
    }
    return SessionSummary(session, durationSec, volume, done.size, sets.size, prs)
}

fun formatKg(v: Double): String =
    if (v % 1.0 == 0.0) v.toInt().toString() else "%.1f".format(v)

@Composable
private fun SessionSummaryDialog(
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
private fun HistoryRow(
    session: WorkoutSessionEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = CardShape,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
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
private fun HistoryDetailDialog(
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
private fun ExercisePickerDialog(
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

/** 计划预览卡：开始训练前先看清这个计划练什么。 */
@Composable
private fun PlanPreviewCard(
    plan: PlanWithItems,
    exerciseName: (String) -> String,
    exerciseSub: (String) -> String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = plan.plan.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            plan.items.sortedBy { it.sortOrder }.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = exerciseName(item.exerciseId),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = exerciseSub(item.exerciseId),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    Text(
                        text = "${item.sets} 组 × ${item.reps}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/** 动作组表卡：组号 / 上次 / 重量 / 次数 / 完成勾。 */
@Composable
private fun ExerciseSetCard(
    name: String,
    subtitle: String,
    imagePath: String,
    sets: List<WorkoutSetEntity>,
    previous: List<WorkoutSetEntity>,
    onOpenDetail: () -> Unit,
    onUpdateSet: (WorkoutSetEntity) -> Unit,
    onToggleDone: (WorkoutSetEntity) -> Unit,
    onAddSet: () -> Unit,
    onRemoveExercise: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(ImageShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(onClick = onOpenDetail)
                ) {
                    if (imagePath.isNotBlank()) {
                        LocalAssetImage(
                            path = imagePath,
                            contentDescription = name,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subtitle.isNotBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
                IconButton(onClick = onRemoveExercise) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "移除动作",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            // 表头
            Row(verticalAlignment = Alignment.CenterVertically) {
                SetCellText("组", Modifier.width(28.dp))
                SetCellText("上次", Modifier.width(64.dp))
                SetCellText("kg", Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                SetCellText("次数", Modifier.weight(1f))
                Spacer(Modifier.width(48.dp))
            }
            sets.forEach { set ->
                val prev = previous.getOrNull(set.setIndex) ?: previous.lastOrNull()
                SetRow(
                    set = set,
                    previousText = prev?.let { "${formatKg(it.weightKg)}×${it.reps}" } ?: "—",
                    onUpdate = onUpdateSet,
                    onToggleDone = { onToggleDone(set) }
                )
            }
            TextButton(onClick = onAddSet) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("加一组")
            }
        }
    }
}

@Composable
private fun SetCellText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

@Composable
private fun SetRow(
    set: WorkoutSetEntity,
    previousText: String,
    onUpdate: (WorkoutSetEntity) -> Unit,
    onToggleDone: () -> Unit
) {
    var weightText by remember(set.id) {
        mutableStateOf(if (set.weightKg % 1.0 == 0.0) set.weightKg.toInt().toString() else set.weightKg.toString())
    }
    var repsText by remember(set.id) { mutableStateOf(set.reps.toString()) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = (set.setIndex + 1).toString(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(28.dp)
        )
        Text(
            text = previousText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(64.dp)
        )
        OutlinedTextField(
            value = weightText,
            onValueChange = { s ->
                weightText = s.filter { it.isDigit() || it == '.' }.take(6)
                onUpdate(set.copy(weightKg = weightText.toDoubleOrNull() ?: 0.0))
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        OutlinedTextField(
            value = repsText,
            onValueChange = { s ->
                repsText = s.filter { it.isDigit() }.take(3)
                onUpdate(set.copy(reps = repsText.toIntOrNull() ?: 0))
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onToggleDone) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = if (set.done) "取消完成" else "完成这组",
                tint = if (set.done) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(
                        if (set.done) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .padding(5.dp)
            )
        }
    }
}

/** 计划选择器：横向可滚动列表，显示所有计划，高亮当前选中 */
@Composable
private fun PlanSelector(
    plans: List<PlanWithItems>,
    current: PlanWithItems?,
    onSelect: (PlanWithItems) -> Unit
) {
    if (plans.isEmpty()) return
    androidx.compose.foundation.lazy.LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(plans, key = { it.plan.id }) { pw ->
            val selected = pw.plan.id == current?.plan?.id
            Surface(
                modifier = Modifier.clickable { onSelect(pw) },
                shape = CardShape,
                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Text(
                        text = pw.plan.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${pw.items.size} 个动作",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** 总用时卡片：渐变背景 + 大号计时 + 暂停/继续/重置 */
@Composable
private fun TotalTimeCard(
    totalSeconds: Long,
    running: Boolean,
    completed: Int,
    total: Int,
    unitLabel: String = "个动作",
    onToggle: () -> Unit,
    onReset: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary
                    )
                )
            )
            .padding(24.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "总用时",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.22f)
                ) {
                    Text(
                        text = if (running) "训练中" else "已暂停",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = formatClock(totalSeconds),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "已完成 $completed / $total $unitLabel",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.92f)
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onToggle,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = if (running) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (running) "暂停" else "开始")
                }
                FilledTonalButton(
                    onClick = onReset,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color.White.copy(alpha = 0.22f),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("同步")
                }
            }
        }
    }
}

/** 组间歇卡片：倒计时数字 + 进度条 + 预设时长按钮 */
@Composable
private fun RestTimerCard(
    total: Int,
    remaining: Int,
    running: Boolean,
    onPreset: (Int) -> Unit,
    onToggle: () -> Unit,
    onReset: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "组间歇",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formatClock(remaining.toLong()),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (running) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { if (total == 0) 0f else remaining.toFloat() / total },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onToggle,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = if (running) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (running) "暂停" else (if (remaining > 0) "继续" else "开始"))
                }
                FilledTonalButton(onClick = onReset, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("重置")
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = "快速设置（完成一组后会自动开始）",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(30, 60, 90, 120).forEach { secs ->
                    val selected = total == secs
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(CardShape)
                            .clickable { onPreset(secs) },
                        shape = CardShape,
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "${secs}s",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 10.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/** 格式化秒为 hh:mm:ss（始终显示小时，支持长时间训练） */
private fun formatClock(seconds: Long): String {
    val s = seconds.coerceAtLeast(0L)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return "%02d:%02d:%02d".format(h, m, sec)
}

/** 组间歇结束震动提醒（minSdk 31，直接用 VibratorManager）。 */
private fun vibrateOnce(context: Context) {
    val vibrator = (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)
        ?.defaultVibrator ?: return
    if (!vibrator.hasVibrator()) return
    vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 320, 180, 320), -1))
}
