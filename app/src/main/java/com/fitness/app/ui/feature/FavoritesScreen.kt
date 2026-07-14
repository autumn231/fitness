package com.fitness.app.ui.feature

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitness.app.data.ExerciseRepository
import com.fitness.app.ui.common.EmptyState
import com.fitness.app.ui.common.ExerciseCard
import com.fitness.app.ui.nav.Destinations

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    repo: ExerciseRepository,
    onNavigate: (String) -> Unit
) {
    val favoritesState = repo.observeFavorites().collectAsStateWithLifecycle(initialValue = emptyList())
    val recentsState = repo.observeRecents(100).collectAsStateWithLifecycle(initialValue = emptyList())
    val favorites = favoritesState.value
    val recents = recentsState.value
    var tab by remember { mutableIntStateOf(0) }

    val items = if (tab == 0) favorites else recents

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("收藏与历史", fontWeight = FontWeight.SemiBold) }
            )
        }
    ) { inner ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(inner)) {
            SecondaryTabRow(selectedTabIndex = tab) {
                Tab(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    text = { Text("收藏 (${favorites.size})") }
                )
                Tab(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    text = { Text("最近浏览 (${recents.size})") }
                )
            }

            if (items.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.Bookmark,
                    title = if (tab == 0) "还没有收藏" else "还没有浏览记录",
                    subtitle = if (tab == 0) "在动作详情页点击收藏按钮即可加入" else "浏览过的动作会出现在这里"
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items, key = { it.exerciseId }) { entity ->
                        val ex = repo.byId(entity.exerciseId) ?: return@items
                        ExerciseCard(
                            exercise = ex,
                            onClick = { onNavigate(Destinations.Exercise.create(ex.id)) }
                        )
                    }
                }
            }
        }
    }
}
