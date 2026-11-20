package com.fitness.app.ui.feature

/** 重量数字格式：整数不带小数，否则保留一位（原在 TrainingScreen，多个页面共用，抽出独立文件）。 */
fun formatKg(v: Double): String =
    if (v % 1.0 == 0.0) v.toInt().toString() else "%.1f".format(v)

/** 格式化秒为 hh:mm:ss（始终显示小时，支持长时间训练）。 */
fun formatClock(seconds: Long): String {
    val s = seconds.coerceAtLeast(0L)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return "%02d:%02d:%02d".format(h, m, sec)
}
