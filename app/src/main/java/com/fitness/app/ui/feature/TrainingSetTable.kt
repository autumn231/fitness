package com.fitness.app.ui.feature

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fitness.app.data.local.WorkoutSetEntity
import com.fitness.app.ui.common.BorderedCard
import com.fitness.app.ui.common.LocalAssetImage
import com.fitness.app.ui.theme.ImageShape

/** 动作组表卡：组号 / 上次 / 重量 / 次数 / 完成勾。 */
@Composable
fun ExerciseSetCard(
    name: String,
    subtitle: String,
    imagePath: String,
    sets: List<WorkoutSetEntity>,
    previous: List<WorkoutSetEntity>,
    onOpenDetail: () -> Unit,
    onUpdateSet: (WorkoutSetEntity) -> Unit,
    onToggleDone: (WorkoutSetEntity) -> Unit,
    onAddSet: () -> Unit,
    onRemoveExercise: () -> Unit
) {
    BorderedCard {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(ImageShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(onClick = onOpenDetail)
                ) {
                    if (imagePath.isNotBlank()) {
                        LocalAssetImage(
                            path = imagePath,
                            contentDescription = name,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subtitle.isNotBlank()) {
                        // 英文原名是区分具体动作的唯一标识，给正文级字号，别用小字灰字
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                IconButton(onClick = onRemoveExercise) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "移除动作",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            // 计时类动作（平板支撑等）：第二列记秒数而不是次数
            val timed = sets.any { it.durationSec > 0 }
            // 表头
            Row(verticalAlignment = Alignment.CenterVertically) {
                SetCellText("组", Modifier.width(28.dp))
                SetCellText("上次", Modifier.width(64.dp))
                SetCellText("kg", Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                SetCellText(if (timed) "秒" else "次数", Modifier.weight(1f))
                Spacer(Modifier.width(48.dp))
            }
            sets.forEach { set ->
                val prev = previous.getOrNull(set.setIndex) ?: previous.lastOrNull()
                SetRow(
                    set = set,
                    timed = timed,
                    previousText = prev?.let {
                        if (it.durationSec > 0) "${it.durationSec}秒" else "${formatKg(it.weightKg)}×${it.reps}"
                    } ?: "—",
                    onUpdate = onUpdateSet,
                    onToggleDone = { onToggleDone(set) }
                )
            }
            TextButton(onClick = onAddSet) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("加一组")
            }
        }
    }
}

@Composable
private fun SetCellText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

@Composable
private fun SetRow(
    set: WorkoutSetEntity,
    timed: Boolean,
    previousText: String,
    onUpdate: (WorkoutSetEntity) -> Unit,
    onToggleDone: () -> Unit
) {
    var weightText by remember(set.id) {
        mutableStateOf(if (set.weightKg % 1.0 == 0.0) set.weightKg.toInt().toString() else set.weightKg.toString())
    }
    var repsText by remember(set.id) {
        mutableStateOf(if (timed) set.durationSec.toString() else set.reps.toString())
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = (set.setIndex + 1).toString(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(28.dp)
        )
        Text(
            text = previousText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(64.dp)
        )
        OutlinedTextField(
            value = weightText,
            onValueChange = { s ->
                weightText = s.filter { it.isDigit() || it == '.' }.take(6)
                onUpdate(set.copy(weightKg = weightText.toDoubleOrNull() ?: 0.0))
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        OutlinedTextField(
            value = repsText,
            onValueChange = { s ->
                repsText = s.filter { it.isDigit() }.take(3)
                val v = repsText.toIntOrNull() ?: 0
                onUpdate(if (timed) set.copy(durationSec = v) else set.copy(reps = v))
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onToggleDone) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = if (set.done) "取消完成" else "完成这组",
                tint = if (set.done) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(
                        if (set.done) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .padding(5.dp)
            )
        }
    }
}
