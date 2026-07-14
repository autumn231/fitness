package com.fitness.app.data

import android.content.Context
import com.fitness.app.data.local.AppDatabase
import com.fitness.app.data.local.FavoriteEntity
import com.fitness.app.data.local.PlanEntity
import com.fitness.app.data.local.PlanItemEntity
import com.fitness.app.data.local.PlanWithItems
import com.fitness.app.data.local.RecentEntity
import com.fitness.app.data.model.Exercise
import com.fitness.app.data.prefs.SettingsDataStore
import kotlinx.coroutines.flow.Flow

/**
 * 仓库：内存中持有全部动作（启动时一次性加载），并暴露 Room 收藏/最近/计划 与设置。
 */
class ExerciseRepository(
    private val context: Context,
    val database: AppDatabase,
    val settings: SettingsDataStore
) {
    @Volatile
    private var loaded: List<Exercise> = emptyList()

    @Volatile
    private var byIdMap: Map<String, Exercise> = emptyMap()

    val isLoaded: Boolean get() = loaded.isNotEmpty()

    suspend fun load(): List<Exercise> {
        if (loaded.isEmpty()) {
            val list = AssetsLoader.loadExercises(context)
            loaded = list
            byIdMap = list.associateBy { it.id }
        }
        return loaded
    }

    fun all(): List<Exercise> = loaded

    fun byId(id: String): Exercise? = byIdMap[id]

    // ---------- 收藏 ----------
    fun observeFavorites(): Flow<List<FavoriteEntity>> = database.favoriteDao().observeAll()

    fun observeIsFavorite(id: String): Flow<Boolean> = database.favoriteDao().observeIsFavorite(id)

    suspend fun toggleFavorite(id: String) {
        val dao = database.favoriteDao()
        if (dao.isFavorite(id)) dao.remove(id) else dao.add(FavoriteEntity(id))
    }

    // ---------- 最近浏览 ----------
    fun observeRecents(limit: Int = 50): Flow<List<RecentEntity>> =
        database.recentDao().observeAll(limit)

    suspend fun recordView(id: String) = database.recentDao().upsert(RecentEntity(id))

    /** 清空所有最近浏览记录 */
    suspend fun clearRecents() = database.recentDao().clear()

    /** 清空所有收藏 */
    suspend fun clearFavorites() = database.favoriteDao().clear()

    // ---------- 训练计划 ----------
    fun observePlans(): Flow<List<PlanWithItems>> = database.planDao().observePlans()

    fun observePlan(id: Long): Flow<PlanWithItems?> = database.planDao().observePlan(id)

    suspend fun createPlan(name: String, note: String = ""): Long =
        database.planDao().insertPlan(PlanEntity(name = name, note = note))

    suspend fun updatePlan(plan: PlanEntity) = database.planDao().updatePlan(plan)

    suspend fun deletePlan(id: Long) = database.planDao().deletePlan(id)

    suspend fun addItem(item: PlanItemEntity): Long = database.planDao().insertItem(item)

    suspend fun updateItem(item: PlanItemEntity) = database.planDao().updateItem(item)

    suspend fun deleteItem(itemId: Long) = database.planDao().deleteItem(itemId)
}
