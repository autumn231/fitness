package com.fitness.app.ui.feature

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fitness.app.data.ExerciseRepository
import com.fitness.app.data.model.PlanTemplate
import com.fitness.app.i18n.displayName
import com.fitness.app.ui.common.EmptyState
import androidx.compose.material.icons.outlined.FitnessCenter
import com.fitness.app.ui.nav.Destinations
import com.fitness.app.ui.theme.CardShape
import kotlinx.coroutines.launch

/** 计划模板列表：内置分化方案，点进详情可一键套用到「我的计划」。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanTemplateListScreen(
    repo: ExerciseRepository,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    val templates = repo.planTemplates()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("计划模板", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = "选一套现成分化方案，一键生成到「我的计划」，之后可自由改名、改动作、排日历。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(templates, key = { it.id }) { t ->
                TemplateCard(t) { onNavigate(Destinations.PlanTemplateDetail.create(t.id)) }
            }
        }
    }
}

@Composable
private fun TemplateCard(template: PlanTemplate, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = CardShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = template.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${template.level} · ${template.daysPerWeek}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
            if (template.desc.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = template.desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(10.dp))
            val actionCount = template.days.sumOf { it.items.size }
            Text(
                text = "共 ${template.days.size} 个训练日 · $actionCount 个动作",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** 计划模板详情：按训练日预览全部动作，底部一键套用。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanTemplateDetailScreen(
    repo: ExerciseRepository,
    templateId: String,
    onBack: () -> Unit,
    onOpenExercise: (String) -> Unit,
    onApplied: () -> Unit
) {
    val template = repo.planTemplateById(templateId)
    val scope = rememberCoroutineScope()
    var applying by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(template?.title ?: "计划模板", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        bottomBar = {
            if (template != null) {
                Surface(shadowElevation = 8.dp) {
                    Box(Modifier.navigationBarsPadding()) {
                    Button(
                        onClick = {
                            if (!applying) {
                                applying = true
                                scope.launch {
                                    repo.applyPlanTemplate(template)
                                    onApplied()
                                }
                            }
                        },
                        enabled = !applying,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(if (applying) "正在加入…" else "全部加入我的计划（${template.days.size} 个训练日）")
                    }
                    }
                }
            }
        }
    ) { inner ->
        if (template == null) {
            EmptyState(
                icon = Icons.Outlined.FitnessCenter,
                title = "模板不存在",
                subtitle = "该模板可能已被移除",
                modifier = Modifier.padding(inner)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    Column {
                        Text(
                            text = "${template.level} · ${template.daysPerWeek}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                        if (template.desc.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = template.desc,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
                template.days.forEach { day ->
                    item(key = "day-${day.name}") {
                        Text(
                            text = day.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                        )
                    }
                    items(day.items, key = { "${day.name}-${it.exerciseId}" }) { ti ->
                        val exercise = repo.byId(ti.exerciseId)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenExercise(ti.exerciseId) },
                            shape = CardShape,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = exercise?.displayName() ?: ti.exerciseId,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "${ti.sets} 组 × ${ti.reps}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}
