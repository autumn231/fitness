package com.fitness.app.ui.feature

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fitness.app.data.ExerciseRepository
import com.fitness.app.i18n.bodyPartZh
import com.fitness.app.i18n.equipmentZh
import com.fitness.app.i18n.muscleZh
import com.fitness.app.ui.common.EmptyState
import com.fitness.app.ui.common.ExerciseCard
import com.fitness.app.ui.nav.Destinations

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(
    repo: ExerciseRepository,
    type: String,
    key: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val title = when (type) {
        "bodyPart" -> bodyPartZh(key)
        "equipment" -> equipmentZh(key)
        "target" -> muscleZh(key)
        else -> key
    }
    val exercises = repo.all().filter { ex ->
        when (type) {
            "bodyPart" -> ex.body_part == key
            "equipment" -> ex.equipment == key
            "target" -> ex.target == key
            else -> false
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(title, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { inner ->
        if (exercises.isEmpty()) {
            EmptyState(
                title = "暂无动作",
                subtitle = "该分类下没有动作",
                modifier = Modifier.padding(inner)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = "共 ${exercises.size} 个动作",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                items(exercises, key = { it.id }) { ex ->
                    ExerciseCard(
                        exercise = ex,
                        onClick = { onNavigate(Destinations.Exercise.create(ex.id)) }
                    )
                }
            }
        }
    }
}
