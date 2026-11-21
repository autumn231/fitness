package com.fitness.app.ui.feature

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitness.app.data.DateKeys
import com.fitness.app.data.ExerciseRepository
import com.fitness.app.data.Meals
import com.fitness.app.data.computeTdee
import com.fitness.app.data.effectiveTdee
import com.fitness.app.data.local.FoodLogEntity
import com.fitness.app.data.model.Food
import com.fitness.app.ui.common.EmptyState
import com.fitness.app.ui.nav.Destinations
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * 营养页：食物库（搜索/分类）+ 按日期回看的摄入记录（按餐次分组）。
 * 搜索状态用 rememberSaveable 保存，从食物详情返回不丢现场；
 * 每日总账的主场在「今日」页，这里保留同口径的汇总卡方便对照。
 * 卡片组件（汇总卡/日期条/食物卡/记录行）在 NutritionCards。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionScreen(
    repo: ExerciseRepository,
    onNavigate: (String) -> Unit
) {
    val scope = rememberCoroutineScope()

    // 查看日期：默认今天，可前后翻看/补记；停在"今天"时跨天自动跟随
    var viewedEpochDay by rememberSaveable { mutableStateOf(LocalDate.now().toEpochDay()) }
    var followingToday by rememberSaveable { mutableStateOf(true) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && followingToday) {
                viewedEpochDay = LocalDate.now().toEpochDay()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val viewedDate = LocalDate.ofEpochDay(viewedEpochDay)
    val isToday = viewedDate == LocalDate.now()
    val dateKey = DateKeys.of(viewedDate)

    val tdeeOverride by repo.settings.tdee.collectAsStateWithLifecycle(initialValue = 0)
    val profile by repo.observeProfile().collectAsStateWithLifecycle(initialValue = null)
    val latestWeight by repo.observeLatestWeight().collectAsStateWithLifecycle(initialValue = null)
    val budget = effectiveTdee(tdeeOverride, profile, latestWeight?.weightKg)

    val logs by repo.observeFoodLogs(dateKey)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val allFoods = remember { repo.allFoods() }
    val categories = remember(allFoods.size) { repo.foodCategories() }

    val totalEnergy = logs.sumOf { it.energy }
    val totalProtein = logs.sumOf { it.protein }
    val totalCarbs = logs.sumOf { it.carbs }
    val totalFat = logs.sumOf { it.fat }

    // 搜索状态保存：返回本页不清空（v2.4 在动作搜索页修过的同款问题，这里补上）
    var query by rememberSaveable { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Food>>(emptyList()) }
    LaunchedEffect(query, allFoods.size) {
        searchResults = if (query.isBlank()) emptyList()
        else repo.searchFoods(query, limit = 50)
    }
    val listState = rememberLazyListState()

    var showTdeeDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var editLog by remember { mutableStateOf<FoodLogEntity?>(null) }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            TopAppBar(
                title = { Text("营养", fontWeight = FontWeight.SemiBold) },
                // 顶栏底色与页面背景（background 灰白）一致，避免默认 surface 纯白与页面形成分层
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                ),
                windowInsets = WindowInsets(0, 0, 0, 0),
                actions = {
                    IconButton(onClick = { showTdeeDialog = true }) {
                        Icon(Icons.Outlined.Settings, contentDescription = "设置每日目标")
                    }
                }
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    placeholder = { Text("搜索食物") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
                Spacer(Modifier.width(8.dp))
                FilledTonalButton(
                    onClick = { onNavigate(Destinations.Recipes.route) },
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("食谱")
                }
            }
        }

        if (query.isNotBlank()) {
            item {
                Text(
                    text = "找到 ${searchResults.size} 个食物",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            if (searchResults.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Outlined.RestaurantMenu,
                        title = "未找到相关食物",
                        subtitle = "试试更通用的名称"
                    )
                }
            } else {
                items(searchResults, key = { it.id }) { food ->
                    FoodCard(
                        food = food,
                        onClick = {
                            onNavigate(
                                Destinations.FoodDetail.create(
                                    food.id, dateKey, Meals.inferNow()
                                )
                            )
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        } else {
            // 日期切换条
            item {
                DateSwitcher(
                    date = viewedDate,
                    isToday = isToday,
                    onPrev = {
                        viewedEpochDay -= 1
                        followingToday = false
                    },
                    onNext = {
                        if (!isToday) {
                            viewedEpochDay += 1
                            followingToday = LocalDate.ofEpochDay(viewedEpochDay) == LocalDate.now()
                        }
                    },
                    onToday = {
                        viewedEpochDay = LocalDate.now().toEpochDay()
                        followingToday = true
                    }
                )
            }

            item {
                CalorieSummaryCard(
                    tdee = budget,
                    intake = totalEnergy,
                    protein = totalProtein,
                    carbs = totalCarbs,
                    fat = totalFat,
                    dateLabel = if (isToday) "今日热量"
                    else "${viewedDate.monthValue}月${viewedDate.dayOfMonth}日热量",
                    onSetTdee = { showTdeeDialog = true }
                )
            }

            item {
                Text(
                    text = "食物分类",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(categories, key = { it.first }) { (cat, count) ->
                        CategoryPill(
                            name = cat,
                            count = count,
                            onClick = { onNavigate(Destinations.FoodList.create(cat)) }
                        )
                    }
                }
            }

            // 记录（按餐次分组）
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${if (isToday) "今日" else "当天"}记录 (${logs.size})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (logs.isNotEmpty()) {
                        TextButton(onClick = { showClearDialog = true }) {
                            Text("清空", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            if (logs.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Outlined.RestaurantMenu,
                        title = "这一天还没有记录",
                        subtitle = "搜索食物或在分类中找到它，记录克数后会自动计算热量与营养"
                    )
                }
            } else {
                Meals.ALL.forEach { meal ->
                    val mealLogs = logs.filter { it.meal == meal }.sortedBy { it.addedAt }
                    if (mealLogs.isNotEmpty()) {
                        item(key = "n_meal_$meal") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = meal,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${mealLogs.sumOf { it.energy }.toInt()} kcal",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        items(mealLogs, key = { it.id }) { log ->
                            FoodLogRow(
                                log = log,
                                onClick = { editLog = log },
                                onDelete = {
                                    scope.launch { repo.removeFoodLog(log.id) }
                                },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showTdeeDialog) {
        TdeeDialog(
            overrideKcal = tdeeOverride,
            computedKcal = computeTdee(profile, latestWeight?.weightKg),
            onSaveOverride = { v ->
                scope.launch {
                    repo.settings.setTdee(v)
                    showTdeeDialog = false
                }
            },
            onDismiss = { showTdeeDialog = false },
            onEditProfile = {
                showTdeeDialog = false
                onNavigate(Destinations.ProfileEdit.route)
            }
        )
    }

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

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清空当天记录？") },
            text = { Text("此操作将移除这一天的全部食物记录，无法撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { repo.clearFoodLogs(dateKey) }
                    showClearDialog = false
                }) { Text("清空", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("取消") }
            }
        )
    }
}
