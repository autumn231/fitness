package com.fitness.app.ui.feature

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.LocalDining
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import com.fitness.app.data.ExerciseRepository
import com.fitness.app.data.model.Food
import com.fitness.app.ui.common.InfoRow
import com.fitness.app.ui.theme.CardShape
import com.fitness.app.ui.theme.ChipShape
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodDetailScreen(
    repo: ExerciseRepository,
    foodId: String,
    onBack: () -> Unit
) {
    val food = remember(foodId) { repo.foodById(foodId) }
    val scope = rememberCoroutineScope()

    // 输入克数，默认 100
    var amountText by remember { mutableStateOf("100") }
    val amount = remember(amountText) {
        amountText.trim().toDoubleOrNull()?.takeIf { it > 0 } ?: 0.0
    }
    val factor = amount / 100.0

    var justAdded by remember { mutableStateOf(false) }

    if (food == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("食物不存在")
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("营养详情", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Hero 卡片：食物名 + 每 100g 4 大营养
            FoodHeroCard(food = food)

            // 摄入克数输入
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = CardShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "记录摄入量",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { s -> amountText = s.filter { it.isDigit() || it == '.' }.take(6) },
                            label = { Text("克数 (g)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(12.dp))
                        // 快捷按钮
                        Column {
                            QuickPill("100g") { amountText = "100" }
                            Spacer(Modifier.height(4.dp))
                            QuickPill("200g") { amountText = "200" }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    // 实时计算的能量与营养
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        NutrientBadge(label = "能量", value = "${(food.energy * factor).toInt()}", unit = "kcal")
                        NutrientBadge(label = "蛋白", value = "${(food.protein * factor).toInt()}", unit = "g")
                        NutrientBadge(label = "碳水", value = "${(food.carbs * factor).toInt()}", unit = "g")
                        NutrientBadge(label = "脂肪", value = "${(food.fat * factor).toInt()}", unit = "g")
                    }
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = {
                            if (amount > 0) {
                                scope.launch {
                                    repo.addFoodLog(food, amount)
                                    justAdded = true
                                    onBack()
                                }
                            }
                        },
                        enabled = amount > 0,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("加入今日记录")
                    }
                }
            }

            // 详细营养信息（每 100g）
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = CardShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "每 100g 营养成分",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))
                    InfoRow(label = "可食部", value = "${food.edible.toInt()}%")
                    InfoRow(label = "能量", value = "${food.energy.toInt()} kcal (${(food.energy * 4.184).toInt()} kJ)")
                    InfoRow(label = "蛋白质", value = "${food.protein} g")
                    InfoRow(label = "脂肪", value = "${food.fat} g")
                    InfoRow(label = "碳水化合物", value = "${food.carbs} g")
                    InfoRow(label = "膳食纤维", value = "${food.fiber} g")
                    InfoRow(label = "胆固醇", value = "${food.cholesterol.toInt()} mg")
                    InfoRow(label = "钙", value = "${food.calcium.toInt()} mg")
                    InfoRow(label = "铁", value = "${food.iron} mg")
                    InfoRow(label = "钠", value = "${food.sodium.toInt()} mg")
                    InfoRow(label = "钾", value = "${food.potassium.toInt()} mg")
                    InfoRow(label = "维生素 C", value = "${food.vitaminC.toInt()} mg")
                    if (food.remark.isNotBlank()) {
                        InfoRow(label = "备注", value = food.remark)
                    }
                }
            }

            Text(
                text = "数据来源：《中国食物成分表》标准版第6版",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            )
        }
    }
}

@Composable
private fun FoodHeroCard(food: Food) {
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
            tint = Color.White.copy(alpha = 0.14f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 20.dp)
                .size(110.dp)
        )
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = food.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${food.category} · ${food.subcategory}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.92f)
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                HeroMetric("能量", "${food.energy.toInt()}", "kcal")
                HeroMetric("蛋白", "${food.protein.toInt()}", "g")
                HeroMetric("碳水", "${food.carbs.toInt()}", "g")
                HeroMetric("脂肪", "${food.fat.toInt()}", "g")
            }
        }
    }
}

@Composable
private fun HeroMetric(label: String, value: String, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.width(2.dp))
            Text(
                text = unit,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.85f)
        )
    }
}

@Composable
private fun NutrientBadge(label: String, value: String, unit: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(2.dp))
            Text(
                text = unit,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 3.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun QuickPill(text: String, onClick: () -> Unit) {
    Surface(
        shape = ChipShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .clip(ChipShape)
            .clickable(onClick = onClick)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}
