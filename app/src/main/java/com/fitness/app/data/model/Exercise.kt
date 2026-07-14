package com.fitness.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Exercise(
    val id: String,
    val name: String,
    val category: String = "",
    val body_part: String = "",
    val equipment: String = "",
    val instructions: Map<String, String> = emptyMap(),
    val instruction_steps: Map<String, List<String>> = emptyMap(),
    val muscle_group: String = "",
    val secondary_muscles: List<String> = emptyList(),
    val target: String = "",
    val media_id: String = "",
    val image: String = "",
    val gif_url: String = "",
    val attribution: String = "",
    val created_at: String = ""
) {
    val zhInstructions: String
        get() = instructions["zh"]?.takeIf { it.isNotBlank() } ?: instructions["en"] ?: ""

    val zhSteps: List<String>
        get() = instruction_steps["zh"]?.takeIf { it.isNotEmpty() }
            ?: instruction_steps["en"] ?: emptyList()
}
