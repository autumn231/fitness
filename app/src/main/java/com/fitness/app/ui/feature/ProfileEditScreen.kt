package com.fitness.app.ui.feature

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitness.app.data.ACTIVITY_OPTIONS
import com.fitness.app.data.ExerciseRepository
import com.fitness.app.data.computeTdee
import com.fitness.app.data.local.ProfileEntity
import kotlinx.coroutines.launch

/**
 * 个人档案编辑：性别 / 出生年份 / 身高 / 活动水平。
 * 填齐 + 有体重记录后，每日热量目标由 Mifflin-St Jeor 公式自动算（见 Tdee.kt）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(
    repo: ExerciseRepository,
    onBack: () -> Unit
) {
    val profile by repo.observeProfile().collectAsStateWithLifecycle(initialValue = null)
    val latestWeight by repo.observeLatestWeight().collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    // 档案到达后初始化表单（只初始化一次）
    var initialized by remember { mutableStateOf(false) }
    var sex by remember { mutableStateOf("") }
    var birthYearText by remember { mutableStateOf("") }
    var heightText by remember { mutableStateOf("") }
    var activityFactor by remember { mutableStateOf(1.375) }
    if (!initialized && profile != null) {
        sex = profile!!.sex
        birthYearText = profile!!.birthYear.takeIf { it > 0 }?.toString() ?: ""
        heightText = profile!!.heightCm.takeIf { it > 0 }?.let {
            if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()
        } ?: ""
        activityFactor = profile!!.activityFactor
        initialized = true
    }

    val draft = ProfileEntity(
        sex = sex,
        birthYear = birthYearText.toIntOrNull() ?: 0,
        heightCm = heightText.toDoubleOrNull() ?: 0.0,
        activityFactor = activityFactor
    )
    val previewTdee = latestWeight?.let { computeTdee(draft, it.weightKg) } ?: 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("个人档案", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(Modifier.height(2.dp))
            Text(
                "填齐档案并记录体重后，每日热量目标会自动按公式算出，不用再手动估。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text("性别", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = sex == "M",
                    onClick = { sex = "M" },
                    label = { Text("男") }
                )
                FilterChip(
                    selected = sex == "F",
                    onClick = { sex = "F" },
                    label = { Text("女") }
                )
            }

            OutlinedTextField(
                value = birthYearText,
                onValueChange = { birthYearText = it.filter { c -> c.isDigit() }.take(4) },
                label = { Text("出生年份（如 2002）") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = heightText,
                onValueChange = { heightText = it.filter { c -> c.isDigit() }.take(3) },
                label = { Text("身高（cm）") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text("日常活动水平", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Column {
                ACTIVITY_OPTIONS.forEach { (label, factor) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = activityFactor == factor,
                                onClick = { activityFactor = factor }
                            )
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = activityFactor == factor,
                            onClick = { activityFactor = factor }
                        )
                        Text(
                            label,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("每日热量目标预览", style = MaterialTheme.typography.labelMedium)
                    when {
                        latestWeight == null -> Text(
                            "先记一条体重，才能算出目标。保存档案后，去「我的 → 体重进度」点 + 记一条就行",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        previewTdee > 0 -> Text(
                            "约 $previewTdee 千卡/天",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        else -> Text(
                            "档案还没填齐（性别、出生年份、身高都要有）",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Button(
                onClick = {
                    scope.launch {
                        repo.saveProfile(draft)
                        snackbar.showSnackbar(
                            if (latestWeight == null) "档案已保存，去记一条体重就能算出每日目标"
                            else "档案已保存"
                        )
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("保存")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
