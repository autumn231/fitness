package com.fitness.app.data

import com.fitness.app.data.local.ProfileEntity
import java.time.LocalDate

/** 活动水平选项：标签 → 系数。 */
val ACTIVITY_OPTIONS: List<Pair<String, Double>> = listOf(
    "久坐（几乎不运动）" to 1.2,
    "轻度活动（每周 1–3 次）" to 1.375,
    "中度活动（每周 3–5 次）" to 1.55,
    "高强度（每周 6–7 次）" to 1.725
)

fun activityLabel(factor: Double): String =
    ACTIVITY_OPTIONS.minByOrNull { kotlin.math.abs(it.second - factor) }?.first ?: "轻度活动（每周 1–3 次）"

/**
 * Mifflin-St Jeor 公式估算 TDEE：
 * 男性 BMR = 10×体重 + 6.25×身高 − 5×年龄 + 5
 * 女性 BMR = 10×体重 + 6.25×身高 − 5×年龄 − 161
 * TDEE = BMR × 活动系数。
 * 档案不全（性别/出生年/身高/体重任一缺失）时返回 0，表示无法计算。
 */
fun computeTdee(profile: ProfileEntity?, weightKg: Double?): Int {
    if (profile == null || weightKg == null || weightKg <= 0) return 0
    if (profile.sex != "M" && profile.sex != "F") return 0
    if (profile.birthYear <= 0 || profile.heightCm <= 0) return 0
    val age = LocalDate.now().year - profile.birthYear
    if (age !in 10..100) return 0
    val bmr = 10 * weightKg + 6.25 * profile.heightCm - 5 * age +
        if (profile.sex == "M") 5 else -161
    if (bmr <= 0) return 0
    return (bmr * profile.activityFactor).toInt()
}

/** 档案是否已填齐到可算 TDEE 的程度（体重另行判断）。 */
fun profileComplete(profile: ProfileEntity?): Boolean =
    profile != null && (profile.sex == "M" || profile.sex == "F") &&
        profile.birthYear > 0 && profile.heightCm > 0

/**
 * 实际生效的每日预算：用户手动覆盖（设置里存的 tdee）优先，
 * 否则用档案 + 最新体重自动算；都拿不到时为 0（未设置）。
 */
fun effectiveTdee(override: Int, profile: ProfileEntity?, weightKg: Double?): Int =
    if (override > 0) override else computeTdee(profile, weightKg)

/** 一次训练的估算消耗：力量训练约 5 kcal/分钟（中等强度经验值，界面上标注「估算」）。 */
fun estimateBurnKcal(durationSec: Long): Int = (durationSec / 60.0 * 5.0).toInt()
