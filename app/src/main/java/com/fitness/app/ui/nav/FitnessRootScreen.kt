package com.fitness.app.ui.nav

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
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

    // 启动加载动画
    AnimatedVisibility(
        visible = loading,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(200))
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return@AnimatedVisibility
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
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(tween(220)) { it } + fadeIn(tween(220)),
                exit = slideOutVertically(tween(220)) { it } + fadeOut(tween(220))
            ) {
                ModernNavigationBar(
                    tabs = tabs,
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        nav.navigate(route) {
                            popUpTo(Destinations.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { inner ->
        NavHost(
            navController = nav,
            startDestination = Destinations.Home.route,
            modifier = Modifier.padding(inner),
            enterTransition = { slideInHorizontally() },
            exitTransition = { slideOutHorizontally() },
            popEnterTransition = { popSlideInHorizontally() },
            popExitTransition = { popSlideOutHorizontally() }
        ) {
            // Tab 主页 — 使用淡入淡出避免与底部栏切换冲突
            composable(
                Destinations.Home.route,
                enterTransition = { tabFadeIn() },
                exitTransition = { tabFadeOut() },
                popEnterTransition = { tabFadeIn() },
                popExitTransition = { tabFadeOut() }
            ) {
                HomeScreen(repo) { route -> nav.navigate(route) }
            }
            composable(
                Destinations.Category.route,
                enterTransition = { tabFadeIn() },
                exitTransition = { tabFadeOut() },
                popEnterTransition = { tabFadeIn() },
                popExitTransition = { tabFadeOut() }
            ) {
                CategoryScreen(repo) { route -> nav.navigate(route) }
            }
            composable(
                Destinations.Plan.route,
                enterTransition = { tabFadeIn() },
                exitTransition = { tabFadeOut() },
                popEnterTransition = { tabFadeIn() },
                popExitTransition = { tabFadeOut() }
            ) {
                PlanScreen(repo) { route -> nav.navigate(route) }
            }
            composable(
                Destinations.Profile.route,
                enterTransition = { tabFadeIn() },
                exitTransition = { tabFadeOut() },
                popEnterTransition = { tabFadeIn() },
                popExitTransition = { tabFadeOut() }
            ) {
                ProfileScreen(repo) { route -> nav.navigate(route) }
            }

            // 子页 — 滑动动画
            composable(Destinations.Search.route) {
                SearchScreen(
                    repo = repo,
                    onBack = { nav.popBackStack() },
                    onNavigate = { route -> nav.navigate(route) }
                )
            }
            composable(Destinations.Favorites.route) {
                FavoritesScreen(repo) { route -> nav.navigate(route) }
            }
            composable(Destinations.About.route) {
                AboutScreen { nav.popBackStack() }
            }
            composable(Destinations.Settings.route) {
                SettingsScreen(repo) { nav.popBackStack() }
            }
            composable(
                Destinations.Exercise.route,
                arguments = listOf(navArgument("exerciseId") { type = NavType.StringType })
            ) { entry ->
                val id = entry.arguments?.getString("exerciseId").orEmpty()
                ExerciseDetailScreen(repo, id) { nav.popBackStack() }
            }
            composable(
                Destinations.List.route,
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
                Destinations.PlanEditor.route,
                arguments = listOf(navArgument("planId") { type = NavType.LongType })
            ) { entry ->
                val planId = entry.arguments?.getLong("planId") ?: 0L
                PlanEditorScreen(
                    repo, planId,
                    onSaved = { nav.popBackStack() },
                    onBack = { nav.popBackStack() }
                )
            }
            composable(
                Destinations.PlanDetail.route,
                arguments = listOf(navArgument("planId") { type = NavType.LongType })
            ) { entry ->
                val planId = entry.arguments?.getLong("planId") ?: 0L
                PlanDetailScreen(
                    repo, planId,
                    onBack = { nav.popBackStack() },
                    onEdit = { nav.navigate(Destinations.PlanEditor.create(planId)) },
                    onPick = { nav.navigate(Destinations.Picker.create(planId)) },
                    onOpenExercise = { id -> nav.navigate(Destinations.Exercise.create(id)) }
                )
            }
            composable(
                Destinations.Picker.route,
                arguments = listOf(navArgument("planId") { type = NavType.LongType })
            ) { entry ->
                val planId = entry.arguments?.getLong("planId") ?: 0L
                PickerScreen(
                    repo, planId,
                    onBack = { nav.popBackStack() },
                    onOpenExercise = { id -> nav.navigate(Destinations.Exercise.create(id)) }
                )
            }
        }
    }
}

@Composable
private fun ModernNavigationBar(
    tabs: List<TabItem>,
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    NavigationBar {
        tabs.forEach { tab ->
            val selected = currentRoute == tab.dest.route
            NavigationBarItem(
                selected = selected,
                onClick = { if (!selected) onNavigate(tab.dest.route) },
                icon = {
                    // 选中时图标轻微放大动画
                    Icon(
                        imageVector = if (selected) tab.selectedIcon else tab.icon,
                        contentDescription = tab.label,
                        modifier = Modifier.scale(if (selected) 1.1f else 1f)
                    )
                },
                label = { Text(tab.label) },
                alwaysShowLabel = true
            )
        }
    }
}
