package com.fitness.app.ui.feature

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fitness.app.data.DateKeys
import com.fitness.app.data.ExerciseRepository
import com.fitness.app.data.Meals
import com.fitness.app.data.computeTdee
import com.fitness.app.ui.nav.Destinations
import com.fitness.app.ui.viewmodel.HomeCards
import com.fitness.app.ui.viewmodel.HomeViewModel
import com.fitness.app.ui.viewmodel.repoViewModelFactory
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * 今日页（原首页）：Diary 式今日记录页——打开 App 第一眼就是今天：
 * 周日期条 → 概览卡片（可自定义增删排序，第 4 期）→ 按餐次的记录，到此为止
 * （v3.5 主页瘦身：动作库区块已移除，入口在训练 Tab；默认只显示热量总账卡）。
 * 状态与聚合在 HomeViewModel（第 3 期迁出）；卡片组件在 HomeCards，
 * 周条/餐次/日记行在 HomeDiary。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    repo: ExerciseRepository,
    onNavigate: (String) -> Unit
) {
    val vm: HomeViewModel = viewModel(factory = repoViewModelFactory { HomeViewModel(repo) })
    val snackbar = remember { SnackbarHostState() }

    val selectedDate by vm.selectedDate.collectAsStateWithLifecycle()
    val logs by vm.logs.collectAsStateWithLifecycle()
    val summary by vm.daySummary.collectAsStateWithLifecycle()
    val trends by vm.trends.collectAsStateWithLifecycle()
    val tdeeOverride by vm.tdeeOverride.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    val activeSession by vm.activeSession.collectAsStateWithLifecycle()
    val cardConfigs by vm.cardConfigs.collectAsStateWithLifecycle()
    val editCardsMode by vm.editCardsMode.collectAsStateWithLifecycle()
    val editLog by vm.editLog.collectAsStateWithLifecycle()
    val quickAddMeal by vm.quickAddMeal.collectAsStateWithLifecycle()
    val saveTemplateMeal by vm.saveTemplateMeal.collectAsStateWithLifecycle()
    val showTdee by vm.showTdee.collectAsStateWithLifecycle()
    val showWeight by vm.showWeight.collectAsStateWithLifecycle()
    val showQuickSheet by vm.showQuickSheet.collectAsStateWithLifecycle()

    // 跨天回到前台时若正停在"今天"则跟随刷新
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) vm.onResume()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(vm) {
        vm.messages.collect { snackbar.showSnackbar(it) }
    }

    val isToday = selectedDate == LocalDate.now()
    val dateKey = DateKeys.of(selectedDate)
    val latestWeight = summary.latestWeight

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            FloatingActionButton(onClick = { vm.setShowQuickSheet(true) }) {
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
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
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
                        TextButton(onClick = { vm.backToToday() }) { Text("回到今天") }
                    }
                    IconButton(onClick = { onNavigate(Destinations.Calendar.route) }) {
                        Icon(Icons.Outlined.CalendarMonth, contentDescription = "日历")
                    }
                    if (!editCardsMode) {
                        TextButton(onClick = { vm.setEditCardsMode(true) }) { Text("编辑卡片") }
                    }
                }
            }

            // 周日期条
            item {
                WeekStrip(
                    selected = selectedDate,
                    onSelect = { date -> vm.selectDate(date) }
                )
            }

            // 概览卡片（可自定义：第 4 期）
            if (editCardsMode) {
                item {
                    HomeCardManager(
                        configs = cardConfigs,
                        onMove = { id, up -> vm.moveCard(id, up) },
                        onToggleVisible = { id, visible -> vm.setCardVisible(id, visible) },
                        onDone = { vm.setEditCardsMode(false) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            } else {
                cardConfigs.filter { it.visible }.forEach { cfg ->
                    when (cfg.id) {
                        HomeCards.BUDGET -> item(key = "card_budget") {
                            BudgetCard(
                                budget = summary.budget,
                                intake = summary.intake,
                                burn = summary.burn,
                                remaining = summary.remaining,
                                protein = summary.protein,
                                carbs = summary.carbs,
                                fat = summary.fat,
                                onClick = { vm.setShowTdee(true) }
                            )
                        }
                        HomeCards.TRAINING -> item(key = "card_training") {
                            TodayTrainingCard(
                                finishedCount = summary.daySessions.size,
                                finishedDurationSec = summary.daySessions.sumOf { it.durationSec },
                                hasActiveToday = activeSession != null &&
                                    activeSession?.dateKey == DateKeys.today(),
                                planName = summary.todayPlan?.plan?.name,
                                planItemCount = summary.todayPlan?.items?.size ?: 0,
                                isToday = isToday,
                                onStart = {
                                    summary.todayPlan?.let { pw ->
                                        repo.requestPlan(pw.plan.id)
                                        onNavigate(Destinations.Training.route)
                                    }
                                },
                                onOpenTraining = { onNavigate(Destinations.Training.route) },
                                onSchedule = { onNavigate(Destinations.Calendar.route) },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                        HomeCards.WEIGHT -> item(key = "card_weight") {
                            WeightCard(
                                latestKg = latestWeight?.weightKg,
                                latestDate = latestWeight?.dateKey,
                                delta = summary.weightDelta,
                                onRecord = { vm.setShowWeight(true) },
                                onOpenProgress = { onNavigate(Destinations.Weight.route) },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                        HomeCards.TREND -> item(key = "card_trend") {
                            TrendCard(
                                trends = trends,
                                onClick = { onNavigate(Destinations.Weight.route) },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // 按餐次记录（主体内容，不参与卡片自定义）
            Meals.ALL.forEach { meal ->
                val mealLogs = logs.filter { it.meal == meal }
                    .sortedBy { it.addedAt }
                item(key = "meal_$meal") {
                    MealSectionHeader(
                        meal = meal,
                        subtotalKcal = mealLogs.sumOf { it.energy },
                        recommended = Meals.recommendedRange(meal, summary.budget),
                        hasLogs = mealLogs.isNotEmpty(),
                        onAdd = {
                            onNavigate(Destinations.AddFood.create(dateKey, Meals.ALL.indexOf(meal)))
                        },
                        onCopyYesterday = { vm.copyYesterday(meal) },
                        onSaveTemplate = { vm.openSaveTemplate(meal) },
                        onQuickAdd = { vm.openQuickAdd(meal) }
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
                            onClick = { vm.openEditLog(log) },
                            onDelete = { vm.deleteLog(log) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 3.dp)
                        )
                    }
                }
            }

        }
    }

    // ---- 对话框 ----
    editLog?.let { log ->
        EditGramsDialog(
            log = log,
            onDismiss = { vm.dismissEditLog() },
            onConfirm = { grams -> vm.confirmEditLog(grams) }
        )
    }
    quickAddMeal?.let { meal ->
        QuickAddDialog(
            meal = meal,
            onDismiss = { vm.dismissQuickAdd() },
            onConfirm = { kcal, name -> vm.confirmQuickAdd(kcal, name) }
        )
    }
    saveTemplateMeal?.let { meal ->
        val mealLogs = logs.filter { it.meal == meal }
        SaveMealTemplateDialog(
            meal = meal,
            itemCount = mealLogs.size,
            onDismiss = { vm.dismissSaveTemplate() },
            onConfirm = { name -> vm.confirmSaveTemplate(name) }
        )
    }
    if (showTdee) {
        TdeeDialog(
            overrideKcal = tdeeOverride,
            computedKcal = computeTdee(profile, latestWeight?.weightKg),
            onSaveOverride = { v -> vm.saveTdeeOverride(v) },
            onDismiss = { vm.setShowTdee(false) },
            onEditProfile = {
                vm.setShowTdee(false)
                onNavigate(Destinations.ProfileEdit.route)
            }
        )
    }
    if (showWeight) {
        WeightEntryDialog(
            date = LocalDate.now(),
            initialKg = latestWeight?.weightKg,
            onDismiss = { vm.setShowWeight(false) },
            onConfirm = { key, kg -> vm.recordWeight(key, kg) }
        )
    }
    if (showQuickSheet) {
        ModalBottomSheet(onDismissRequest = { vm.setShowQuickSheet(false) }) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                QuickActionRow("记饮食", "添加今天吃的食物") {
                    vm.setShowQuickSheet(false)
                    onNavigate(
                        Destinations.AddFood.create(
                            DateKeys.today(),
                            Meals.ALL.indexOf(vm.inferMealNow())
                        )
                    )
                }
                QuickActionRow("记体重", "记录今天的体重") {
                    vm.setShowQuickSheet(false)
                    vm.setShowWeight(true)
                }
                QuickActionRow("开始训练", "去训练页开始一次训练") {
                    vm.setShowQuickSheet(false)
                    onNavigate(Destinations.Training.route)
                }
            }
        }
    }
}
