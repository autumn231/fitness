package com.fitness.app.data

import android.content.Context
import com.fitness.app.data.model.Exercise
import kotlinx.serialization.json.Json

/** 从 assets/exercises.json 一次性加载全部动作到内存（启动时调用）。 */
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
}
