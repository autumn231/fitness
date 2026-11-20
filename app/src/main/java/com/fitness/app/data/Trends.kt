package com.fitness.app.data

import com.fitness.app.data.local.FoodLogEntity
import com.fitness.app.data.local.WeightLogEntity
import com.fitness.app.data.local.WorkoutSessionEntity
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/**
 * 趋势结论（蓝图 v2 第 4 期）：不给裸数据，给方向结论。
 * 口径统一为「近 90 天 vs 长期均值」：
 * - 近期窗口 = 今天往前 90 天（含今天）；
 * - 长期均值优先取窗口之前的全部数据，不足时退回全部数据均值。
 * 数据不足时 enoughData=false，界面显示空状态而不是硬凑结论。
 */
data class TrendConclusion(
    /** 指标键：weight / training / intake。 */
    val key: String,
    /** 人话结论，如「体重下降中」。 */
    val headline: String,
    /** 数字依据，如「近90天周均 -0.3 kg · 近期均值 71.2 kg（长期 73.5 kg）」。 */
    val detail: String,
    val enoughData: Boolean
)

object Trends {

    const val WINDOW_DAYS = 90L

    private fun fmt1(v: Double): String = "%.1f".format(v)

    private fun fmtSigned1(v: Double): String = if (v > 0) "+${fmt1(v)}" else fmt1(v)

    private fun insufficient(key: String, reason: String) =
        TrendConclusion(key, "数据不足，继续记录", reason, enoughData = false)

    /** 体重趋势：窗口内首末条算周均变化率定方向，均值与长期均值对照。入参须按 dateKey 升序。 */
    fun weightTrend(
        weights: List<WeightLogEntity>,
        today: LocalDate = LocalDate.now()
    ): TrendConclusion {
        val windowStart = today.minusDays(WINDOW_DAYS - 1)
        val recent = weights.filter { DateKeys.parse(it.dateKey)?.let { d -> !d.isBefore(windowStart) } == true }
        if (recent.size < 2) {
            return insufficient("weight", "近 90 天内体重记录不足 2 条，攒几天再来看趋势")
        }
        val first = recent.first()
        val last = recent.last()
        val firstDate = DateKeys.parse(first.dateKey) ?: today
        val lastDate = DateKeys.parse(last.dateKey) ?: today
        val spanDays = ChronoUnit.DAYS.between(firstDate, lastDate)
        if (spanDays < 1) {
            return insufficient("weight", "近 90 天的体重记录都挤在同一天，还看不出方向")
        }
        val ratePerWeek = (last.weightKg - first.weightKg) / (spanDays / 7.0)
        val recentMean = recent.map { it.weightKg }.average()
        val prior = weights.filter { DateKeys.parse(it.dateKey)?.isBefore(windowStart) == true }
        val longMean = (if (prior.isNotEmpty()) prior else weights).map { it.weightKg }.average()
        val direction = when {
            ratePerWeek <= -0.10 -> "下降中"
            ratePerWeek >= 0.10 -> "上升中"
            else -> "平稳"
        }
        return TrendConclusion(
            key = "weight",
            headline = "体重$direction",
            detail = "近90天周均 ${fmtSigned1(ratePerWeek)} kg · 近期均值 ${fmt1(recentMean)} kg（长期 ${fmt1(longMean)} kg）",
            enoughData = true
        )
    }

    /** 训练趋势：窗口内周均完成次数 vs 长期周均完成次数。 */
    fun trainingTrend(
        sessions: List<WorkoutSessionEntity>,
        today: LocalDate = LocalDate.now()
    ): TrendConclusion {
        val finished = sessions.filter { it.endedAt != null }
        if (finished.isEmpty()) {
            return insufficient("training", "还没有完成的训练记录")
        }
        val windowStart = today.minusDays(WINDOW_DAYS - 1)
        val recentCount = finished.count {
            DateKeys.parse(it.dateKey)?.let { d -> !d.isBefore(windowStart) } == true
        }
        val windowWeeks = WINDOW_DAYS / 7.0
        val recentPerWeek = recentCount / windowWeeks
        val earliest = finished.mapNotNull { DateKeys.parse(it.dateKey) }.minOrNull() ?: today
        val spanWeeks = (ChronoUnit.DAYS.between(earliest, today) / 7.0).coerceAtLeast(1.0)
        val longPerWeek = finished.size / spanWeeks
        if (longPerWeek <= 0.0) {
            return insufficient("training", "训练记录太少，还看不出节奏")
        }
        if (recentCount == 0) {
            return TrendConclusion(
                key = "training",
                headline = "最近没练",
                detail = "近 90 天没有完成的训练 · 长期周均 ${fmt1(longPerWeek)} 次",
                enoughData = true
            )
        }
        val diffRatio = (recentPerWeek - longPerWeek) / longPerWeek
        val headline = when {
            diffRatio >= 0.15 -> "训练更勤了"
            diffRatio <= -0.15 -> "训练变少了"
            else -> "训练节奏平稳"
        }
        return TrendConclusion(
            key = "training",
            headline = headline,
            detail = "近90天周均 ${fmt1(recentPerWeek)} 次（共 $recentCount 次）· 长期周均 ${fmt1(longPerWeek)} 次",
            enoughData = true
        )
    }

    /** 摄入趋势：窗口内有记录天的日均热量 vs 长期日均热量。 */
    fun intakeTrend(
        logs: List<FoodLogEntity>,
        today: LocalDate = LocalDate.now()
    ): TrendConclusion {
        val daily = logs.groupBy { it.dateKey }
            .mapValues { (_, rows) -> rows.sumOf { it.energy } }
        val windowStart = today.minusDays(WINDOW_DAYS - 1)
        val windowStartKey = DateKeys.of(windowStart)
        val recentDays = daily.filterKeys { it >= windowStartKey }
        if (recentDays.size < 3) {
            return insufficient("intake", "近 90 天内记录饮食的天数不足 3 天，攒几天再来看趋势")
        }
        val recentMean = recentDays.values.average()
        val priorDays = daily.filterKeys { it < windowStartKey }
        val longMean = (if (priorDays.size >= 3) priorDays else daily).values.average()
        if (longMean <= 0.0) {
            return insufficient("intake", "饮食记录太少，还看不出趋势")
        }
        val diffPct = (recentMean - longMean) / longMean * 100.0
        val headline = when {
            diffPct >= 3.0 -> "摄入上升"
            diffPct <= -3.0 -> "摄入下降"
            else -> "摄入平稳"
        }
        return TrendConclusion(
            key = "intake",
            headline = headline,
            detail = "近90天日均 ${recentMean.toInt()} kcal（${recentDays.size} 天有记录）· 长期日均 ${longMean.toInt()} kcal",
            enoughData = true
        )
    }
}
