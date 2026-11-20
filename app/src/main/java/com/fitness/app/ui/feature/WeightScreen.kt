package com.fitness.app.ui.feature

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitness.app.data.DateKeys
import com.fitness.app.data.ExerciseRepository
import com.fitness.app.data.local.WeightLogEntity
import com.fitness.app.ui.common.EmptyState
import kotlinx.coroutines.launch

/**
 * 体重进度页：最新体重 + 趋势折线（最近 90 条）+ 历史列表（点按改克数式的编辑、删除）。
 * 蓝图 v2 第 2 期：体重落库、日记页卡片点进此处。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightScreen(
    repo: ExerciseRepository,
    onBack: () -> Unit
) {
    val weights by repo.observeWeights().collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    var showAdd by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<WeightLogEntity?>(null) }
    var deleteTarget by remember { mutableStateOf<WeightLogEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("体重进度", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Filled.Add, contentDescription = "记体重")
            }
        }
    ) { padding ->
        if (weights.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                EmptyState(
                    title = "还没有体重记录",
                    subtitle = "点右下角 + 记下今天的体重，攒起来就能看趋势了"
                )
            }
        } else {
            // observeWeights 为日期升序：末条是最新
            val latest = weights.last()
            val previous = weights.getOrNull(weights.size - 2)
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Spacer(Modifier.height(4.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("最新体重", style = MaterialTheme.typography.labelMedium)
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    "${formatKg(latest.weightKg)} kg",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (previous != null) {
                                    val delta = latest.weightKg - previous.weightKg
                                    val arrow = when {
                                        delta > 0.05 -> "▲ +${formatKg(delta)}"
                                        delta < -0.05 -> "▼ ${formatKg(delta)}"
                                        else -> "→ 持平"
                                    }
                                    Text(
                                        "  $arrow（较上次 ${previous.dateKey}）",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (weights.size >= 2) {
                                Spacer(Modifier.height(12.dp))
                                WeightTrendChart(
                                    entries = weights.takeLast(90),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                )
                            }
                        }
                    }
                }
                item {
                    Text(
                        "历史记录",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(weights.reversed(), key = { it.dateKey }) { entry ->
                    Card(onClick = { editTarget = entry }) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(entry.dateKey, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "点按修改",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                "${formatKg(entry.weightKg)} kg",
                                fontWeight = FontWeight.SemiBold
                            )
                            IconButton(onClick = { deleteTarget = entry }) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "删除",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }

    if (showAdd) {
        WeightEntryDialog(
            initialKg = weights.lastOrNull()?.weightKg,
            onDismiss = { showAdd = false },
            onConfirm = { key, kg ->
                scope.launch { repo.setWeight(key, kg) }
                showAdd = false
            }
        )
    }
    editTarget?.let { entry ->
        WeightEntryDialog(
            date = DateKeys.parse(entry.dateKey) ?: java.time.LocalDate.now(),
            initialKg = entry.weightKg,
            onDismiss = { editTarget = null },
            onConfirm = { key, kg ->
                scope.launch { repo.setWeight(key, kg) }
                editTarget = null
            }
        )
    }
    deleteTarget?.let { entry ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除这条体重记录？") },
            text = { Text("${entry.dateKey}：${formatKg(entry.weightKg)} kg") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { repo.removeWeight(entry.dateKey) }
                    deleteTarget = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun WeightTrendChart(entries: List<WeightLogEntity>, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
    Canvas(modifier = modifier) {
        if (entries.size < 2) return@Canvas
        val minKg = entries.minOf { it.weightKg }
        val maxKg = entries.maxOf { it.weightKg }
        val span = (maxKg - minKg).coerceAtLeast(1.0)
        val padY = size.height * 0.12f
        val stepX = size.width / (entries.size - 1)
        fun point(i: Int): Offset {
            val frac = ((entries[i].weightKg - minKg) / span).toFloat()
            return Offset(
                x = stepX * i,
                y = size.height - padY - frac * (size.height - 2 * padY)
            )
        }
        // 网格线（最高/最低）
        drawLine(gridColor, Offset(0f, padY), Offset(size.width, padY), strokeWidth = 1f)
        drawLine(gridColor, Offset(0f, size.height - padY), Offset(size.width, size.height - padY), strokeWidth = 1f)
        val path = Path()
        entries.indices.forEach { i ->
            val p = point(i)
            if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
        }
        drawPath(path, color = lineColor, style = Stroke(width = 3f, cap = StrokeCap.Round))
        entries.indices.forEach { i ->
            drawCircle(color = lineColor, radius = 4f, center = point(i))
        }
    }
}
