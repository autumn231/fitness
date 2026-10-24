package com.fitness.app.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 全局统一的日期键（yyyy-MM-dd）。
 * 之前 UI 用 SimpleDateFormat(Locale.getDefault())、仓库用 Locale.CHINA，
 * 某些语言环境下会得到不同分隔符，导致"今天"的记录对不上，这里统一收口。
 */
object DateKeys {
    private val ISO: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun of(date: LocalDate): String = date.format(ISO)

    fun today(): String = of(LocalDate.now())

    fun parse(key: String): LocalDate? = runCatching { LocalDate.parse(key, ISO) }.getOrNull()
}
