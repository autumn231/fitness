package com.fitness.app.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val exerciseId: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "recents")
data class RecentEntity(
    @PrimaryKey val exerciseId: String,
    val viewedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "plans")
data class PlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "plan_items")
data class PlanItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long,
    val exerciseId: String,
    val sets: Int = 3,
    val reps: String = "12",
    val sortOrder: Int = 0
)

data class PlanWithItems(
    @Embedded val plan: PlanEntity,
    @Relation(parentColumn = "id", entityColumn = "planId")
    val items: List<PlanItemEntity>
)

/**
 * 日历安排：某天执行某个计划。
 * dateKey 格式为 yyyy-MM-dd，同一天只保留一个计划（主键约束）。
 */
@Entity(tableName = "calendar_plans")
data class CalendarPlanEntity(
    @PrimaryKey val dateKey: String,
    val planId: Long,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 食物摄入日志：用户在某天吃了多少克某食物。
 * dateKey 格式 yyyy-MM-dd；同一天可有多条记录。
 */
@Entity(tableName = "food_logs")
data class FoodLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateKey: String,
    val foodId: String,
    val foodName: String,
    val amountGram: Double,        // 实际摄入克数
    val energy: Double,            // 该份能量 kcal = food.energy * amountGram / 100
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val addedAt: Long = System.currentTimeMillis()
)
