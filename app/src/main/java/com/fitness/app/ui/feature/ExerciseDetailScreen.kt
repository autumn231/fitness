package com.fitness.app.ui.feature

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitness.app.data.ExerciseRepository
import com.fitness.app.i18n.bodyPartZh
import com.fitness.app.i18n.displayName
import com.fitness.app.i18n.equipmentZh
import com.fitness.app.i18n.muscleZh
import com.fitness.app.i18n.subtitle
import com.fitness.app.ui.common.InfoRow
import com.fitness.app.ui.common.LocalAssetImage
import com.fitness.app.ui.common.ModernChip
import com.fitness.app.ui.common.ChipStyle
import com.fitness.app.ui.theme.CardShape
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExerciseDetailScreen(
    repo: ExerciseRepository,
    exerciseId: String,
    onBack: () -> Unit
) {
    val exercise = repo.byId(exerciseId)
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val isFavState = repo.observeIsFavorite(exerciseId)
        .collectAsStateWithLifecycle(initialValue = false)
    val isFav = isFavState.value

    LaunchedEffect(exerciseId) {
        exercise?.let { repo.recordView(it.id) }
    }

    if (exercise == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("动作不存在")
        }
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回",
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                },
                actions = {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                    ) {
                        IconButton(onClick = {
                            scope.launch { repo.toggleFavorite(exerciseId) }
                        }) {
                            Icon(
                                imageVector = if (isFav) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = if (isFav) "取消收藏" else "收藏",
                                tint = if (isFav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { inner ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // 大尺寸 GIF 动图（视差效果）
            item {
                val scrollOffset = listState.firstVisibleItemScrollOffset.toFloat()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    LocalAssetImage(
                        path = exercise.gif_url,
                        contentDescription = exercise.displayName(),
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { translationY = scrollOffset * 0.5f },
                        contentScale = ContentScale.Fit
                    )
                    // 底部渐变蒙版
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.background
                                    )
                                )
                            )
                    )
                }
            }

            // 标题区
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                    Text(
                        text = exercise.displayName(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = exercise.subtitle(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ModernChip(text = bodyPartZh(exercise.body_part), style = ChipStyle.PRIMARY)
                        ModernChip(text = equipmentZh(exercise.equipment), style = ChipStyle.ACCENT)
                        ModernChip(text = muscleZh(exercise.target), style = ChipStyle.NEUTRAL)
                    }
                }
            }

            // 基本信息卡
            item {
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
                        InfoRow(label = "部位", value = bodyPartZh(exercise.body_part))
                        InfoRow(label = "器械", value = equipmentZh(exercise.equipment))
                        InfoRow(label = "目标肌群", value = muscleZh(exercise.target))
                        InfoRow(label = "主肌群", value = muscleZh(exercise.muscle_group))
                        if (exercise.secondary_muscles.isNotEmpty()) {
                            InfoRow(
                                label = "辅助肌群",
                                value = exercise.secondary_muscles.joinToString("、") { muscleZh(it) }
                            )
                        }
                    }
                }
            }

            // 步骤指引
            item {
                SectionTitle(text = "动作步骤", modifier = Modifier.padding(horizontal = 20.dp))
            }
            if (exercise.zhSteps.isNotEmpty()) {
                itemsIndexed(exercise.zhSteps) { idx, step ->
                    StepCard(
                        index = idx + 1,
                        text = step,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            } else if (exercise.zhInstructions.isNotBlank()) {
                item {
                    Text(
                        text = exercise.zhInstructions,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    )
                }
            } else {
                item {
                    Text(
                        text = "暂无步骤说明",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
            }

            // 完整文本指导
            if (exercise.zhInstructions.isNotBlank() && exercise.zhSteps.isNotEmpty()) {
                item {
                    SectionTitle(text = "完整指导", modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = CardShape,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = exercise.zhInstructions,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            // 数据来源
            item {
                Text(
                    text = "数据来源：exercises-dataset（hasaneyldrm）",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(vertical = 8.dp)
    )
}

/** 现代步骤卡片：大编号圆 + 步骤正文 */
@Composable
private fun StepCard(index: Int, text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = index.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(14.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 4.dp)
            )
        }
    }
}
