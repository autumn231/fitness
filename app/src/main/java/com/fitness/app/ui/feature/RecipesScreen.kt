package com.fitness.app.ui.feature

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fitness.app.data.ExerciseRepository
import com.fitness.app.data.Meals
import com.fitness.app.data.model.Recipe
import com.fitness.app.ui.common.EmptyState
import com.fitness.app.ui.nav.Destinations
import com.fitness.app.ui.theme.CardShape
import kotlinx.coroutines.launch

private val CATEGORY_FILTERS = listOf("全部", "增肌", "减脂")

/** 食谱列表：按增肌/减脂筛选，营养数字由食物库实时汇总。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeListScreen(
    repo: ExerciseRepository,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    var filter by remember { mutableStateOf("全部") }
    val all = repo.recipes()
    val shown = if (filter == "全部") all else all.filter { it.category == filter }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("食谱", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(CATEGORY_FILTERS) { cat ->
                        FilterChip(
                            selected = filter == cat,
                            onClick = { filter = cat },
                            label = { Text(cat) }
                        )
                    }
                }
            }
            items(shown, key = { it.id }) { recipe ->
                RecipeCard(repo, recipe) { onNavigate(Destinations.RecipeDetail.create(recipe.id)) }
            }
        }
    }
}

@Composable
private fun RecipeCard(repo: ExerciseRepository, recipe: Recipe, onClick: () -> Unit) {
    val totals = repo.recipeNutrition(recipe)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = CardShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = recipe.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${recipe.category} · 约 ${recipe.minutes} 分钟" +
                    if (recipe.tags.isNotEmpty()) " · " + recipe.tags.joinToString(" / ") else "",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "约 ${totals.energy.toInt()} 千卡 · 蛋白质 ${fmt(totals.protein)} 克 · ${recipe.ingredients.size} 样食材",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** 食谱详情：食材（可按份数缩放）、营养汇总、做法，一键整单加入今日饮食。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    repo: ExerciseRepository,
    recipeId: String,
    onBack: () -> Unit
) {
    val recipe = repo.recipeById(recipeId)
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var servings by remember { mutableIntStateOf(1) }
    var meal by remember { mutableStateOf(Meals.inferNow()) }
    var adding by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(recipe?.title ?: "食谱", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (recipe != null) {
                Surface(shadowElevation = 8.dp) {
                    Button(
                        onClick = {
                            if (!adding) {
                                adding = true
                                scope.launch {
                                    val n = repo.addRecipeToToday(recipe, servings.toDouble(), meal)
                                    adding = false
                                    snackbar.showSnackbar("已加入今日$meal（$n 样食材）")
                                }
                            }
                        },
                        enabled = !adding,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(if (adding) "正在加入…" else "加入今日$meal")
                    }
                }
            }
        }
    ) { inner ->
        if (recipe == null) {
            EmptyState(
                icon = Icons.Outlined.RestaurantMenu,
                title = "食谱不存在",
                subtitle = "该食谱可能已被移除",
                modifier = Modifier.padding(inner)
            )
        } else {
            val totals = repo.recipeNutrition(recipe, servings.toDouble())
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = "${recipe.category} · 约 ${recipe.minutes} 分钟" +
                            if (recipe.tags.isNotEmpty()) " · " + recipe.tags.joinToString(" / ") else "",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }

                // 份数与餐次
                item {
                    Surface(
                        shape = CardShape,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("份数", fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.weight(1f))
                                IconButton(onClick = { if (servings > 1) servings-- }) {
                                    Text("－", style = MaterialTheme.typography.titleLarge)
                                }
                                Text(
                                    "$servings 份",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                IconButton(onClick = { if (servings < 4) servings++ }) {
                                    Text("＋", style = MaterialTheme.typography.titleLarge)
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text("加入到", fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(Meals.ALL) { m ->
                                    FilterChip(
                                        selected = meal == m,
                                        onClick = { meal = m },
                                        label = { Text(m) }
                                    )
                                }
                            }
                        }
                    }
                }

                // 营养汇总
                item {
                    Surface(
                        shape = CardShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            NutritionCell("热量", "${totals.energy.toInt()} 千卡")
                            NutritionCell("蛋白质", "${fmt(totals.protein)} 克")
                            NutritionCell("碳水", "${fmt(totals.carbs)} 克")
                            NutritionCell("脂肪", "${fmt(totals.fat)} 克")
                        }
                    }
                }

                item {
                    Text(
                        "食材",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                items(recipe.ingredients, key = { it.foodId }) { ing ->
                    val food = repo.foodById(ing.foodId)
                    Surface(
                        shape = CardShape,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(food?.name ?: ing.foodId, style = MaterialTheme.typography.bodyMedium)
                                if (ing.note.isNotBlank()) {
                                    Text(
                                        ing.note,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                "${fmt(ing.gram * servings)} 克",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                item {
                    Text(
                        "做法",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                items(recipe.steps.withIndex().toList(), key = { it.index }) { (idx, step) ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "${idx + 1}.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(step, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
private fun NutritionCell(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

private fun fmt(v: Double): String =
    if (v == v.toLong().toDouble()) v.toLong().toString() else "%.1f".format(v)
