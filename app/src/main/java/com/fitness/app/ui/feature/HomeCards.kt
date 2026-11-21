package com.fitness.app.ui.feature

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fitness.app.data.TrendConclusion
import com.fitness.app.ui.common.BorderedCard
import com.fitness.app.ui.theme.CardShape
import com.fitness.app.ui.viewmodel.HomeCardConfig
import com.fitness.app.ui.viewmodel.HomeCards

/** 热量总账卡：还可吃 = 目标 − 摄入 + 运动消耗（估算）。 */
@Composable
fun BudgetCard(
    budget: Int,
    intake: Double,
    burn: Int,
    remaining: Double,
    protein: Double,
    carbs: Double,
    fat: Double,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
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
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            if (budget <= 0) {
                Text(
                    text = "每日热量目标未设置",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "已摄入 ${intake.toInt()} kcal。点此设置目标，或完善档案后自动计算。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            } else {
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (remaining >= 0) "还可吃" else "已超出",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Text(
                            text = "${kotlin.math.abs(remaining).toInt()}",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "kcal",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        BudgetStat("目标", "$budget")
                        BudgetStat("已摄入", "${intake.toInt()}")
                        BudgetStat("运动消耗(估算)", "+$burn")
                    }
                }
                Spacer(Modifier.height(12.dp))
                val denom = (budget + burn).coerceAtLeast(1)
                LinearProgressIndicator(
                    progress = { (intake / denom).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.25f)
                )
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MacroProgress("蛋白质", protein, budget * 0.25 / 4)
                    MacroProgress("碳水", carbs, budget * 0.50 / 4)
                    MacroProgress("脂肪", fat, budget * 0.25 / 9)
                }
            }
        }
    }
}

@Composable
private fun BudgetStat(label: String, value: String) {
    Row {
        Text(
            text = "$label ",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.8f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun MacroProgress(label: String, eaten: Double, target: Double) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "${eaten.toInt()} / ${target.toInt()}g",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun WeightCard(
    latestKg: Double?,
    latestDate: String?,
    delta: Double?,
    onRecord: () -> Unit,
    onOpenProgress: () -> Unit,
    modifier: Modifier = Modifier
) {
    BorderedCard(modifier = modifier, onClick = onOpenProgress) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (latestKg != null) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = latestKg.toString(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "kg",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        if (delta != null && delta != 0.0) {
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = if (delta > 0) "↑ ${"%.1f".format(delta)}" else "↓ ${"%.1f".format(-delta)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (delta > 0) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }
                    Text(
                        text = "最近记录：$latestDate",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text("还没有体重记录", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "记录体重后，这里会显示趋势，热量目标也会自动计算。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            TextButton(onClick = onRecord) { Text("记体重") }
        }
    }
}

/** 趋势卡（第 4 期）：体重/训练/摄入三条方向结论，点进体重页看详情。 */
@Composable
fun TrendCard(
    trends: List<TrendConclusion>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BorderedCard(modifier = modifier, onClick = onClick) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "趋势 · 近 90 天 vs 长期",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            if (trends.all { !it.enoughData }) {
                Text(
                    text = "数据不足，继续记录几天再来看趋势。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                trends.forEach { t ->
                    TrendRow(t)
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun TrendRow(t: TrendConclusion) {
    val label = when (t.key) {
        "weight" -> "体重"
        "training" -> "训练"
        "intake" -> "摄入"
        else -> t.key
    }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(40.dp)
            )
            Text(
                text = t.headline,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (t.enoughData) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (t.enoughData) {
            Text(
                text = t.detail,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 40.dp)
            )
        }
    }
}

/** 卡片管理面板（编辑模式）：每张卡一行，上移/下移/显示隐藏，改动即存。 */
@Composable
fun HomeCardManager(
    configs: List<HomeCardConfig>,
    onMove: (id: String, up: Boolean) -> Unit,
    onToggleVisible: (id: String, visible: Boolean) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    BorderedCard(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "自定义概览卡片",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onDone) { Text("完成") }
            }
            Text(
                text = "调整顺序或隐藏不看的卡片，改动立即生效。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            configs.forEachIndexed { index, cfg ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = HomeCards.titleOf(cfg.id),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { onMove(cfg.id, true) },
                        enabled = index > 0
                    ) {
                        Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "上移")
                    }
                    IconButton(
                        onClick = { onMove(cfg.id, false) },
                        enabled = index < configs.lastIndex
                    ) {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "下移")
                    }
                    IconButton(onClick = { onToggleVisible(cfg.id, !cfg.visible) }) {
                        Icon(
                            imageVector = if (cfg.visible) Icons.Filled.Visibility
                            else Icons.Filled.VisibilityOff,
                            contentDescription = if (cfg.visible) "隐藏" else "显示",
                            tint = if (cfg.visible) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
