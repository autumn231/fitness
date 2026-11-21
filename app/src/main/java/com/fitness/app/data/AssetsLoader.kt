package com.fitness.app.data

import android.content.Context
import com.fitness.app.data.model.Exercise
import com.fitness.app.data.model.Food
import com.fitness.app.data.model.PlanTemplate
import com.fitness.app.data.model.PlanTemplatesFile
import com.fitness.app.data.model.Recipe
import com.fitness.app.data.model.RecipesFile
import kotlinx.serialization.json.Json

/** 从 assets 一次性加载本地数据到内存（启动时调用）。 */
object AssetsLoader {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    fun loadExercises(context: Context): List<Exercise> {
        context.assets.open("exercises.json").use { input ->
            val text = input.bufferedReader(Charsets.UTF_8).readText()
            return json.decodeFromString<List<Exercise>>(text)
        }
    }

    /** 加载全部食物营养成分（约 1800 条，500KB）。 */
    fun loadFoods(context: Context): List<Food> {
        context.assets.open("foods.json").use { input ->
            val text = input.bufferedReader(Charsets.UTF_8).readText()
            return json.decodeFromString<List<Food>>(text)
        }
    }

    /** 加载内置计划模板（plan_templates.json）。 */
    fun loadPlanTemplates(context: Context): List<PlanTemplate> {
        context.assets.open("plan_templates.json").use { input ->
            val text = input.bufferedReader(Charsets.UTF_8).readText()
            return json.decodeFromString<PlanTemplatesFile>(text).templates
        }
    }

    /** 加载内置食谱（recipes.json）。 */
    fun loadRecipes(context: Context): List<Recipe> {
        context.assets.open("recipes.json").use { input ->
            val text = input.bufferedReader(Charsets.UTF_8).readText()
            return json.decodeFromString<RecipesFile>(text).recipes
        }
    }
}
