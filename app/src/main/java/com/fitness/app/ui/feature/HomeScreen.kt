package com.fitness.app.ui.feature

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitness.app.data.ExerciseRepository
import com.fitness.app.i18n.groupByBodyPart
import com.fitness.app.ui.common.ExerciseCard
import com.fitness.app.ui.common.SectionHeader
import com.fitness.app.ui.nav.Destinations
import com.fitness.app.ui.theme.CardShape
import com.fitness.app.ui.theme.ChipShape
import com.fitness.app.ui.theme.ImageShape

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    repo: ExerciseRepository,
    onNavigate: (String) -> Unit
) {
    val recentsState = repo.observeRecents(8).collectAsStateWithLifecycle(initialValue = emptyList())
    val recents = recentsState.value
    val exercises = repo.all()
    val bodyParts = remember(exercises.size) { groupByBodyPart(exercises).take(10) }
    val featured = remember(exercises.size) {
        if (exercises.isEmpty()) emptyList()
        else exercises.shuffled().take(8)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Hero 区：渐变大卡片
        item {
            HeroCard(total = exercises.size, onSearch = { onNavigate(Destinations.Search.route) })
        }

        // 快捷入口：三宫格
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickEntry(
                    icon = Icons.Outlined.LocalFireDepartment,
                    label = "开始训练",
                    subtitle = "我的计划",
                    modifier = Modifier.weight(1f)
                ) { onNavigate(Destinations.Plan.route) }
                QuickEntry(
                    icon = Icons.Outlined.History,
                    label = "历史记录",
                    subtitle = "${recents.size} 条",
                    modifier = Modifier.weight(1f)
                ) { onNavigate(Destinations.Favorites.route) }
                QuickEntry(
                    icon = Icons.Outlined.FitnessCenter,
                    label = "动作分类",
                    subtitle = "${bodyParts.size} 大部位",
                    modifier = Modifier.weight(1f)
                ) { onNavigate(Destinations.Category.route) }
            }
        }

        // 按部位浏览
        item {
            SectionHeader(title = "按部位浏览")
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                bodyParts.forEach { entry ->
                    BodyPartChip(
                        name = entry.nameZh,
                        count = entry.count,
                        onClick = { onNavigate(Destinations.List.create("bodyPart", entry.keyEn)) }
                    )
                }
            }
        }

        // 最近浏览
        if (recents.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "最近浏览",
                    action = "全部",
                    onAction = { onNavigate(Destinations.Favorites.route) }
                )
            }
            items(recents.take(5), key = { it.exerciseId }) { recent ->
                val ex = repo.byId(recent.exerciseId) ?: return@items
                ExerciseCard(
                    exercise = ex,
                    onClick = { onNavigate(Destinations.Exercise.create(ex.id)) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        // 为你推荐
        item {
            SectionHeader(title = "为你推荐")
        }
        items(featured, key = { it.id }) { ex ->
            ExerciseCard(
                exercise = ex,
                onClick = { onNavigate(Destinations.Exercise.create(ex.id)) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

/** Hero 卡片：大尺寸渐变 + 标语 + 搜索按钮 */
@Composable
private fun HeroCard(total: Int, onSearch: () -> Unit) {
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
        // 装饰图标
        Icon(
            imageVector = Icons.Outlined.FitnessCenter,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.12f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 20.dp)
                .size(120.dp)
        )

        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "fitness",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "$total 个动作 · 全离线 · 中文指导",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.92f)
            )
            Spacer(Modifier.height(20.dp))
            // 搜索入口按钮
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onSearch),
                shape = ChipShape,
                color = Color.White.copy(alpha = 0.22f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "搜索动作、肌群、器械…",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.96f)
                    )
                    Spacer(Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickEntry(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = CardShape,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BodyPartChip(name: String, count: Int, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = ChipShape,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
