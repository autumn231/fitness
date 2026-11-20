package com.fitness.app.ui.feature

import androidx.compose.foundation.layout.Column
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

/**
 * 每日热量目标（TDEE）设置：档案齐全时按 Mifflin-St Jeor 公式自动计算，
 * 这里只保留「手动覆盖」作为高级选项。营养页、今日页、设置页共用。
 */
@Composable
fun TdeeDialog(
    overrideKcal: Int,
    computedKcal: Int,
    onSaveOverride: (Int) -> Unit,
    onDismiss: () -> Unit,
    onEditProfile: (() -> Unit)? = null
) {
    var text by remember { mutableStateOf(if (overrideKcal > 0) overrideKcal.toString() else "") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("每日热量目标 (TDEE)") },
        text = {
            Column {
                when {
                    overrideKcal > 0 -> Text(
                        text = "当前使用手动设置：$overrideKcal kcal。清空输入并保存可恢复自动计算。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    computedKcal > 0 -> Text(
                        text = "已按你的档案自动计算：$computedKcal kcal（Mifflin-St Jeor 公式 × 活动系数）。体重变化时会自动更新。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    else -> Text(
                        text = "完善个人档案（性别/出生年/身高）并记录体重后，这里会自动算出你的每日消耗；也可以先手动填一个。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { s ->
                        text = s.filter { it.isDigit() }.take(5)
                        error = null
                    },
                    label = { Text("手动覆盖 (kcal，可留空)") },
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                if (onEditProfile != null) {
                    TextButton(onClick = onEditProfile) { Text("去完善个人档案") }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val t = text.trim()
                when {
                    t.isEmpty() -> onSaveOverride(0)
                    else -> {
                        val v = t.toIntOrNull()
                        when {
                            v == null -> error = "请输入数字"
                            v !in 800..8000 -> error = "请输入 800 ~ 8000 之间的数值"
                            else -> onSaveOverride(v)
                        }
                    }
                }
            }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
