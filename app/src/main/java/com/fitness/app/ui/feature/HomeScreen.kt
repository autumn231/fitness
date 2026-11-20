package com.fitness.app.ui.feature

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitness.app.data.DateKeys
import com.fitness.app.data.ExerciseRepository
import com.fitness.app.data.Meals
import com.fitness.app.data.effectiveTdee
import com.fitness.app.data.estimateBurnKcal
import com.fitness.app.data.local.FoodLogEntity
import com.fitness.app.ui.common.ExerciseCard
import com.fitness.app.ui.common.SectionHeader
import com.fitness.app.ui.nav.Destinations
import com.fitness.app.ui.theme.CardShape
import com.fitness.app.ui.theme.ChipShape
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * 今日页（原首页）：Diary 式今日记录页——打开 App 第一眼就是今天：
 * 周日期条 → 热量总账（还可吃）→ 按餐次的记录 → 今日训练 → 体重。
 * 动作库降为底部二级入口。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    repo: ExerciseRepository,
    onNavigate: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    // 查看的日期（默认今天；跨天回到前台时若正停在"今天"则跟随刷新）
    var selectedEpochDay by rememberSaveable { mutableStateOf(LocalDate.now().toEpochDay()) }
    var followingToday by rememberSaveable { mutableStateOf(true) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && followingToday) {
                selectedEpochDay = LocalDate.now().toEpochDay()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val selectedDate = LocalDate.ofEpochDay(selectedEpochDay)
    val isToday = selectedDate == LocalDate.now()
    val dateKey = DateKeys.of(selectedDate)

    // 数据
    val logs by repo.observeFoodLogs(dateKey)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val tdeeOverride by repo.settings.tdee.collectAsStateWithLifecycle(initialValue = 0)
    val profile by repo.observeProfile().collectAsStateWithLifecycle(initialValue = null)
    val weights by repo.observeWeights().collectAsStateWithLifecycle(initialValue = emptyList())
    val sessions by repo.observeSessions().collectAsStateWithLifecycle(initialValue = emptyList())
    val activeSession by repo.observeActiveSession()
        .collectAsStateWithLifecycle(initialValue = null)
    val calendarPlans by repo.observeCalendarPlans()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val plans by repo.observePlans().collectAsStateWithLifecycle(initialValue = emptyList())
    val recents by repo.observeRecents(8).collectAsStateWithLifecycle(initialValue = emptyList())

    val latestWeight = weights.lastOrNull()
    val budget = effectiveTdee(tdeeOverride, profile, latestWeight?.weightKg)

    val intake = logs.sumOf { it.energy }
    val totalProtein = logs.sumOf { it.protein }
    val totalCarbs = logs.sumOf { it.carbs }
    val totalFat = logs.sumOf { it.fat }

    val daySessions = sessions.filter { it.dateKey == dateKey && it.endedAt != null }
    val burn = daySessions.sumOf { estimateBurnKcal(it.durationSec) }
    val remaining = budget - intake + burn

    val todayPlan = calendarPlans.firstOrNull { it.dateKey == dateKey }
        ?.let { entry -> plans.firstOrNull { it.plan.id == entry.planId } }

    // 对话框状态
    var editLog by remember { mutableStateOf<FoodLogEntity?>(null) }
    var quickAddMeal by remember { mutableStateOf<String?>(null) }
    var saveTemplateMeal by remember { mutableStateOf<String?>(null) }
    var showTdee by remember { mutableStateOf(false) }
    var showWeight by remember { mutableStateOf(false) }
    var showQuickSheet by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showQuickSheet = true }) {
                Icon(Icons.Filled.Add, contentDescription = "快速记录")
            }
        }
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .statusBarsPadding(),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 日期标题
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${selectedDate.monthValue}月${selectedDate.dayOfMonth}日 · ${
                                selectedDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.CHINESE)
                            }",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isToday) "今天" else "历史记录（可补记）",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!isToday) {
                        TextButton(onClick = {
                            selectedEpochDay = LocalDate.now().toEpochDay()
                            followingToday = true
                        }) { Text("回到今天") }
                    }
                }
            }

            // 周日期条
            item {
                WeekStrip(
                    selected = selectedDate,
                    onSelect = { date ->
                        selectedEpochDay = date.toEpochDay()
                        followingToday = date == LocalDate.now()
                    }
                )
            }

            // 热量总账
            item {
                BudgetCard(
                    budget = budget,
                    intake = intake,
                    burn = burn,
                    remaining = remaining,
                    protein = totalProtein,
                    carbs = totalCarbs,
                    fat = totalFat,
                    onClick = { showTdee = true }
                )
            }

            // 按餐次记录
            Meals.ALL.forEach { meal ->
                val mealLogs = logs.filter { it.meal == meal }
                    .sortedBy { it.addedAt }
                item(key = "meal_$meal") {
                    MealSectionHeader(
                        meal = meal,
                        subtotalKcal = mealLogs.sumOf { it.energy },
                        recommended = Meals.recommendedRange(meal, budget),
                        hasLogs = mealLogs.isNotEmpty(),
                        onAdd = {
                            onNavigate(Destinations.AddFood.create(dateKey, Meals.ALL.indexOf(meal)))
                        },
                        onCopyYesterday = {
                            scope.launch {
                                val n = repo.copyMeal(
                                    DateKeys.of(selectedDate.minusDays(1)), meal, dateKey
                                )
                                snackbar.showSnackbar(
                                    if (n > 0) "已复制昨天${meal}的 $n 样食物"
                                    else "昨天${meal}没有记录可复制"
                                )
                            }
                        },
                        onSaveTemplate = { saveTemplateMeal = meal },
                        onQuickAdd = { quickAddMeal = meal }
                    )
                }
                if (mealLogs.isEmpty()) {
                    item(key = "meal_${meal}_empty") {
                        Text(
                            text = "暂无记录，点 + 添加",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    items(mealLogs, key = { it.id }) { log ->
                        DiaryLogRow(
                            log = log,
                            onClick = { editLog = log },
                            onDelete = { scope.launch { repo.removeFoodLog(log.id) } },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // 今日训练
            item { SectionHeader(title = if (isToday) "今日训练" else "当天训练") }
            item {
                TodayTrainingCard(
                    finishedCount = daySessions.size,
                    finishedDurationSec = daySessions.sumOf { it.durationSec },
                    hasActiveToday = activeSession != null && activeSession?.dateKey == DateKeys.today(),
                    planName = todayPlan?.plan?.name,
                    planItemCount = todayPlan?.items?.size ?: 0,
                    isToday = isToday,
                    onStart = {
                        todayPlan?.let { pw ->
                            repo.requestPlan(pw.plan.id)
                            onNavigate(Destinations.Training.route)
                        }
                    },
                    onOpenTraining = { onNavigate(Destinations.Training.route) },
                    onSchedule = { onNavigate(Destinations.Calendar.route) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // 体重
            item { SectionHeader(title = "体重") }
            item {
                WeightCard(
                    latestKg = latestWeight?.weightKg,
                    latestDate = latestWeight?.dateKey,
                    delta = weightDelta(weights.map { it.weightKg }),
                    onRecord = { showWeight = true },
                    onOpenProgress = { onNavigate(Destinations.Weight.route) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // 动作库（二级入口）
            item { SectionHeader(title = "动作库") }
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clickable { onNavigate(Destinations.Search.route) },
                    shape = ChipShape,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Search, contentDescription = null)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "搜索动作、肌群、器械…",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LibraryChip("动作分类", Modifier.weight(1f)) {
                        onNavigate(Destinations.Category.route)
                    }
                    LibraryChip("收藏与历史", Modifier.weight(1f)) {
                        onNavigate(Destinations.Favorites.route)
                    }
                    LibraryChip("我的计划", Modifier.weight(1f)) {
                        onNavigate(Destinations.Plans.route)
                    }
                }
            }
            if (recents.isNotEmpty()) {
                item {
                    Text(
                        text = "最近浏览",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                }
                items(recents.take(3), key = { "recent_" + it.exerciseId }) { recent ->
                    val ex = repo.byId(recent.exerciseId) ?: return@items
                    ExerciseCard(
                        exercise = ex,
                        onClick = { onNavigate(Destinations.Exercise.create(ex.id)) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }

    // ---- 对话框 ----
    editLog?.let { log ->
        EditGramsDialog(
            log = log,
            onDismiss = { editLog = null },
            onConfirm = { grams ->
                scope.launch {
                    repo.updateFoodLogAmount(log, grams)
                    editLog = null
                }
            }
        )
    }
    quickAddMeal?.let { meal ->
        QuickAddDialog(
            meal = meal,
            onDismiss = { quickAddMeal = null },
            onConfirm = { kcal, name ->
                scope.launch {
                    repo.quickAddFoodLog(kcal, name, dateKey, meal)
                    quickAddMeal = null
                    snackbar.showSnackbar("已入账 ${kcal.toInt()} kcal")
                }
            }
        )
    }
    saveTemplateMeal?.let { meal ->
        val mealLogs = logs.filter { it.meal == meal }
        SaveMealTemplateDialog(
            meal = meal,
            itemCount = mealLogs.size,
            onDismiss = { saveTemplateMeal = null },
            onConfirm = { name ->
                scope.launch {
                    repo.saveMealTemplate(name, mealLogs)
                    saveTemplateMeal = null
                    snackbar.showSnackbar("已存为常用餐「$name」")
                }
            }
        )
    }
    if (showTdee) {
        TdeeDialog(
            overrideKcal = tdeeOverride,
            computedKcal = com.fitness.app.data.computeTdee(profile, latestWeight?.weightKg),
            onSaveOverride = { v ->
                scope.launch {
                    repo.settings.setTdee(v)
                    showTdee = false
                }
            },
            onDismiss = { showTdee = false },
            onEditProfile = {
                showTdee = false
                onNavigate(Destinations.ProfileEdit.route)
            }
        )
    }
    if (showWeight) {
        WeightEntryDialog(
            date = LocalDate.now(),
            initialKg = latestWeight?.weightKg,
            onDismiss = { showWeight = false },
            onConfirm = { key, kg ->
                scope.launch {
                    repo.setWeight(key, kg)
                    showWeight = false
                    snackbar.showSnackbar("已记录体重 $kg kg")
                }
            }
        )
    }
    if (showQuickSheet) {
        ModalBottomSheet(onDismissRequest = { showQuickSheet = false }) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                QuickActionRow("记饮食", "添加今天吃的食物") {
                    showQuickSheet = false
                    onNavigate(
                        Destinations.AddFood.create(
                            DateKeys.today(),
                            Meals.ALL.indexOf(Meals.inferNow())
                        )
                    )
                }
                QuickActionRow("记体重", "记录今天的体重") {
                    showQuickSheet = false
                    showWeight = true
                }
                QuickActionRow("开始训练", "去训练页开始一次训练") {
                    showQuickSheet = false
                    onNavigate(Destinations.Training.route)
                }
            }
        }
    }
}

private fun weightDelta(values: List<Double>): Double? =
    if (values.size >= 2) values.last() - values[values.size - 2] else null

@Composable
private fun WeekStrip(selected: LocalDate, onSelect: (LocalDate) -> Unit) {
    val monday = selected.with(DayOfWeek.MONDAY)
    val today = LocalDate.now()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        (0..6).forEach { offset ->
            val date = monday.plusDays(offset.toLong())
            val isSelected = date == selected
            val isToday = date == today
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onSelect(date) }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text(
                    text = date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.CHINESE),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isSelected -> MaterialTheme.colorScheme.primary
                                isToday -> MaterialTheme.colorScheme.primaryContainer
                                else -> Color.Transparent
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = date.dayOfMonth.toString(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                        color = when {
                            isSelected -> MaterialTheme.colorScheme.onPrimary
                            isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }
        }
    }
}

/** 热量总账卡：还可吃 = 目标 − 摄入 + 运动消耗（估算）。 */
@Composable
private fun BudgetCard(
    budget: Int,
    intake: Double,
    burn: Int,
    remaining: Double,
    protein: Double,
    carbs: Double,
    fat: Double,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(CardShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary
                    )
                )
            )
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            if (budget <= 0) {
                Text(
                    text = "每日热量目标未设置",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "已摄入 ${intake.toInt()} kcal。点此设置目标，或完善档案后自动计算。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            } else {
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (remaining >= 0) "还可吃" else "已超出",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Text(
                            text = "${kotlin.math.abs(remaining).toInt()}",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "kcal",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        BudgetStat("目标", "$budget")
                        BudgetStat("已摄入", "${intake.toInt()}")
                        BudgetStat("运动消耗(估算)", "+$burn")
                    }
                }
                Spacer(Modifier.height(12.dp))
                val denom = (budget + burn).coerceAtLeast(1)
                LinearProgressIndicator(
                    progress = { (intake / denom).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.25f)
                )
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MacroProgress("蛋白质", protein, budget * 0.25 / 4)
                    MacroProgress("碳水", carbs, budget * 0.50 / 4)
                    MacroProgress("脂肪", fat, budget * 0.25 / 9)
                }
            }
        }
    }
}

@Composable
private fun BudgetStat(label: String, value: String) {
    Row {
        Text(
            text = "$label ",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.8f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun MacroProgress(label: String, eaten: Double, target: Double) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "${eaten.toInt()} / ${target.toInt()}g",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}

/** 餐次段头：餐名 + 推荐区间 + 小计 + 添加/菜单。 */
@Composable
private fun MealSectionHeader(
    meal: String,
    subtotalKcal: Double,
    recommended: IntRange?,
    hasLogs: Boolean,
    onAdd: () -> Unit,
    onCopyYesterday: () -> Unit,
    onSaveTemplate: () -> Unit,
    onQuickAdd: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp, top = 14.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = meal,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = buildString {
                if (recommended != null) append("推荐 ${recommended.first}–${recommended.last} kcal")
                if (subtotalKcal > 0) {
                    if (isNotEmpty()) append(" · ")
                    append("已 ${subtotalKcal.toInt()} kcal")
                }
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onAdd) {
            Icon(Icons.Filled.Add, contentDescription = "添加$meal", tint = MaterialTheme.colorScheme.primary)
        }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "${meal}更多操作")
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("复制昨天同餐") },
                    onClick = { menuOpen = false; onCopyYesterday() }
                )
                DropdownMenuItem(
                    text = { Text("快速添加（只填热量）") },
                    onClick = { menuOpen = false; onQuickAdd() }
                )
                if (hasLogs) {
                    DropdownMenuItem(
                        text = { Text("存为常用餐") },
                        onClick = { menuOpen = false; onSaveTemplate() }
                    )
                }
            }
        }
    }
}

