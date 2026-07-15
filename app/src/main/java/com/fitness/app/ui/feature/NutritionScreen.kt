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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.LocalDining
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitness.app.data.ExerciseRepository
import com.fitness.app.data.local.FoodLogEntity
import com.fitness.app.data.model.Food
import com.fitness.app.ui.common.EmptyState
import com.fitness.app.ui.nav.Destinations
import com.fitness.app.ui.theme.CardShape
import com.fitness.app.ui.theme.ChipShape
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionScreen(
    repo: ExerciseRepository,
    onNavigate: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val todayKey = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    val tdeeState = repo.settings.tdee.collectAsStateWithLifecycle(initialValue = 0)
    val tdee = tdeeState.value
    val logsState = repo.observeFoodLogs(todayKey)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val logs = logsState.value

    val allFoods = remember { repo.allFoods() }
    val categories = remember(allFoods.size) { repo.foodCategories() }

    // 营养汇总
    val totalEnergy = logs.sumOf { it.energy }
    val totalProtein = logs.sumOf { it.protein }
    val totalCarbs = logs.sumOf { it.carbs }
    val totalFat = logs.sumOf { it.fat }

    // 搜索状态
    var query by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Food>>(emptyList()) }
    LaunchedEffect(query, allFoods.size) {
        searchResults = if (query.isBlank()) emptyList()
        else repo.searchFoods(query, limit = 50)
    }

    var showTdeeDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // 顶部：搜索 + 标题
        item {
            TopAppBar(
                title = { Text("营养", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = { showTdeeDialog = true }) {
                        Icon(Icons.Outlined.Settings, contentDescription = "设置每日消耗")
                    }
                }
            )
        }

        // 搜索框
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = { Text("搜索食物，如：包子、鸡胸肉…") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )
        }

        // 搜索结果（折叠态时显示其他内容）
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
                        onClick = { onNavigate(Destinations.FoodDetail.create(food.id)) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        } else {
            // 今日热量大卡片
            item {
                CalorieSummaryCard(
                    tdee = tdee,
                    intake = totalEnergy,
                    protein = totalProtein,
                    carbs = totalCarbs,
                    fat = totalFat,
                    onSetTdee = { showTdeeDialog = true },
                    onClear = { showClearDialog = true }
                )
            }

            // 食物分类
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

            // 今日记录
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "今日记录 (${logs.size})",
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
                        title = "今天还没有记录",
                        subtitle = "搜索食物或在分类中找到它，记录克数后会自动计算热量与营养"
                    )
                }
            } else {
                items(logs, key = { it.id }) { log ->
                    FoodLogRow(
                        log = log,
                        onDelete = {
                            scope.launch { repo.removeFoodLog(log.id) }
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }

    if (showTdeeDialog) {
        TdeeDialog(
            current = tdee,
            onDismiss = { showTdeeDialog = false },
            onSave = {
                scope.launch {
                    repo.settings.setTdee(it)
                    showTdeeDialog = false
                }
            }
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清空今日记录？") },
            text = { Text("此操作将移除今天的全部食物记录，无法撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { repo.clearFoodLogs(todayKey) }
                    showClearDialog = false
                }) { Text("清空", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("取消") }
            }
        )
    }
}

/** 顶部热量总览卡：TDEE / 摄入 / 缺口 + 三大营养进度 */
@Composable
private fun CalorieSummaryCard(
    tdee: Int,
    intake: Double,
    protein: Double,
    carbs: Double,
    fat: Double,
    onSetTdee: () -> Unit,
    onClear: () -> Unit
) {
    val gap = tdee - intake   // 正值 = 缺口；负值 = 超出
    val progress = if (tdee > 0) (intake / tdee).toFloat().coerceIn(0f, 1f) else 0f
    val gapColor = when {
        tdee <= 0 -> MaterialTheme.colorScheme.onSurfaceVariant
        gap >= 800 -> MaterialTheme.colorScheme.error   // 缺口过大不健康
        gap >= 300 -> MaterialTheme.colorScheme.tertiary // 适中减脂
        gap >= 0 -> MaterialTheme.colorScheme.primary    // 接近持平
        else -> MaterialTheme.colorScheme.error          // 超出
    }
    val tip = when {
        tdee <= 0 -> "未设置每日消耗，点击右上角设置"
        gap >= 1000 -> "热量缺口过大，注意避免代谢下降"
        gap >= 500 -> "适合减脂，建议保持"
        gap >= 200 -> "温和减脂区间"
        gap >= 0 -> "已接近持平"
        else -> "今日摄入超出 ${(-gap).toInt()} kcal"
    }

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
    ) {
        Icon(
            imageVector = Icons.Outlined.LocalDining,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.12f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 20.dp)
                .size(110.dp)
        )
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "今日热量",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "${intake.toInt()}",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "/ ${if (tdee > 0) tdee else "?"} kcal",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.25f),
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricColumn(label = "缺口", value = if (tdee > 0) "${gap.toInt()}" else "—", valueColor = Color.White)
                MetricColumn(label = "蛋白质", value = "${protein.toInt()}g", valueColor = Color.White)
                MetricColumn(label = "碳水", value = "${carbs.toInt()}g", valueColor = Color.White)
                MetricColumn(label = "脂肪", value = "${fat.toInt()}g", valueColor = Color.White)
            }
            Spacer(Modifier.height(12.dp))
            Surface(
                shape = ChipShape,
                color = Color.White.copy(alpha = 0.18f)
            ) {
                Text(
                    text = tip,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(onClick = onSetTdee) {
                    Text(if (tdee > 0) "调整 TDEE" else "设置 TDEE")
                }
                if (intake > 0) {
                    FilledTonalButton(onClick = onClear) {
                        Text("清空今日")
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricColumn(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = valueColor.copy(alpha = 0.8f)
        )
    }
}

/** 食物分类 pill */
@Composable
private fun CategoryPill(name: String, count: Int, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = CardShape,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "$count 种",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** 食物列表卡片 */
@Composable
fun FoodCard(
    food: Food,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 圆形食物图标占位
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocalDining,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = food.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${food.subcategory} · ${food.energy.toInt()} kcal / 100g",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "P ${food.protein.toInt()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "C ${food.carbs.toInt()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "F ${food.fat.toInt()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** 今日记录行 */
@Composable
private fun FoodLogRow(
    log: FoodLogEntity,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
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
                    text = log.foodName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${log.amountGram.toInt()}g · 蛋白 ${log.protein.toInt()}g / 碳水 ${log.carbs.toInt()}g / 脂肪 ${log.fat.toInt()}g",
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

/** TDEE 设置对话框 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TdeeDialog(
    current: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var text by remember { mutableStateOf(if (current > 0) current.toString() else "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置每日能量消耗 (TDEE)") },
        text = {
            Column {
                Text(
                    text = "TDEE = 基础代谢 × 活动系数。可用 Mifflin-St Jeor 公式估算，或参考常用值：久坐 1800、轻度活动 2000、中度活动 2300、高强度 2600 kcal。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { s -> text = s.filter { it.isDigit() }.take(5) },
                    label = { Text("每日消耗 (kcal)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val v = text.trim().toIntOrNull() ?: 0
                if (v in 800..8000) onSave(v)
            }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
