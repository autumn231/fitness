package com.fitness.app.data

import java.time.LocalTime

/**
 * 餐次结构：记录的第一层组织（早餐/午餐/晚餐/加餐），
 * 今日页、营养页、添加页共用同一套常量与预算分配比例。
 */
object Meals {
    const val BREAKFAST = "早餐"
    const val LUNCH = "午餐"
    const val DINNER = "晚餐"
    const val SNACK = "加餐"

    val ALL: List<String> = listOf(BREAKFAST, LUNCH, DINNER, SNACK)

    /** 各餐占全天预算的比例（薄荷式每餐推荐区间的来源）。 */
    fun budgetRatio(meal: String): Double = when (meal) {
        BREAKFAST -> 0.25
        LUNCH -> 0.35
        DINNER -> 0.30
        else -> 0.10
    }

    /** 某餐的推荐热量区间（预算 × 比例，上下浮动约 15%）；预算未设置时返回 null。 */
    fun recommendedRange(meal: String, dailyBudget: Int): IntRange? {
        if (dailyBudget <= 0) return null
        val center = dailyBudget * budgetRatio(meal)
        val low = (center * 0.85).toInt()
        val high = (center * 1.15).toInt()
        return low..high
    }

    /** 按当前时间推断餐次（新建记录的默认值，用户可改）。 */
    fun inferNow(): String = infer(LocalTime.now().hour)

    fun infer(hour: Int): String = when (hour) {
        in 5..10 -> BREAKFAST
        in 11..14 -> LUNCH
        in 17..22 -> DINNER
        else -> SNACK
    }
}
