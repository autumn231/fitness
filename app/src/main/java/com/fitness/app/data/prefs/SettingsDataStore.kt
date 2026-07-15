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
}
