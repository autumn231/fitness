package com.fitness.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "fitness_settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

class SettingsDataStore(private val context: Context) {

    private val themeKey = stringPreferencesKey("theme_mode")
    private val tdeeKey = intPreferencesKey("tdee_kcal")

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { p ->
        when (p[themeKey]) {
            "LIGHT" -> ThemeMode.LIGHT
            "DARK" -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[themeKey] = mode.name }
    }

    /** 每日总能量消耗（TDEE，kcal）；0 表示未设置。 */
    val tdee: Flow<Int> = context.dataStore.data.map { it[tdeeKey] ?: 0 }

    suspend fun setTdee(kcal: Int) {
        context.dataStore.edit { it[tdeeKey] = kcal }
    }

    private val homeCardsKey = stringPreferencesKey("home_cards_config")

    /**
     * 今日页概览卡片配置（顺序 + 可见性），序列化为 "id:1,id:0,..."；
     * 空字符串表示从未设置，用默认配置。只存设置层，不进 Room。
     */
    val homeCardsConfig: Flow<String> = context.dataStore.data.map { it[homeCardsKey] ?: "" }

    suspend fun setHomeCardsConfig(value: String) {
        context.dataStore.edit { it[homeCardsKey] = value }
    }
}
