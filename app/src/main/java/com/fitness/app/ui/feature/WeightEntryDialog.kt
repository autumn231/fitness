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
import com.fitness.app.data.DateKeys
import java.time.LocalDate

/** 记体重对话框：今日页、「我的」、体重进度页共用。一天一条，重复记录即覆盖。 */
@Composable
fun WeightEntryDialog(
    date: LocalDate = LocalDate.now(),
    initialKg: Double? = null,
    onDismiss: () -> Unit,
    onConfirm: (dateKey: String, kg: Double) -> Unit
) {
    var text by remember {
        mutableStateOf(initialKg?.let {
            if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()
        } ?: "")
    }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("记录体重") },
        text = {
            Column {
                Text(
                    text = "${date.monthValue}月${date.dayOfMonth}日 · 同一天重复记录会覆盖",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { s ->
                        text = s.filter { it.isDigit() || it == '.' }.take(6)
                        error = null
                    },
                    label = { Text("体重 (kg)") },
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val v = text.trim().toDoubleOrNull()
                when {
                    v == null -> error = "请输入数字"
                    v !in 25.0..350.0 -> error = "请输入 25 ~ 350 之间的数值"
                    else -> onConfirm(DateKeys.of(date), v)
                }
            }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
