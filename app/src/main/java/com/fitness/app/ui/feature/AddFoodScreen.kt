package com.fitness.app.ui.feature

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitness.app.data.ExerciseRepository
import com.fitness.app.data.local.MealTemplateWithItems
import com.fitness.app.data.model.Food
import com.fitness.app.data.model.Recipe
import com.fitness.app.ui.common.EmptyState
import com.fitness.app.ui.common.SectionHeader
import com.fitness.app.ui.theme.CardShape
import kotlinx.coroutines.launch

/**
 * 添加食物页：默认落地「最近/最常吃 + 常用餐」，搜索退居其次——
 * 大多数记录是重复记录，不该每次都从搜索框开始。
 * 加入一条后留在本页继续添加（配合调用方保留列表状态，现场不丢）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFoodScreen(
    repo: ExerciseRepository,
    dateKey: String,
    meal: String,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Food>>(emptyList()) }
    LaunchedEffect(query) {
        results = if (query.isBlank()) emptyList() else repo.searchFoods(query, limit = 50)
    }

    // 最近/最常吃：(食物, 上次克数)
    var recentFoods by remember { mutableStateOf<List<Pair<Food, Double>>>(emptyList()) }
    var frequentFoods by remember { mutableStateOf<List<Pair<Food, Double>>>(emptyList()) }
    var reloadTick by remember { mutableStateOf(0) }
    LaunchedEffect(reloadTick) {
        val entries = repo.recentFoodEntries()
            .filter { it.foodId != ExerciseRepository.QUICK_ADD_FOOD_ID }
        recentFoods = entries
            .mapNotNull { e -> repo.foodById(e.foodId)?.let { it to e.amountGram } }
            .distinctBy { it.first.id }
            .take(15)
        frequentFoods = entries.groupBy { it.foodId }
            .entries.sortedByDescending { it.value.size }
            .mapNotNull { (foodId, list) ->
                repo.foodById(foodId)?.let { it to (list.first().amountGram) }
            }
            .take(15)
    }

    val templates by repo.observeMealTemplates()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val recipes by repo.observeAllRecipes()
        .collectAsStateWithLifecycle(initialValue = repo.recipes())

    var dialogFood by remember { mutableStateOf<Pair<Food, Double>?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("添加食物 · $meal", fontWeight = FontWeight.SemiBold)
                        Text(
                            text = dateKey,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("搜索食物，如：包子、鸡胸肉…") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
            }

            if (query.isNotBlank()) {
                if (results.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Outlined.RestaurantMenu,
                            title = "未找到相关食物",
                            subtitle = "试试更通用的名称"
                        )
                    }
                } else {
                    items(results, key = { it.id }) { food ->
                        FoodCard(
                            food = food,
                            onClick = { dialogFood = food to 100.0 },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }
            } else {
                if (templates.isNotEmpty()) {
                    item { SectionHeader(title = "常用餐") }
                    items(templates, key = { it.template.id }) { t ->
                        MealTemplateRow(
                            template = t,
                            onApply = {
                                scope.launch {
                                    repo.applyMealTemplate(t, dateKey, meal)
                                    onBack()
                                }
                            },
                            onDelete = {
                                scope.launch { repo.deleteMealTemplate(t.template.id) }
                            },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }

                if (recipes.isNotEmpty()) {
                    item { SectionHeader(title = "食谱") }
                    items(recipes, key = { "recipe_" + it.id }) { recipe ->
                        RecipePickRow(
                            repo = repo,
                            recipe = recipe,
                            onApply = {
                                scope.launch {
                                    repo.addRecipeToDate(recipe, 1.0, dateKey, meal)
                                    onBack()
                                }
                            },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }

                if (recentFoods.isNotEmpty()) {
                    item { SectionHeader(title = "最近吃过") }
                    items(recentFoods, key = { "r_" + it.first.id }) { (food, grams) ->
                        RecentFoodRow(
                            food = food,
                            lastGrams = grams,
                            onClick = { dialogFood = food to grams },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }

                if (frequentFoods.isNotEmpty()) {
                    item { SectionHeader(title = "最常吃") }
                    items(frequentFoods, key = { "f_" + it.first.id }) { (food, grams) ->
                        RecentFoodRow(
                            food = food,
                            lastGrams = grams,
                            onClick = { dialogFood = food to grams },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }

                if (templates.isEmpty() && recipes.isEmpty() &&
                    recentFoods.isEmpty() && frequentFoods.isEmpty()
                ) {
                    item {
                        EmptyState(
                            icon = Icons.Outlined.RestaurantMenu,
                            title = "还没有记录历史",
                            subtitle = "先用上方搜索添加几次，之后这里会自动出现你常吃的食物"
                        )
                    }
                }
            }
        }
    }

    dialogFood?.let { (food, grams) ->
        AddFoodDialog(
            food = food,
            initialGrams = grams,
            onDismiss = { dialogFood = null },
            onConfirm = { g ->
                scope.launch {
                    repo.addFoodLog(food, g, dateKey, meal)
                    dialogFood = null
                    reloadTick++
                    snackbar.showSnackbar("已加入 ${food.name} ${g.toInt()}g")
                }
            }
        )
    }
}

@Composable
private fun RecentFoodRow(
    food: Food,
    lastGrams: Double,
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
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = food.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "上次 ${lastGrams.toInt()}g · ${food.energy.toInt()} kcal / 100g",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "添加",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun MealTemplateRow(
    template: MealTemplateWithItems,
    onApply: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalKcal = template.items.sumOf { it.energy }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onApply),
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = template.template.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${template.items.size} 样 · 共 ${totalKcal.toInt()} kcal，点按整餐加入",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "删除模板",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

/** 食谱整单加入行：与「常用餐」同款式，点按按 1 份把全部食材写入当前日期餐次。 */
@Composable
private fun RecipePickRow(
    repo: ExerciseRepository,
    recipe: Recipe,
    onApply: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totals = repo.recipeNutrition(recipe)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onApply),
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recipe.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${recipe.ingredients.size} 样食材 · 约 ${totals.energy.toInt()} kcal，点按整单加入",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "整单加入",
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
