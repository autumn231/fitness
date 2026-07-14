package com.fitness.app.ui.feature

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    repo: ExerciseRepository,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val exercises = repo.all()
    var query by remember { mutableStateOf("") }
    var selectedBodyParts by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedEquipments by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedTargets by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showFilter by remember { mutableStateOf(false) }

    val allBodyParts = remember(exercises.size) { exercises.map { it.body_part }.distinct().sorted() }
    val allEquipments = remember(exercises.size) { exercises.map { it.equipment }.distinct().sorted() }
    val allTargets = remember(exercises.size) { exercises.map { it.target }.distinct().sorted() }

    val results = remember(
        exercises.size, query, selectedBodyParts, selectedEquipments, selectedTargets
    ) {
        val q = query.trim().lowercase()
        exercises.filter { ex ->
            val matchQ = q.isBlank() || run {
                val name = ex.name.lowercase()
                val disp = "${ex.equipment} ${ex.target} ${ex.body_part} ${ex.muscle_group}".lowercase()
                val zh = "${equipmentZh(ex.equipment)} ${muscleZh(ex.target)} ${bodyPartZh(ex.body_part)}".lowercase()
                name.contains(q) || disp.contains(q) || zh.contains(q)
            }
            val matchBp = selectedBodyParts.isEmpty() || ex.body_part in selectedBodyParts
            val matchEq = selectedEquipments.isEmpty() || ex.equipment in selectedEquipments
            val matchTg = selectedTargets.isEmpty() || ex.target in selectedTargets
            matchQ && matchBp && matchEq && matchTg
        }
    }

    val filterCount = selectedBodyParts.size + selectedEquipments.size + selectedTargets.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("搜索", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showFilter = true }) {
                        if (filterCount > 0) {
                            BadgedBox(badge = { Badge { Text(filterCount.toString()) } }) {
                                Icon(Icons.Outlined.FilterList, contentDescription = "筛选")
                            }
                        } else {
                            Icon(Icons.Outlined.FilterList, contentDescription = "筛选")
                        }
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
                placeholder = { Text("搜索动作、肌群、器械…") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = "清除")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            if (results.isEmpty()) {
                EmptyState(title = "未找到相关动作", subtitle = "试试其他关键词或筛选条件")
            } else {
                Text(
                    text = "找到 ${results.size} 个动作",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(results, key = { it.id }) { ex ->
                        ExerciseCard(
                            exercise = ex,
                            onClick = { onNavigate(Destinations.Exercise.create(ex.id)) }
                        )
                    }
                }
            }
        }
    }

    if (showFilter) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showFilter = false },
            sheetState = sheetState
        ) {
            FilterContent(
                allBodyParts = allBodyParts,
                allEquipments = allEquipments,
                allTargets = allTargets,
                selectedBodyParts = selectedBodyParts,
                selectedEquipments = selectedEquipments,
                selectedTargets = selectedTargets,
                onBodyPartsChange = { selectedBodyParts = it },
                onEquipmentsChange = { selectedEquipments = it },
                onTargetsChange = { selectedTargets = it },
                onClear = {
                    selectedBodyParts = emptySet()
                    selectedEquipments = emptySet()
                    selectedTargets = emptySet()
                },
                onDone = { showFilter = false }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterContent(
    allBodyParts: List<String>,
    allEquipments: List<String>,
    allTargets: List<String>,
    selectedBodyParts: Set<String>,
    selectedEquipments: Set<String>,
    selectedTargets: Set<String>,
    onBodyPartsChange: (Set<String>) -> Unit,
    onEquipmentsChange: (Set<String>) -> Unit,
    onTargetsChange: (Set<String>) -> Unit,
    onClear: () -> Unit,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        FilterSection(
            title = "部位",
            options = allBodyParts,
            selected = selectedBodyParts,
            labelFor = { bodyPartZh(it) },
            onChange = onBodyPartsChange
        )
        FilterSection(
            title = "器械",
            options = allEquipments,
            selected = selectedEquipments,
            labelFor = { equipmentZh(it) },
            onChange = onEquipmentsChange
        )
        FilterSection(
            title = "目标肌群",
            options = allTargets,
            selected = selectedTargets,
            labelFor = { muscleZh(it) },
            onChange = onTargetsChange
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onClear) { Text("清空筛选") }
            TextButton(onClick = onDone) { Text("完成") }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterSection(
    title: String,
    options: List<String>,
    selected: Set<String>,
    labelFor: (String) -> String,
    onChange: (Set<String>) -> Unit
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        options.forEach { opt ->
            FilterChip(
                selected = opt in selected,
                onClick = {
                    onChange(if (opt in selected) selected - opt else selected + opt)
                },
                label = { Text(labelFor(opt)) }
            )
        }
    }
}
