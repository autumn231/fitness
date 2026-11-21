package com.fitness.app.data.model

import kotlinx.serialization.Serializable

/** 计划模板文件（assets/plan_templates.json）：整套分化方案，一键套用到「我的计划」。 */
@Serializable
data class PlanTemplatesFile(
    val templates: List<PlanTemplate> = emptyList()
)

@Serializable
data class PlanTemplate(
    val id: String,
    val title: String,
    val desc: String = "",
    val level: String = "",
    val daysPerWeek: String = "",
    val days: List<TemplateDay> = emptyList()
)

/** 模板中的一个训练日；套用时按天各生成一个计划。 */
@Serializable
data class TemplateDay(
    val name: String,
    val items: List<TemplateItem> = emptyList()
)

@Serializable
data class TemplateItem(
    val exerciseId: String,
    val sets: Int = 3,
    val reps: String = "12"
)

/** 食谱文件（assets/recipes.json）：食材只存食物 id 与克数，营养由食物库实时汇总。 */
@Serializable
data class RecipesFile(
    val recipes: List<Recipe> = emptyList()
)

@Serializable
data class Recipe(
    val id: String,
    val title: String,
    val category: String = "",          // 增肌 / 减脂 / 家常
    val minutes: Int = 0,
    val tags: List<String> = emptyList(),
    val ingredients: List<RecipeIngredient> = emptyList(),
    val steps: List<String> = emptyList(),
    /** true = 用户自建（存数据库，id 形如 user_12），可编辑/删除；内置食谱为 false。 */
    val editable: Boolean = false
)

@Serializable
data class RecipeIngredient(
    val foodId: String,
    val gram: Double,
    val note: String = ""
)

/** 一份食谱（按倍数缩放后）的营养汇总。 */
data class NutritionTotals(
    val energy: Double = 0.0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0
)
