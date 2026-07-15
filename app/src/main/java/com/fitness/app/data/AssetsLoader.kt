package com.fitness.app.data

import android.content.Context
import com.fitness.app.data.model.Exercise
import com.fitness.app.data.model.Food
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
}
