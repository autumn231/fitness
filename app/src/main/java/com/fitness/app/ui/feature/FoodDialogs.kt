package com.fitness.app.ui.feature

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fitness.app.data.local.FoodLogEntity
import com.fitness.app.data.model.Food

/**
 * 添加食物对话框：输入克数时实时预览这一份的营养构成（Cronometer 式），
 * 确认后留在原地继续添加，不打断记录流。
 */
@Composable
fun AddFoodDialog(
    food: Food,
    initialGrams: Double = 100.0,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var text by remember(food.id) {
        mutableStateOf(
            if (initialGrams % 1.0 == 0.0) initialGrams.toInt().toString()
            else initialGrams.toString()
        )
    }
    val grams = text.trim().toDoubleOrNull()?.takeIf { it > 0 } ?: 0.0
    val f = grams / 100.0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(food.name) },
        text = {
            Column {
                Text(
                    text = "每 100g：${food.energy.toInt()} kcal · 蛋白 ${food.protein.toInt()}g · 碳水 ${food.carbs.toInt()}g · 脂肪 ${food.fat.toInt()}g",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { s -> text = s.filter { it.isDigit() || it == '.' }.take(6) },
                    label = { Text("克数 (g)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    PreviewMetric("能量", "${(food.energy * f).toInt()} kcal")
                    PreviewMetric("蛋白", "${(food.protein * f).toInt()} g")
                    PreviewMetric("碳水", "${(food.carbs * f).toInt()} g")
                    PreviewMetric("脂肪", "${(food.fat * f).toInt()} g")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (grams > 0) onConfirm(grams) },
                enabled = grams > 0
            ) { Text("添加") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun PreviewMetric(label: String, value: String) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** 修改已记录条目的克数。 */
@Composable
fun EditGramsDialog(
    log: FoodLogEntity,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var text by remember(log.id) {
        mutableStateOf(
            if (log.amountGram % 1.0 == 0.0) log.amountGram.toInt().toString()
            else log.amountGram.toString()
        )
    }
    val grams = text.trim().toDoubleOrNull()?.takeIf { it > 0 } ?: 0.0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(log.foodName) },
        text = {
            Column {
                Text(
                    text = "当前 ${log.amountGram.toInt()}g · ${log.energy.toInt()} kcal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { s -> text = s.filter { it.isDigit() || it == '.' }.take(6) },
                    label = { Text("克数 (g)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (grams > 0) onConfirm(grams) },
                enabled = grams > 0
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

/** 快速添加：只知道总热量时直接入账（外食/打包餐）。 */
@Composable
fun QuickAddDialog(
    meal: String,
    onDismiss: () -> Unit,
    onConfirm: (kcal: Double, name: String) -> Unit
) {
    var kcalText by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    val kcal = kcalText.trim().toDoubleOrNull()?.takeIf { it > 0 } ?: 0.0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("快速添加 · $meal") },
        text = {
            Column {
                Text(
                    text = "只填总热量即可入账，适合外食等无法拆分食材的情况。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = kcalText,
                    onValueChange = { s -> kcalText = s.filter { it.isDigit() || it == '.' }.take(6) },
                    label = { Text("热量 (kcal)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(20) },
                    label = { Text("名称（可选，如：食堂套餐）") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (kcal > 0) onConfirm(kcal, name.trim()) },
                enabled = kcal > 0
            ) { Text("入账") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

/** 把当前餐存为常用餐时的命名对话框。 */
@Composable
fun SaveMealTemplateDialog(
    meal: String,
    itemCount: Int,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("${meal}常用餐") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("存为常用餐") },
        text = {
            Column {
                Text(
                    text = "把这一餐的 $itemCount 样食物存成模板，下次在添加页一键整餐带入。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(20) },
                    label = { Text("模板名称") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim()) },
                enabled = name.isNotBlank()
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
