package com.fitness.app.ui.feature

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fitness.app.data.ExerciseRepository
import com.fitness.app.i18n.CategoryEntry
import com.fitness.app.i18n.groupByBodyPart
import com.fitness.app.i18n.groupByEquipment
import com.fitness.app.i18n.groupByTarget
import com.fitness.app.ui.nav.Destinations
import com.fitness.app.ui.theme.CardShape

private enum class CategoryTab(val title: String) {
    BODYPART("按部位"),
    EQUIPMENT("按器械"),
    TARGET("按目标肌群")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    repo: ExerciseRepository,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit = {}
) {
    val exercises = repo.all()
    var tabIndex by remember { mutableIntStateOf(0) }
    val tabs = CategoryTab.entries

    val groups: List<CategoryEntry> = when (tabs[tabIndex]) {
        CategoryTab.BODYPART -> remember(exercises.size, tabIndex) { groupByBodyPart(exercises) }
        CategoryTab.EQUIPMENT -> remember(exercises.size, tabIndex) { groupByEquipment(exercises) }
        CategoryTab.TARGET -> remember(exercises.size, tabIndex) { groupByTarget(exercises) }
    }

    val typeKey = when (tabs[tabIndex]) {
        CategoryTab.BODYPART -> "bodyPart"
        CategoryTab.EQUIPMENT -> "equipment"
        CategoryTab.TARGET -> "target"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("动作分类", fontWeight = FontWeight.SemiBold) },
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
                    text = "选择一个分类查看对应动作",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            item {
                SecondaryTabRow(selectedTabIndex = tabIndex) {
                    tabs.forEachIndexed { i, tab ->
                        Tab(
                            selected = tabIndex == i,
                            onClick = { tabIndex = i },
                            text = { Text(tab.title, fontWeight = if (tabIndex == i) FontWeight.SemiBold else FontWeight.Normal) }
                        )
                    }
                }
            }
            items(groups, key = { it.keyEn }) { entry ->
                CategoryRow(entry = entry) {
                    onNavigate(Destinations.List.create(typeKey, entry.keyEn))
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(entry: CategoryEntry, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = CardShape,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.nameZh,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = entry.keyEn,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = entry.count.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
