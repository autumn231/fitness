package com.fitness.app.ui.feature

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CalendarMonth
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitness.app.data.ExerciseRepository
import com.fitness.app.data.local.CalendarPlanEntity
import com.fitness.app.data.local.PlanWithItems
import com.fitness.app.ui.theme.CardShape
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    repo: ExerciseRepository,
    onNavigate: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val calendarState = repo.observeCalendarPlans().collectAsStateWithLifecycle(initialValue = emptyList())
    val plansState = repo.observePlans().collectAsStateWithLifecycle(initialValue = emptyList())
    val calendarPlans = calendarState.value
    val plans = plansState.value

    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var showPicker by remember { mutableStateOf(false) }

    // 选中某天时，若该天已有安排则展示计划名
    val selectedDateKey = selectedDate?.format(DateTimeFormatter.ISO_DATE)
    val selectedEntry = calendarPlans.firstOrNull { it.dateKey == selectedDateKey }
    val selectedPlan = plans.firstOrNull { it.plan.id == selectedEntry?.planId }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("日历", fontWeight = FontWeight.SemiBold) })
        }
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
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
                        onSetPlan = { showPicker = true },
                        onRemove = {
                            scope.launch {
                                repo.removeCalendarPlan(selectedDateKey!!)
                            }
                        },
                        onStartTraining = {
                            selectedPlan?.let {
                                onNavigate("training")
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
                .filter { LocalDate.parse(it.dateKey) >= LocalDate.now() }
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
                            selectedDate = LocalDate.parse(entry.dateKey)
                            currentMonth = YearMonth.from(selectedDate!!)
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

/** 月份选择器：左右箭头 + 年月显示 */
@Composable
private fun MonthSelector(
    yearMonth: YearMonth,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrev) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = "上个月")
            }
            Text(
                text = "${yearMonth.year}年${yearMonth.monthValue}月",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            IconButton(onClick = onNext) {
                Icon(Icons.Filled.ChevronRight, contentDescription = "下个月")
            }
        }
    }
}

/** 日历网格：星期表头 + 当月日期 */
@Composable
private fun CalendarGrid(
    yearMonth: YearMonth,
    selectedDate: LocalDate?,
    scheduledDates: Set<String>,
    onDateSelected: (LocalDate) -> Unit
) {
    val today = LocalDate.now()
    val firstDay = yearMonth.atDay(1)
    // 周一为一周开始
    val startOffset = (firstDay.dayOfWeek.value - 1)
    val daysInMonth = yearMonth.lengthOfMonth()
    val dateKeyFormatter = DateTimeFormatter.ISO_DATE

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // 星期表头
            val weekDays = listOf(
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                weekDays.forEach { dow ->
                    Text(
                        text = dow.getDisplayName(TextStyle.NARROW, Locale.CHINESE),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f).padding(vertical = 6.dp)
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            // 日期网格
            val totalCells = startOffset + daysInMonth
            val rows = (totalCells + 6) / 7
            for (row in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0..6) {
                        val index = row * 7 + col
                        val day = index - startOffset + 1
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f)) {
                            if (day in 1..daysInMonth) {
                                val date = yearMonth.atDay(day)
                                val dateKey = date.format(dateKeyFormatter)
                                val isSelected = date == selectedDate
                                val isToday = date == today
                                val hasSchedule = dateKey in scheduledDates
                                CalendarDayCell(
                                    day = day,
                                    isToday = isToday,
                                    isSelected = isSelected,
                                    hasSchedule = hasSchedule,
                                    onClick = { onDateSelected(date) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: Int,
    isToday: Boolean,
    isSelected: Boolean,
    hasSchedule: Boolean,
    onClick: () -> Unit
) {
    val bgColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primaryContainer
        else -> Color.Transparent
    }
    val fgColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(2.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                color = fgColor
            )
            if (hasSchedule) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.primary
                        )
                )
            }
        }
    }
}

@Composable
private fun SelectedDateCard(
    date: LocalDate,
    planName: String?,
    planItemCount: Int,
    onSetPlan: () -> Unit,
    onRemove: () -> Unit,
    onStartTraining: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "${date.monthValue}月${date.dayOfMonth}日 · ${
                    date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.CHINESE)
                }",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(10.dp))
            if (planName != null) {
                Text(
                    text = "计划：$planName",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "共 $planItemCount 个动作",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onStartTraining),
                        shape = CardShape,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "去训练",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onSetPlan),
                        shape = CardShape,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Text(
                            text = "更换计划",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                    IconButton(onClick = onRemove) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "移除安排",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            } else {
                Text(
                    text = "当天暂无训练安排",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Spacer(Modifier.height(12.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onSetPlan),
                    shape = CardShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = "为这一天安排训练计划",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun UpcomingRow(
    dateKey: String,
    planName: String,
    itemCount: Int,
    onClick: () -> Unit
) {
    val date = LocalDate.parse(dateKey)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = CardShape,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${date.dayOfMonth}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = planName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    text = "${date.monthValue}月${date.dayOfMonth}日 · $itemCount 个动作",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
            )
        }
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
                Column {
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
