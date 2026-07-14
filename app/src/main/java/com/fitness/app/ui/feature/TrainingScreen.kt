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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitness.app.data.ExerciseRepository
import com.fitness.app.data.local.PlanItemEntity
import com.fitness.app.data.local.PlanWithItems
import com.fitness.app.i18n.displayName
import com.fitness.app.i18n.subtitle
import com.fitness.app.ui.common.EmptyState
import com.fitness.app.ui.common.LocalAssetImage
import com.fitness.app.ui.nav.Destinations
import com.fitness.app.ui.theme.CardShape
import com.fitness.app.ui.theme.ImageShape
import kotlinx.coroutines.delay

/**
 * 训练执行页（导航栏"训练"Tab）：
 * - 顶部可切换计划
 * - 总用时（可暂停/继续，始终 hh:mm:ss）
 * - 组间歇倒计时
 * - 训练清单待办
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingScreen(
    repo: ExerciseRepository,
    onNavigate: (String) -> Unit
) {
    val plansState = repo.observePlans().collectAsStateWithLifecycle(initialValue = emptyList())
    val plans = plansState.value

    // 当前选中计划 id（默认第一个）
    var selectedPlanId by rememberSaveable(plans.firstOrNull()?.plan?.id) {
        mutableLongStateOf(plans.firstOrNull()?.plan?.id ?: 0L)
    }
    val currentPlan = plans.firstOrNull { it.plan.id == selectedPlanId }

    // 总用时
    var totalSeconds by rememberSaveable { mutableLongStateOf(0L) }
    var timerRunning by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(timerRunning) {
        while (timerRunning) {
            delay(1000L)
            totalSeconds += 1
        }
    }

    // 组间歇
    var restTotal by remember { mutableIntStateOf(90) }
    var restRemaining by remember { mutableIntStateOf(0) }
    var restRunning by remember { mutableStateOf(false) }
    LaunchedEffect(restRunning) {
        while (restRunning && restRemaining > 0) {
            delay(1000L)
            restRemaining -= 1
            if (restRemaining <= 0) restRunning = false
        }
    }

    // 待办完成状态（按计划 id 记忆）
    val items = currentPlan?.items ?: emptyList()
    val completed = remember(selectedPlanId) { mutableStateOf(setOf<Long>()) }
    val completedSet = completed.value

    // 切换计划时重置计时与完成状态
    LaunchedEffect(selectedPlanId) {
        totalSeconds = 0L
        timerRunning = false
        restRunning = false
        restRemaining = 0
        completed.value = emptySet()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("训练", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = { onNavigate(Destinations.Plans.route) }) {
                        Icon(Icons.Filled.Check, contentDescription = "管理计划")
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
            // 计划选择器
            item {
                PlanSelector(
                    plans = plans,
                    current = currentPlan,
                    onSelect = { selectedPlanId = it.plan.id }
                )
            }

            if (currentPlan == null || items.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Outlined.RadioButtonUnchecked,
                        title = "还没有可选的计划",
                        subtitle = "请先在「我的计划」中创建训练计划并添加动作",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                // 总用时卡片
                item {
                    TotalTimeCard(
                        totalSeconds = totalSeconds,
                        running = timerRunning,
                        completed = completedSet.size,
                        total = items.size,
                        onToggle = { timerRunning = !timerRunning },
                        onReset = {
                            timerRunning = false
                            totalSeconds = 0L
                        }
                    )
                }

                // 组间歇卡片
                item {
                    RestTimerCard(
                        total = restTotal,
                        remaining = restRemaining,
                        running = restRunning,
                        onPreset = { secs ->
                            restTotal = secs
                            restRemaining = secs
                            restRunning = true
                        },
                        onToggle = {
                            if (restRemaining <= 0) {
                                restRemaining = restTotal
                                restRunning = true
                            } else {
                                restRunning = !restRunning
                            }
                        },
                        onReset = {
                            restRunning = false
                            restRemaining = 0
                        }
                    )
                }

                // 训练清单标题
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "训练清单",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${completedSet.size}/${items.size}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                items(items, key = { it.id }) { item ->
                    val ex = repo.byId(item.exerciseId)
                    TrainingItemRow(
                        item = item,
                        name = ex?.displayName() ?: "未知动作",
                        subtitle = ex?.subtitle() ?: "",
                        imagePath = ex?.image ?: "",
                        done = item.id in completedSet,
                        onToggleDone = {
                            completed.value = if (item.id in completedSet) {
                                completedSet - item.id
                            } else {
                                completedSet + item.id
                            }
                        },
                        onClick = { ex?.let { onNavigate(Destinations.Exercise.create(it.id)) } }
                    )
                }
            }
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
                modifier = Modifier
                    .clickable { onSelect(pw) },
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
                text = "已完成 $completed / $total 个动作",
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
                    Text("重置")
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
                text = "快速设置",
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

@Composable
private fun TrainingItemRow(
    item: PlanItemEntity,
    name: String,
    subtitle: String,
    imagePath: String,
    done: Boolean,
    onToggleDone: () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = CardShape,
        color = if (done) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggleDone) {
                Icon(
                    imageVector = if (done) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = if (done) "取消完成" else "标记完成",
                    tint = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp)
                )
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(ImageShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (imagePath.isNotBlank()) {
                    LocalAssetImage(
                        path = imagePath,
                        contentDescription = name,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    color = if (done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$subtitle · ${item.sets}组 × ${item.reps}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            if (done) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
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
