package com.fitness.app.data.model

import kotlinx.serialization.Serializable

/**
 * 食物营养成分（每 100g 可食部）。
 * 数据来源：《中国食物成分表》标准版第6版，由 china-food-composition-data 项目 OCR 整理。
 */
@Serializable
data class Food(
    val id: String,
    val name: String,
    val category: String = "",
    val subcategory: String = "",
    val edible: Double = 0.0,        // 可食部 %
    val energy: Double = 0.0,        // 能量 kcal
    val protein: Double = 0.0,       // 蛋白质 g
    val fat: Double = 0.0,           // 脂肪 g
    val carbs: Double = 0.0,         // 碳水化合物 g
    val fiber: Double = 0.0,         // 膳食纤维 g
    val cholesterol: Double = 0.0,   // 胆固醇 mg
    val calcium: Double = 0.0,       // 钙 mg
    val iron: Double = 0.0,          // 铁 mg
    val sodium: Double = 0.0,        // 钠 mg
    val potassium: Double = 0.0,     // 钾 mg
    val vitaminC: Double = 0.0,      // 维生素 C mg
    val remark: String = ""
)
