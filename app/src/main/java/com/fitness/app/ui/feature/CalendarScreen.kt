package com.fitness.app.ui.feature

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitness.app.data.DateKeys
import com.fitness.app.data.ExerciseRepository
import com.fitness.app.data.local.PlanWithItems
import com.fitness.app.ui.nav.Destinations
import com.fitness.app.ui.theme.CardShape
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * 日历页：计划即日历——月历上排计划、看完成标记，选中日期可去训练。
 * 月历网格/选中日卡/即将到来行在 CalendarGrid，本文件是页面骨架与计划选择对话框。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    repo: ExerciseRepository,
    onNavigate: (String) -> Unit,
    onBack: (() -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val calendarState = repo.observeCalendarPlans().collectAsStateWithLifecycle(initialValue = emptyList())
    val plansState = repo.observePlans().collectAsStateWithLifecycle(initialValue = emptyList())
    val calendarPlans = calendarState.value
    val plans = plansState.value
    // 已完成训练的日期（落库后日历能区分「排了计划」与「真的练了」）
    val sessionsState = repo.observeSessions().collectAsStateWithLifecycle(initialValue = emptyList())
    val finishedByDate = sessionsState.value
        .filter { it.endedAt != null }
        .groupBy { it.dateKey }
        .mapValues { (_, list) -> list.maxByOrNull { it.endedAt ?: 0L }!! }
    val finishedDates = finishedByDate.keys

    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var showPicker by remember { mutableStateOf(false) }

    // 选中某天时，若该天已有安排则展示计划名
    val selectedDateKey = selectedDate?.format(DateTimeFormatter.ISO_DATE)
    val selectedEntry = calendarPlans.firstOrNull { it.dateKey == selectedDateKey }
    val selectedPlan = plans.firstOrNull { it.plan.id == selectedEntry?.planId }

    Scaffold(
        // 底部导航栏高度由根 Scaffold 让出，避免重复内边距
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("日历", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
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
            // 月份选择器
            item {
                MonthSelector(
                    yearMonth = currentMonth,
                    onPrev = { currentMonth = currentMonth.minusMonths(1) },
                    onNext = { currentMonth = currentMonth.plusMonths(1) }
                )
            }

            // 日历网格
            item {
                CalendarGrid(
                    yearMonth = currentMonth,
                    selectedDate = selectedDate,
                    scheduledDates = calendarPlans.map { it.dateKey }.toSet(),
                    finishedDates = finishedDates,
                    onDateSelected = { date ->
                        selectedDate = if (date == selectedDate) null else date
                    }
                )
            }

            // 选中日期详情
            if (selectedDate != null) {
                item {
                    SelectedDateCard(
                        date = selectedDate!!,
                        planName = selectedPlan?.plan?.name,
                        planItemCount = selectedPlan?.items?.size ?: 0,
                        finishedSummary = finishedByDate[selectedDateKey]?.let { s ->
                            val mins = (s.durationSec / 60).toInt()
                            "已完成训练：${s.planName.ifBlank { "自由训练" }} · ${mins} 分钟"
                        },
                        onSetPlan = { showPicker = true },
                        onRemove = {
                            scope.launch {
                                repo.removeCalendarPlan(selectedDateKey!!)
                            }
                        },
                        onStartTraining = {
                            selectedPlan?.let { pw ->
                                // 把当天安排的固定计划带到训练页，避免用户进去还要手动再选一遍
                                repo.requestPlan(pw.plan.id)
                                onNavigate(Destinations.Training.route)
                            }
                        }
                    )
                }
            }

            // 即将到来的安排列表
            item {
                Text(
                    text = "即将到来",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            val upcoming = calendarPlans
                .filter { DateKeys.parse(it.dateKey)?.let { d -> !d.isBefore(LocalDate.now()) } == true }
                .sortedBy { it.dateKey }
            if (upcoming.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = CardShape,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "暂无安排，点击任意日期添加训练计划",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(upcoming, key = { it.dateKey }) { entry ->
                    val plan = plans.firstOrNull { it.plan.id == entry.planId }
                    UpcomingRow(
                        dateKey = entry.dateKey,
                        planName = plan?.plan?.name ?: "已删除的计划",
                        itemCount = plan?.items?.size ?: 0,
                        onClick = {
                            DateKeys.parse(entry.dateKey)?.let { date ->
                                selectedDate = date
                                currentMonth = YearMonth.from(date)
                            }
                        }
                    )
                }
            }
        }
    }

    // 计划选择对话框
    if (showPicker && selectedDate != null) {
        PlanPickerDialog(
            plans = plans,
            onDismiss = { showPicker = false },
            onPick = { pw ->
                scope.launch {
                    repo.setCalendarPlan(selectedDateKey!!, pw.plan.id)
                }
                showPicker = false
            }
        )
    }
}

@Composable
private fun PlanPickerDialog(
    plans: List<PlanWithItems>,
    onDismiss: () -> Unit,
    onPick: (PlanWithItems) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择训练计划") },
        text = {
            if (plans.isEmpty()) {
                Text("还没有计划，请先创建训练计划。")
            } else {
                Column(
                    modifier = Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    plans.forEach { pw ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(pw) }
                                .padding(vertical = 4.dp),
                            shape = CardShape,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = pw.plan.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${pw.items.size} 个动作",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
