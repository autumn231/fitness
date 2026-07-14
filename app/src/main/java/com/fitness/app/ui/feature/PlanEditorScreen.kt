package com.fitness.app.ui.feature

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitness.app.data.ExerciseRepository
import com.fitness.app.data.local.PlanEntity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanEditorScreen(
    repo: ExerciseRepository,
    planId: Long,
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    val isNew = planId == 0L
    val scope = rememberCoroutineScope()

    var existing by remember { mutableStateOf<PlanEntity?>(null) }
    var name by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var loadedExisting by remember { mutableStateOf(false) }

    if (!isNew) {
        val planState = repo.observePlan(planId).collectAsStateWithLifecycle(initialValue = null)
        LaunchedEffect(planState.value) {
            val pw = planState.value
            if (pw != null && !loadedExisting) {
                existing = pw.plan
                name = pw.plan.name
                note = pw.plan.note
                loadedExisting = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "新建计划" else "编辑计划", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (name.isBlank()) return@IconButton
                        scope.launch {
                            if (isNew) {
                                repo.createPlan(name.trim(), note.trim())
                            } else {
                                existing?.let {
                                    repo.updatePlan(it.copy(name = name.trim(), note = note.trim()))
                                }
                            }
                            onSaved()
                        }
                    }) {
                        Icon(Icons.Filled.Check, contentDescription = "保存")
                    }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("计划名称") },
                placeholder = { Text("例如：胸部日 / 推日 / 全身循环") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("备注（可选）") },
                placeholder = { Text("写一些训练备注，比如组间休息、目标部位等") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            )

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    if (name.isBlank()) return@Button
                    scope.launch {
                        if (isNew) {
                            repo.createPlan(name.trim(), note.trim())
                        } else {
                            existing?.let {
                                repo.updatePlan(it.copy(name = name.trim(), note = note.trim()))
                            }
                        }
                        onSaved()
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isNew) "创建计划" else "保存修改")
            }
            if (!isNew && existing != null) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            repo.deletePlan(planId)
                            onSaved()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("删除计划", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
