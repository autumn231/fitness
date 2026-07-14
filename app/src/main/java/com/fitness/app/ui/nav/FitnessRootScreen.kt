package com.fitness.app.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fitness.app.data.ExerciseRepository
import com.fitness.app.ui.feature.AboutScreen
import com.fitness.app.ui.feature.CategoryScreen
import com.fitness.app.ui.feature.ExerciseDetailScreen
import com.fitness.app.ui.feature.FavoritesScreen
import com.fitness.app.ui.feature.HomeScreen
import com.fitness.app.ui.feature.ListScreen
import com.fitness.app.ui.feature.PickerScreen
import com.fitness.app.ui.feature.PlanDetailScreen
import com.fitness.app.ui.feature.PlanEditorScreen
import com.fitness.app.ui.feature.PlanScreen
import com.fitness.app.ui.feature.ProfileScreen
import com.fitness.app.ui.feature.SearchScreen
import com.fitness.app.ui.feature.SettingsScreen

/**
 * 应用根容器：启动时加载本地数据，加载完成后展示主导航（4 Tab + 子页路由）。
 */
@Composable
fun FitnessRootScreen(repo: ExerciseRepository) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    var loading by remember { mutableStateOf(!repo.isLoaded) }
    var loadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (!repo.isLoaded) {
            runCatching { repo.load() }
                .onFailure { loadError = it.message ?: "加载失败" }
                .also { loading = false }
        } else {
            loading = false
        }
    }

    if (loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    loadError?.let { err ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("数据加载失败：$err")
        }
        return
    }

    val showBottomBar = currentRoute in tabRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                androidx.compose.material3.NavigationBar {
                    tabs.forEach { tab ->
                        val selected = currentRoute == tab.dest.route
                        androidx.compose.material3.NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    nav.navigate(tab.dest.route) {
                                        popUpTo(Dest.Home.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                androidx.compose.material3.Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.icon,
                                    contentDescription = tab.label
                                )
                            },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { inner ->
        NavHost(
            navController = nav,
            startDestination = Dest.Home.route,
            modifier = Modifier.padding(inner)
        ) {
            composable(Dest.Home.route) {
                HomeScreen(repo) { route -> nav.navigate(route) }
            }
            composable(Dest.Category.route) {
                CategoryScreen(repo) { route -> nav.navigate(route) }
            }
            composable(Dest.Plan.route) {
                PlanScreen(repo) { route -> nav.navigate(route) }
            }
            composable(Dest.Profile.route) {
                ProfileScreen(repo) { route -> nav.navigate(route) }
            }
            composable(Dest.Search.route) {
                SearchScreen(
                    repo = repo,
                    onBack = { nav.popBackStack() },
                    onNavigate = { route -> nav.navigate(route) }
                )
            }
            composable(Dest.Favorites.route) {
                FavoritesScreen(repo) { route -> nav.navigate(route) }
            }
            composable(Dest.About.route) {
                AboutScreen { nav.popBackStack() }
            }
            composable(Dest.Settings.route) {
                SettingsScreen(repo) { nav.popBackStack() }
            }
            composable(
                Dest.Exercise.route,
                arguments = listOf(navArgument("exerciseId") { type = NavType.StringType })
            ) { entry ->
                val id = entry.arguments?.getString("exerciseId").orEmpty()
                ExerciseDetailScreen(repo, id) { nav.popBackStack() }
            }
            composable(
                Dest.List.route,
                arguments = listOf(
                    navArgument("type") { type = NavType.StringType },
                    navArgument("key") { type = NavType.StringType }
                )
            ) { entry ->
                val type = entry.arguments?.getString("type").orEmpty()
                val key = entry.arguments?.getString("key").orEmpty()
                ListScreen(
                    repo = repo,
                    type = type,
                    key = key,
                    onBack = { nav.popBackStack() },
                    onNavigate = { route -> nav.navigate(route) }
                )
            }
            composable(
                Dest.PlanEditor.route,
                arguments = listOf(navArgument("planId") { type = NavType.LongType })
            ) { entry ->
                val planId = entry.arguments?.getLong("planId") ?: 0L
                PlanEditorScreen(repo, planId,
                    onSaved = { nav.popBackStack() },
                    onBack = { nav.popBackStack() }
                )
            }
            composable(
                Dest.PlanDetail.route,
                arguments = listOf(navArgument("planId") { type = NavType.LongType })
            ) { entry ->
                val planId = entry.arguments?.getLong("planId") ?: 0L
                PlanDetailScreen(repo, planId,
                    onBack = { nav.popBackStack() },
                    onEdit = { nav.navigate(Dest.PlanEditor.create(planId)) },
                    onPick = { nav.navigate(Dest.Picker.create(planId)) },
                    onOpenExercise = { id -> nav.navigate(Dest.Exercise.create(id)) }
                )
            }
            composable(
                Dest.Picker.route,
                arguments = listOf(navArgument("planId") { type = NavType.LongType })
            ) { entry ->
                val planId = entry.arguments?.getLong("planId") ?: 0L
                PickerScreen(repo, planId,
                    onBack = { nav.popBackStack() },
                    onOpenExercise = { id -> nav.navigate(Dest.Exercise.create(id)) }
                )
            }
        }
    }
}
