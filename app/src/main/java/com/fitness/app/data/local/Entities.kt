package com.fitness.app.data.local

import androidx.room.ColumnInfo
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
    // 餐次（早餐/午餐/晚餐/加餐）：v4 起记录的第一层结构，不再是事后标签。
    // 迁移时旧记录统一填「加餐」，新记录由 UI 按时间段推断、可改。
    @ColumnInfo(defaultValue = "加餐")
    val meal: String = "加餐",
    val addedAt: Long = System.currentTimeMillis()
)

/** 常用餐模板：把一餐存为命名餐，下次整餐一键带入（MyFitnessPal 的 Save Meal）。 */
@Entity(tableName = "meal_templates")
data class MealTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

/** 常用餐的条目快照（字段与食物日志对齐，应用模板时复制为当天的日志行）。 */
@Entity(tableName = "meal_template_items")
data class MealTemplateItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val templateId: Long,
    val foodId: String,
    val foodName: String,
    val amountGram: Double,
    val energy: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double
)

data class MealTemplateWithItems(
    @Embedded val template: MealTemplateEntity,
    @Relation(parentColumn = "id", entityColumn = "templateId")
    val items: List<MealTemplateItemEntity>
)

/**
 * 一次训练会话：点「开始训练」即建行，练完写 endedAt/durationSec。
 * 训练从此有历史，日历完成标记、Previous 内联、总结页都读这张表。
 */
@Entity(tableName = "workout_sessions")
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long? = null,          // 来源计划（自由训练为 null）
    val planName: String = "",         // 计划名快照（计划改名/删除后历史仍可读）
    val dateKey: String,
    val startedAt: Long,
    val endedAt: Long? = null,         // null = 进行中
    val durationSec: Long = 0
)

/** 训练组记录：会话内每个动作的每一组（重量/次数/是否完成）。 */
@Entity(tableName = "workout_sets")
data class WorkoutSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: String,
    val exerciseName: String,          // 动作名快照
    val setIndex: Int,                 // 组序号（同一动作内从 0 开始）
    val weightKg: Double = 0.0,
    val reps: Int = 0,
    // 计时类动作（如平板支撑）的目标秒数；>0 时本组按秒计时、reps 不用（v6 起）
    @ColumnInfo(defaultValue = "0")
    val durationSec: Int = 0,
    val done: Boolean = false,
    val sortOrder: Int = 0             // 动作在会话内的出现顺序
)

/** 体重记录：一天一条（dateKey 主键），可改。 */
@Entity(tableName = "weight_logs")
data class WeightLogEntity(
    @PrimaryKey val dateKey: String,
    val weightKg: Double,
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 个人档案（单行表，id 固定为 1）：TDEE 由身高/体重/年龄/活动水平自动算，
 * 不再让用户手填一个裸数字。字段为 0/空表示未填写。
 */
@Entity(tableName = "user_profile")
data class ProfileEntity(
    @PrimaryKey val id: Int = 1,
    val sex: String = "",              // "M" / "F" / ""
    val birthYear: Int = 0,
    val heightCm: Double = 0.0,
    val activityFactor: Double = 1.375 // 活动系数（久坐 1.2 / 轻度 1.375 / 中度 1.55 / 高强度 1.725）
)

/** 用户自建食谱：食材只存食物 id 与克数，营养口径与内置食谱一致（由食物库实时汇总）。 */
@Entity(tableName = "user_recipes")
data class UserRecipeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String = "",
    val minutes: Int = 0,
    /** 标签，逗号分隔（如「下饭,快手」）。 */
    val tags: String = "",
    /** 做法步骤，换行分隔，一行一步。 */
    val steps: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_recipe_items")
data class UserRecipeItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipeId: Long,
    val foodId: String,
    val gram: Double,
    val note: String = "",
    val sortOrder: Int = 0
)

data class UserRecipeWithItems(
    @Embedded val recipe: UserRecipeEntity,
    @Relation(parentColumn = "id", entityColumn = "recipeId")
    val items: List<UserRecipeItemEntity>
)
