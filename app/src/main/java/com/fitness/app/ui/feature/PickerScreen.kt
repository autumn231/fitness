package com.fitness.app.ui.feature

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitness.app.data.ExerciseRepository
import com.fitness.app.data.local.PlanItemEntity
import com.fitness.app.i18n.bodyPartZh
import com.fitness.app.i18n.displayName
import com.fitness.app.i18n.equipmentZh
import com.fitness.app.ui.common.LocalAssetImage
import com.fitness.app.ui.theme.CardShape
import com.fitness.app.ui.theme.ImageShape
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickerScreen(
    repo: ExerciseRepository,
    planId: Long,
    onBack: () -> Unit,
    onOpenExercise: (String) -> Unit
) {
    val exercises = repo.all()
    var query by remember { mutableStateOf("") }
    // 仅看收藏
    var onlyFavorites by remember { mutableStateOf(false) }
    val planState = repo.observePlan(planId).collectAsStateWithLifecycle(initialValue = null)
    val favoritesState = repo.observeFavorites().collectAsStateWithLifecycle(initialValue = emptyList())
    val existingIds = remember(planState.value) {
        planState.value?.items?.map { it.exerciseId }?.toSet() ?: emptySet()
    }
    val favoriteIds = remember(favoritesState.value) {
        favoritesState.value.map { it.exerciseId }.toSet()
    }
    val scope = rememberCoroutineScope()

    val results = remember(exercises.size, query, onlyFavorites, favoriteIds) {
        val q = query.trim().lowercase()
        var list = if (onlyFavorites) exercises.filter { it.id in favoriteIds } else exercises
        if (q.isNotBlank()) {
            list = list.filter { ex ->
                ex.name.lowercase().contains(q) ||
                        "${ex.equipment} ${ex.target} ${ex.body_part}".lowercase().contains(q) ||
                        "${equipmentZh(ex.equipment)} ${bodyPartZh(ex.body_part)}".contains(q)
            }
        }
        list
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("添加动作", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { inner ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(inner)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("搜索动作…") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )
            // 仅看收藏 筛选 Chip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = onlyFavorites,
                    onClick = { onlyFavorites = !onlyFavorites },
                    label = { Text("仅看收藏") },
                    leadingIcon = {
                        if (onlyFavorites) {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                )
                Spacer(Modifier.width(8.dp))
                if (onlyFavorites) {
                    Text(
                        text = "共 ${favoriteIds.size} 个收藏",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (results.isEmpty()) {
                val msg = if (onlyFavorites) "收藏夹为空" else "未找到相关动作"
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(results, key = { it.id }) { ex ->
                        val alreadyAdded = ex.id in existingIds
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = CardShape,
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (alreadyAdded) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(ImageShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    LocalAssetImage(
                                        path = ex.image,
                                        contentDescription = ex.displayName(),
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = ex.displayName(),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = ex.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                                if (alreadyAdded) {
                                    Text(
                                        text = "已添加",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                } else {
                                    IconButton(onClick = {
                                        scope.launch {
                                            val nextOrder = (planState.value?.items?.maxOfOrNull { it.sortOrder } ?: 0) + 1
                                            repo.addItem(
                                                PlanItemEntity(
                                                    planId = planId,
                                                    exerciseId = ex.id,
                                                    sortOrder = nextOrder
                                                )
                                            )
                                        }
                                    }) {
                                        Icon(
                                            Icons.Filled.Add,
                                            contentDescription = "加入计划",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                // 已添加/未添加 均可查看详情
                                IconButton(onClick = { onOpenExercise(ex.id) }) {
                                    Icon(Icons.Outlined.Info, contentDescription = "查看详情")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
