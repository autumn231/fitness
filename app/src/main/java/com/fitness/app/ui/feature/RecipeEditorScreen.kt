package com.fitness.app.ui.feature

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitness.app.data.ExerciseRepository
import com.fitness.app.data.model.Recipe
import com.fitness.app.data.model.RecipeIngredient
import com.fitness.app.ui.common.EmptyState
import com.fitness.app.ui.theme.CardShape
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val EDITOR_CATEGORIES = listOf("增肌", "减脂", "家常")

/** 食材编辑行：克数/备注用文本状态承载，保存时再解析为数字。 */
private class IngEditState(
    val foodId: String,
    val foodName: String,
    gram: Double,
    note: String
) {
    var gramText by mutableStateOf(
        if (gram == gram.toLong().toDouble()) gram.toLong().toString() else gram.toString()
    )
    var noteText by mutableStateOf(note)
}

/**
 * 食谱编辑页：recipeId 为 "new" 时新建，否则编辑对应的用户自建食谱。
 * 食材从食物库搜索添加，营养预览与详情页同一口径（食物库实时汇总）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeEditorScreen(
    repo: ExerciseRepository,
    recipeId: String,
    onBack: () -> Unit,
    onDone: () -> Unit
) {
    val isNew = recipeId == "new"
    val allRecipes by repo.observeAllRecipes()
        .collectAsStateWithLifecycle(initialValue = repo.recipes())
    val existing = if (isNew) null else allRecipes.firstOrNull { it.id == recipeId }
    var waited by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(800)
        waited = true
    }

    when {
        isNew -> RecipeEditorForm(repo, null, onBack, onDone)
        existing != null -> RecipeEditorForm(repo, existing, onBack, onDone)
        !waited -> Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("编辑食谱", fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    }
                )
            }
        ) { inner ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        }
        else -> Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("编辑食谱", fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    }
                )
            }
        ) { inner ->
            EmptyState(
                icon = Icons.Outlined.RestaurantMenu,
                title = "食谱不存在",
                subtitle = "该食谱可能已被删除",
                modifier = Modifier.padding(inner)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecipeEditorForm(
    repo: ExerciseRepository,
    initial: Recipe?,
    onBack: () -> Unit,
    onDone: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var category by remember { mutableStateOf(initial?.category?.ifBlank { "家常" } ?: "家常") }
    var minutesText by remember {
        mutableStateOf(initial?.minutes?.takeIf { it > 0 }?.toString() ?: "")
    }
    var tagsText by remember { mutableStateOf(initial?.tags?.joinToString("，") ?: "") }
    var stepsText by remember { mutableStateOf(initial?.steps?.joinToString("\n") ?: "") }
    val ingredients = remember {
        mutableStateListOf<IngEditState>().apply {
            initial?.ingredients?.forEach { ing ->
                add(
                    IngEditState(
                        foodId = ing.foodId,
                        foodName = repo.foodById(ing.foodId)?.name ?: ing.foodId,
                        gram = ing.gram,
                        note = ing.note
                    )
                )
            }
        }
    }
    var foodQuery by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val searchResults = remember(foodQuery) {
        val q = foodQuery.trim()
        if (q.isEmpty()) emptyList()
        else repo.allFoods().filter { it.name.contains(q) }.take(15)
    }

    // 当前表单拼出的食谱草稿：营养预览、保存都用它
    val draft = Recipe(
        id = initial?.id ?: "new",
        title = title.trim(),
        category = category,
        minutes = minutesText.toIntOrNull() ?: 0,
        tags = tagsText.split("，", ",").map { it.trim() }.filter { it.isNotEmpty() },
        ingredients = ingredients.mapNotNull { st ->
            st.gramText.toDoubleOrNull()?.takeIf { it > 0 }
                ?.let { RecipeIngredient(st.foodId, it, st.noteText.trim()) }
        },
        steps = stepsText.lines().map { it.trim() }.filter { it.isNotEmpty() },
        editable = true
    )
    val totals = repo.recipeNutrition(draft)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (initial == null) "新建食谱" else "编辑食谱",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    error?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                    Button(
                        onClick = {
                            when {
                                draft.title.isEmpty() -> error = "先给食谱起个名字"
                                draft.ingredients.isEmpty() -> error = "至少加一样食材，并填好克数"
                                !saving -> {
                                    error = null
                                    saving = true
                                    scope.launch {
                                        repo.saveUserRecipe(draft)
                                        saving = false
                                        onDone()
                                    }
                                }
                            }
                        },
                        enabled = !saving,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (saving) "正在保存…" else "保存食谱")
                    }
                }
            }
        }
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("食谱名") },
                    placeholder = { Text("如：番茄炒蛋") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(EDITOR_CATEGORIES) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = minutesText,
                        onValueChange = { minutesText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("用时（分钟）") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = tagsText,
                        onValueChange = { tagsText = it },
                        label = { Text("标签（逗号分隔）") },
                        placeholder = { Text("如：快手，下饭") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "食材",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        "约 ${totals.energy.toInt()} 千卡 · 蛋白质 ${fmtGram(totals.protein)} 克",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(ingredients, key = { it.foodId }) { st ->
                Surface(
                    shape = CardShape,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(start = 12.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(st.foodName, style = MaterialTheme.typography.bodyMedium)
                            OutlinedTextField(
                                value = st.noteText,
                                onValueChange = { st.noteText = it },
                                placeholder = { Text("处理备注（可空）") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(
                            value = st.gramText,
                            onValueChange = { st.gramText = it },
                            suffix = { Text("克") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.width(104.dp)
                        )
                        IconButton(onClick = { ingredients.remove(st) }) {
                            Icon(Icons.Outlined.Close, contentDescription = "移除食材")
                        }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = foodQuery,
                    onValueChange = { foodQuery = it },
                    placeholder = { Text("搜索食物库添加食材，如：鸡蛋") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            items(searchResults, key = { it.id }) { food ->
                Surface(
                    shape = CardShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (ingredients.any { it.foodId == food.id }) {
                                error = "「${food.name}」已在食材清单里"
                            } else {
                                error = null
                                ingredients.add(IngEditState(food.id, food.name, 100.0, ""))
                                foodQuery = ""
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(food.name, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.weight(1f))
                        Text(
                            "${food.energy.toInt()} 千卡/100克",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Text(
                    "做法（一行一步）",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            item {
                OutlinedTextField(
                    value = stepsText,
                    onValueChange = { stepsText = it },
                    placeholder = { Text("鸡蛋打散\n热锅少油炒至凝固盛出\n……") },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item { Spacer(Modifier.height(4.dp)) }
        }
    }
}

private fun fmtGram(v: Double): String =
    if (v == v.toLong().toDouble()) v.toLong().toString() else "%.1f".format(v)
