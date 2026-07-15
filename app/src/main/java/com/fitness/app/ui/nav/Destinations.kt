package com.fitness.app.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalDining
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Destinations(val route: String) {
    object Home : Destinations("home")
    object Nutrition : Destinations("nutrition")
    object Training : Destinations("training")
    object Calendar : Destinations("calendar")
    object Profile : Destinations("profile")
    object Category : Destinations("category")   // 已从 Tab 移除，作为子页保留（主页快捷入口使用）
    object Search : Destinations("search")
    object Favorites : Destinations("favorites")
    object Settings : Destinations("settings")
    object About : Destinations("about")
    object Plans : Destinations("plans")

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

    object FoodDetail : Destinations("food/{foodId}") {
        fun create(id: String) = "food/$id"
    }

    object FoodList : Destinations("foodList/{category}") {
        fun create(category: String) = "foodList/$category"
    }
}

data class TabItem(
    val dest: Destinations,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
)

// 5 Tab：首页 / 营养 / 训练(正中间) / 日历 / 我的
val tabs: List<TabItem> = listOf(
    TabItem(Destinations.Home, "首页", Icons.Outlined.Home, Icons.Filled.Home),
    TabItem(Destinations.Nutrition, "营养", Icons.Outlined.LocalDining, Icons.Filled.LocalDining),
    TabItem(Destinations.Training, "训练", Icons.Outlined.PlayCircle, Icons.Filled.PlayCircle),
    TabItem(Destinations.Calendar, "日历", Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth),
    TabItem(Destinations.Profile, "我的", Icons.Outlined.Person, Icons.Filled.Person)
)

val tabRoutes: Set<String> = tabs.map { it.dest.route }.toSet()
