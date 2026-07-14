package com.fitness.app.ui.feature

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fitness.app.data.ExerciseRepository
import com.fitness.app.i18n.CategoryEntry
import com.fitness.app.i18n.groupByBodyPart
import com.fitness.app.i18n.groupByEquipment
import com.fitness.app.i18n.groupByTarget
import com.fitness.app.ui.nav.Destinations

private enum class CategoryTab(val title: String) {
    BODYPART("按部位"),
    EQUIPMENT("按器械"),
    TARGET("按目标肌群")
}

@Composable
fun CategoryScreen(
    repo: ExerciseRepository,
    onNavigate: (String) -> Unit
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

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "动作分类",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
        )
        SecondaryTabRow(selectedTabIndex = tabIndex) {
            tabs.forEachIndexed { i, tab ->
                Tab(
                    selected = tabIndex == i,
                    onClick = { tabIndex = i },
                    text = { Text(tab.title) }
                )
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
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
                    .padding(start = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${entry.count}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