/** 日记行：点按改克数，右侧删除。 */
@Composable
private fun DiaryLogRow(
    log: FoodLogEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
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
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.foodName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (log.amountGram > 0)
                        "${log.amountGram.toInt()}g · 蛋白 ${log.protein.toInt()}g / 碳水 ${log.carbs.toInt()}g / 脂肪 ${log.fat.toInt()}g"
                    else "快速添加",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "${log.energy.toInt()} kcal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TodayTrainingCard(
    finishedCount: Int,
    finishedDurationSec: Long,
    hasActiveToday: Boolean,
    planName: String?,
    planItemCount: Int,
    isToday: Boolean,
    onStart: () -> Unit,
    onOpenTraining: () -> Unit,
    onSchedule: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            when {
                hasActiveToday -> {
                    Text("训练进行中", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "有一场训练还没结束，回去继续完成它。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    TextButton(onClick = onOpenTraining) { Text("继续训练") }
                }
                finishedCount > 0 -> {
                    Text("已完成训练 ✓", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "共 $finishedCount 场 · 总时长 ${finishedDurationSec / 60} 分钟",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    TextButton(onClick = onOpenTraining) { Text("查看训练") }
                }
                planName != null -> {
                    Text(planName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "已安排 · 共 $planItemCount 个动作",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isToday) {
                        Spacer(Modifier.height(10.dp))
                        TextButton(onClick = onStart) { Text("去训练") }
                    }
                }
                else -> {
                    Text("没有安排训练", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "在日历里为这一天安排一个计划，或直接开始自由训练。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    Row {
                        TextButton(onClick = onSchedule) { Text("去排计划") }
                        TextButton(onClick = onOpenTraining) { Text("开始训练") }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeightCard(
    latestKg: Double?,
    latestDate: String?,
    delta: Double?,
    onRecord: () -> Unit,
    onOpenProgress: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenProgress),
        shape = CardShape,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (latestKg != null) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = latestKg.toString(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "kg",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        if (delta != null && delta != 0.0) {
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = if (delta > 0) "↑ ${"%.1f".format(delta)}" else "↓ ${"%.1f".format(-delta)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (delta > 0) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }
                    Text(
                        text = "最近记录：$latestDate",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text("还没有体重记录", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "记录体重后，这里会显示趋势，热量目标也会自动计算。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            TextButton(onClick = onRecord) { Text("记体重") }
        }
    }
}

@Composable
private fun LibraryChip(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = ChipShape,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
        )
    }
}

@Composable
private fun QuickActionRow(title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.FitnessCenter,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
