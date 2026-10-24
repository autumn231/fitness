package com.fitness.app.data

import android.content.Context
import androidx.room.withTransaction
import com.fitness.app.data.local.AppDatabase
import com.fitness.app.data.local.CalendarPlanEntity
import com.fitness.app.data.local.FavoriteEntity
import com.fitness.app.data.local.FoodLogEntity
import com.fitness.app.data.local.PlanEntity
import com.fitness.app.data.local.PlanItemEntity
import com.fitness.app.data.local.PlanWithItems
import com.fitness.app.data.local.RecentEntity
import com.fitness.app.data.model.Exercise
import com.fitness.app.data.model.Food
import com.fitness.app.data.prefs.SettingsDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * 仓库：内存中持有全部动作与食物（启动时一次性加载），并暴露 Room 收藏/最近/计划/食物日志 与设置。
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

    @Volatile
    private var foods: List<Food> = emptyList()

    @Volatile
    private var foodByIdMap: Map<String, Food> = emptyMap()

    val isLoaded: Boolean get() = loaded.isNotEmpty()

    /**
     * 启动时一次性加载。exercises.json 有 16MB，解析必须离开主线程，
     * 否则冷启动会在首帧阻塞 1~2 秒甚至触发 ANR。
     */
    suspend fun load(): List<Exercise> = withContext(Dispatchers.IO) {
        if (loaded.isEmpty()) {
            val list = AssetsLoader.loadExercises(context)
            byIdMap = list.associateBy { it.id }
            loaded = list
        }
        if (foods.isEmpty()) {
            val list = AssetsLoader.loadFoods(context)
            foodByIdMap = list.associateBy { it.id }
            foods = list
        }
        loaded
    }

    fun all(): List<Exercise> = loaded

    fun byId(id: String): Exercise? = byIdMap[id]

    // ---------- 食物 ----------
    fun allFoods(): List<Food> = foods

    fun foodById(id: String): Food? = foodByIdMap[id]

    /** 按关键词搜索食物（命中名称或子类）。 */
    fun searchFoods(query: String, limit: Int = 200): List<Food> {
        val q = query.trim().lowercase()
        if (q.isBlank()) return foods.take(limit)
        return foods.asSequence()
            .filter { it.name.lowercase().contains(q) || it.subcategory.lowercase().contains(q) }
            .take(limit)
            .toList()
    }

    /** 按大类获取食物。 */
    fun foodsByCategory(category: String): List<Food> =
        foods.filter { it.category == category }

    /** 所有食物大类（按数据量降序）。 */
    fun foodCategories(): List<Pair<String, Int>> =
        foods.groupBy { it.category }
            .map { (cat, list) -> cat to list.size }
            .sortedByDescending { it.second }

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

    /**
     * 删除计划时同步清掉它下面的动作与日历安排。
     * 之前只删了 plans 表，plan_items / calendar_plans 变成孤儿数据，
     * 日历里会一直显示"已删除的计划"。
     */
    suspend fun deletePlan(id: Long) {
        database.withTransaction {
            database.planDao().deleteItemsByPlan(id)
            database.calendarDao().removeByPlan(id)
            database.planDao().deletePlan(id)
        }
    }

    suspend fun addItem(item: PlanItemEntity): Long = database.planDao().insertItem(item)

    suspend fun updateItem(item: PlanItemEntity) = database.planDao().updateItem(item)

    suspend fun deleteItem(itemId: Long) = database.planDao().deleteItem(itemId)

    // ---------- 日历安排 ----------
    fun observeCalendarPlans(): Flow<List<CalendarPlanEntity>> = database.calendarDao().observeAll()

    suspend fun getCalendarPlan(dateKey: String): CalendarPlanEntity? =
        database.calendarDao().get(dateKey)

    suspend fun setCalendarPlan(dateKey: String, planId: Long) =
        database.calendarDao().upsert(CalendarPlanEntity(dateKey = dateKey, planId = planId))

    suspend fun removeCalendarPlan(dateKey: String) = database.calendarDao().remove(dateKey)

    // ---------- 食物摄入日志 ----------
    fun observeFoodLogs(dateKey: String): Flow<List<FoodLogEntity>> =
        database.foodLogDao().observeByDate(dateKey)

    /** 添加一条摄入记录：根据食物 id 与克数自动计算营养。 */
    suspend fun addFoodLog(food: Food, amountGram: Double, dateKey: String = DateKeys.today()): Long {
        val factor = amountGram / 100.0
        val entity = FoodLogEntity(
            dateKey = dateKey,
            foodId = food.id,
            foodName = food.name,
            amountGram = amountGram,
            energy = food.energy * factor,
            protein = food.protein * factor,
            carbs = food.carbs * factor,
            fat = food.fat * factor
        )
        return database.foodLogDao().insert(entity)
    }

    suspend fun removeFoodLog(id: Long) = database.foodLogDao().remove(id)

    suspend fun clearFoodLogs(dateKey: String) = database.foodLogDao().clearDate(dateKey)

    // ---------- 跨 Tab 的"去训练"请求 ----------
    // 日历里点"去训练"时把计划 id 放进来，训练页消费后清空。
    // 之前 Calendar 只是跳到 training Tab，计划根本没传过去，用户还得手动再选一次。
    private val _requestedPlanId = MutableStateFlow<Long?>(null)
    val requestedPlanId: StateFlow<Long?> = _requestedPlanId.asStateFlow()

    fun requestPlan(planId: Long) {
        _requestedPlanId.value = planId
    }

    fun consumeRequestedPlan(): Long? {
        val id = _requestedPlanId.value
        _requestedPlanId.value = null
        return id
    }
}
