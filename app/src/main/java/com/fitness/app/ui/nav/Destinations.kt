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

sealed class Dest(val route: String) {
    object Home : Dest("home")
    object Category : Dest("category")
    object Plan : Dest("plan")
    object Profile : Dest("profile")
    object Search : Dest("search")
    object Favorites : Dest("favorites")
    object Settings : Dest("settings")
    object About : Dest("about")

    object Exercise : Dest("exercise/{exerciseId}") {
        fun create(id: String) = "exercise/$id"
    }

    object List : Dest("list/{type}/{key}") {
        fun create(type: String, key: String) = "list/$type/$key"
    }

    object PlanEditor : Dest("planEditor/{planId}") {
        fun create(id: Long) = "planEditor/$id"
    }

    object PlanDetail : Dest("planDetail/{planId}") {
        fun create(id: Long) = "planDetail/$id"
    }

    object Picker : Dest("picker/{planId}") {
        fun create(planId: Long) = "picker/$planId"
    }
}

data class TabItem(
    val dest: Dest,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
)

val tabs: List<TabItem> = listOf(
    TabItem(Dest.Home, "首页", Icons.Outlined.Home, Icons.Filled.Home),
    TabItem(Dest.Category, "分类", Icons.Outlined.Category, Icons.Filled.Category),
    TabItem(Dest.Plan, "训练", Icons.Outlined.FitnessCenter, Icons.Filled.FitnessCenter),
    TabItem(Dest.Profile, "我的", Icons.Outlined.Person, Icons.Filled.Person)
)

val tabRoutes: Set<String> = tabs.map { it.dest.route }.toSet()
