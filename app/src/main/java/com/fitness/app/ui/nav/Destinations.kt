package com.fitness.app.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Destinations(val route: String) {
    object Home : Destinations("home")
    object Category : Destinations("category")
    object Plan : Destinations("plan")
    object Profile : Destinations("profile")
    object Search : Destinations("search")
    object Favorites : Destinations("favorites")
    object Settings : Destinations("settings")
    object About : Destinations("about")

    object Exercise : Destinations("exercise/{exerciseId}") {
        fun create(id: String) = "exercise/$id"
    }

    object List : Destinations("list/{type}/{key}") {
        fun create(type: String, key: String) = "list/$type/$key"
    }

    object PlanEditor : Destinations("planEditor/{planId}") {
        fun create(id: Long) = "planEditor/$id"
    }

    object PlanDetail : Destinations("planDetail/{planId}") {
        fun create(id: Long) = "planDetail/$id"
    }

    object Picker : Destinations("picker/{planId}") {
        fun create(planId: Long) = "picker/$planId"
    }
}

data class TabItem(
    val dest: Destinations,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
)

val tabs: List<TabItem> = listOf(
    TabItem(Destinations.Home, "首页", Icons.Outlined.Home, Icons.Filled.Home),
    TabItem(Destinations.Category, "分类", Icons.Outlined.Category, Icons.Filled.Category),
    TabItem(Destinations.Plan, "训练", Icons.Outlined.FitnessCenter, Icons.Filled.FitnessCenter),
    TabItem(Destinations.Profile, "我的", Icons.Outlined.Person, Icons.Filled.Person)
)

val tabRoutes: Set<String> = tabs.map { it.dest.route }.toSet()
