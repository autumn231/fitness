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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitness.app.data.ExerciseRepository
import com.fitness.app.data.local.PlanItemEntity
import com.fitness.app.i18n.displayName
import com.fitness.app.i18n.subtitle
import com.fitness.app.ui.common.LocalAssetImage
import com.fitness.app.ui.theme.CardShape
import com.fitness.app.ui.theme.ImageShape
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanTrainingScreen(
    repo: ExerciseRepository,
    planId: Long,
    onBack: () -> Unit,
    onOpenExercise: (String) -> Unit
) {
    val planState = repo.observePlan(planId).collectAsStateWithLifecycle(initialValue = null)
    val pw = planState.value

    // 总用时（秒），进入页面即开始计时
    var totalSeconds by rememberSaveable { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            totalSeconds += 1
        }
    }

    // 组间歇倒计时
    var restTotal by remember { mutableIntStateOf(90) } // 设定时长
    var restRemaining by remember { mutableIntStateOf(0) } // 剩余
    var restRunning by remember { mutableStateOf(false) }
    LaunchedEffect(restRunning) {
        while (restRunning && restRemaining > 0) {
            delay(1000L)
            restRemaining -= 1
            if (restRemaining <= 0) {
                restRunning = false
            }
        }
    }

    // 待办完成状态
    val items = pw?.items ?: emptyList()
    val completed = remember(items) { mutableStateOf(setOf<Long>()) }
    val completedSet = completed.value

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("训练中 · ${pw?.plan?.name ?: ""}", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.Stop, contentDescription = "结束训练", tint = MaterialTheme.colorScheme.error)
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
            // 总用时卡片
            item {
                TotalTimeCard(totalSeconds = totalSeconds, completed = completedSet.size, total = items.size)
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

            // 待办列表标题
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

            if (items.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = CardShape,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "该计划暂无动作",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
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
                        onClick = { ex?.let { onOpenExercise(it.id) } }
                    )
                }
            }
        }
    }
}

/** 总用时卡片：渐变背景 + 大号计时 */
@Composable
private fun TotalTimeCard(totalSeconds: Long, completed: Int, total: Int) {
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
            Text(
                text = "总用时",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.85f)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = formatClock(totalSeconds),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "已完成 $completed / $total 个动作",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.92f)
            )
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
                if (remaining > 0) {
                    Text(
                        text = formatClock(remaining.toLong()),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (running) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "00:00",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            if (total > 0) {
                LinearProgressIndicator(
                    progress = { if (total == 0) 0f else remaining.toFloat() / total },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            Spacer(Modifier.height(14.dp))
            // 控制按钮
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
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
            // 完成勾选
            IconButton(onClick = onToggleDone) {
                Icon(
                    imageVector = if (done) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = if (done) "取消完成" else "标记完成",
                    tint = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp)
                )
            }
            // 缩略图
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

/** 格式化秒为 mm:ss 或 hh:mm:ss */
private fun formatClock(seconds: Long): String {
    val s = seconds.coerceAtLeast(0L)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) {
        "%02d:%02d:%02d".format(h, m, sec)
    } else {
        "%02d:%02d".format(m, sec)
    }
}
