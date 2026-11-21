package com.fitness.app.data

import android.content.Context
import androidx.room.withTransaction
import com.fitness.app.data.local.AppDatabase
import com.fitness.app.data.local.CalendarPlanEntity
import com.fitness.app.data.local.FavoriteEntity
import com.fitness.app.data.local.FoodLogEntity
import com.fitness.app.data.local.MealTemplateEntity
import com.fitness.app.data.local.MealTemplateItemEntity
import com.fitness.app.data.local.MealTemplateWithItems
import com.fitness.app.data.local.PlanEntity
import com.fitness.app.data.local.PlanItemEntity
import com.fitness.app.data.local.PlanWithItems
import com.fitness.app.data.local.ProfileEntity
import com.fitness.app.data.local.RecentEntity
import com.fitness.app.data.local.UserRecipeEntity
import com.fitness.app.data.local.UserRecipeItemEntity
import com.fitness.app.data.local.UserRecipeWithItems
import com.fitness.app.data.local.WeightLogEntity
import com.fitness.app.data.local.WorkoutSessionEntity
import com.fitness.app.data.local.WorkoutSetEntity
import com.fitness.app.i18n.displayName
import com.fitness.app.data.model.Exercise
import com.fitness.app.data.model.Food
import com.fitness.app.data.model.NutritionTotals
import com.fitness.app.data.model.PlanTemplate
import com.fitness.app.data.model.Recipe
import com.fitness.app.data.model.RecipeIngredient
import com.fitness.app.data.prefs.SettingsDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
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

    @Volatile
    private var planTemplates: List<PlanTemplate> = emptyList()

    @Volatile
    private var recipes: List<Recipe> = emptyList()

    @Volatile
    private var recipeByIdMap: Map<String, Recipe> = emptyMap()

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
        if (planTemplates.isEmpty()) {
            planTemplates = AssetsLoader.loadPlanTemplates(context)
        }
        if (recipes.isEmpty()) {
            val list = AssetsLoader.loadRecipes(context)
            recipeByIdMap = list.associateBy { it.id }
            recipes = list
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

    suspend fun setCalendarPlan(dateKey: String, planId: Long) =
        database.calendarDao().upsert(CalendarPlanEntity(dateKey = dateKey, planId = planId))

    suspend fun removeCalendarPlan(dateKey: String) = database.calendarDao().remove(dateKey)

    // ---------- 食物摄入日志 ----------
    fun observeFoodLogs(dateKey: String): Flow<List<FoodLogEntity>> =
        database.foodLogDao().observeByDate(dateKey)

    /** 全部摄入记录（趋势统计用）。 */
    fun observeAllFoodLogs(): Flow<List<FoodLogEntity>> =
        database.foodLogDao().observeAll()

    /** 添加一条摄入记录：根据食物 id 与克数自动计算营养。 */
    suspend fun addFoodLog(
        food: Food,
        amountGram: Double,
        dateKey: String = DateKeys.today(),
        meal: String = Meals.inferNow()
    ): Long {
        val factor = amountGram / 100.0
        val entity = FoodLogEntity(
            dateKey = dateKey,
            foodId = food.id,
            foodName = food.name,
            amountGram = amountGram,
            energy = food.energy * factor,
            protein = food.protein * factor,
            carbs = food.carbs * factor,
            fat = food.fat * factor,
            meal = meal
        )
        return database.foodLogDao().insert(entity)
    }

    /** Quick Add：只知道总热量时直接入账，不拆食材（外食场景）。 */
    suspend fun quickAddFoodLog(
        energyKcal: Double,
        name: String,
        dateKey: String,
        meal: String
    ): Long {
        val entity = FoodLogEntity(
            dateKey = dateKey,
            foodId = QUICK_ADD_FOOD_ID,
            foodName = name.ifBlank { "快速添加" },
            amountGram = 0.0,
            energy = energyKcal,
            protein = 0.0,
            carbs = 0.0,
            fat = 0.0,
            meal = meal
        )
        return database.foodLogDao().insert(entity)
    }

    /** 改一条记录的克数：能找到原食物时按每 100g 成分重算，否则按原克数等比缩放。 */
    suspend fun updateFoodLogAmount(log: FoodLogEntity, newGram: Double) {
        if (newGram <= 0) return
        val food = foodById(log.foodId)
        val updated = if (food != null) {
            val f = newGram / 100.0
            log.copy(
                amountGram = newGram,
                energy = food.energy * f,
                protein = food.protein * f,
                carbs = food.carbs * f,
                fat = food.fat * f
            )
        } else if (log.amountGram > 0) {
            val r = newGram / log.amountGram
            log.copy(
                amountGram = newGram,
                energy = log.energy * r,
                protein = log.protein * r,
                carbs = log.carbs * r,
                fat = log.fat * r
            )
        } else {
            log.copy(amountGram = newGram)
        }
        database.foodLogDao().update(updated)
    }

    /** 把某天某餐整组复制到目标日期的同餐（「复制昨天同餐」的实现）。返回复制条数。 */
    suspend fun copyMeal(fromDateKey: String, meal: String, toDateKey: String): Int {
        val rows = database.foodLogDao().getByDateAndMeal(fromDateKey, meal)
        val now = System.currentTimeMillis()
        rows.forEachIndexed { idx, row ->
            database.foodLogDao().insert(
                row.copy(id = 0, dateKey = toDateKey, addedAt = now + idx)
            )
        }
        return rows.size
    }

    /**
     * 「最近 / 最常吃」聚合：添加食物页的默认落地内容，全部来自本地已有记录。
     * 返回 (最近吃过, 最常吃) 两组食物（已去重、剔除快速添加与库中不存在的条目）。
     */
    suspend fun recentAndFrequentFoods(): Pair<List<Food>, List<Food>> {
        val rows = database.foodLogDao().recent(300)
            .filter { it.foodId != QUICK_ADD_FOOD_ID }
        val recent = rows.mapNotNull { foodById(it.foodId) }.distinctBy { it.id }.take(20)
        val frequent = rows.groupBy { it.foodId }
            .entries.sortedByDescending { it.value.size }
            .mapNotNull { foodById(it.key) }
            .take(20)
        return recent to frequent
    }

    /** 最近的原始日志行（添加页用来预填上次的克数）。 */
    suspend fun recentFoodEntries(limit: Int = 300): List<FoodLogEntity> =
        database.foodLogDao().recent(limit)

    suspend fun removeFoodLog(id: Long) = database.foodLogDao().remove(id)

    suspend fun clearFoodLogs(dateKey: String) = database.foodLogDao().clearDate(dateKey)

    // ---------- 常用餐模板 ----------
    fun observeMealTemplates(): Flow<List<MealTemplateWithItems>> =
        database.mealTemplateDao().observeAll()

    /** 把一组日志行存为常用餐（快照克数与营养）。 */
    suspend fun saveMealTemplate(name: String, logs: List<FoodLogEntity>): Long {
        val dao = database.mealTemplateDao()
        val templateId = dao.insertTemplate(MealTemplateEntity(name = name))
        logs.forEach { log ->
            dao.insertItem(
                MealTemplateItemEntity(
                    templateId = templateId,
                    foodId = log.foodId,
                    foodName = log.foodName,
                    amountGram = log.amountGram,
                    energy = log.energy,
                    protein = log.protein,
                    carbs = log.carbs,
                    fat = log.fat
                )
            )
        }
        return templateId
    }

    /** 应用常用餐：把模板条目整组写成目标日期某餐的日志。返回写入条数。 */
    suspend fun applyMealTemplate(
        template: MealTemplateWithItems,
        dateKey: String,
        meal: String
    ): Int {
        val now = System.currentTimeMillis()
        template.items.forEachIndexed { idx, item ->
            database.foodLogDao().insert(
                FoodLogEntity(
                    dateKey = dateKey,
                    foodId = item.foodId,
                    foodName = item.foodName,
                    amountGram = item.amountGram,
                    energy = item.energy,
                    protein = item.protein,
                    carbs = item.carbs,
                    fat = item.fat,
                    meal = meal,
                    addedAt = now + idx
                )
            )
        }
        return template.items.size
    }

    suspend fun deleteMealTemplate(id: Long) {
        database.withTransaction {
            database.mealTemplateDao().deleteItems(id)
            database.mealTemplateDao().deleteTemplate(id)
        }
    }

    // ---------- 训练会话 ----------
    fun observeSessions(): Flow<List<WorkoutSessionEntity>> =
        database.workoutDao().observeSessions()

    fun observeActiveSession(): Flow<WorkoutSessionEntity?> =
        database.workoutDao().observeActiveSession()

    fun observeSets(sessionId: Long): Flow<List<WorkoutSetEntity>> =
        database.workoutDao().observeSets(sessionId)

    /**
     * 按计划开始一次训练：建会话并按计划项预生成组记录，
     * 每组的重量/次数预填该动作上一次训练的同组数值（Previous 机制）。
     */
    suspend fun startSessionFromPlan(plan: PlanWithItems): Long {
        val dao = database.workoutDao()
        val sessionId = dao.insertSession(
            WorkoutSessionEntity(
                planId = plan.plan.id,
                planName = plan.plan.name,
                dateKey = DateKeys.today(),
                startedAt = System.currentTimeMillis()
            )
        )
        plan.items.sortedBy { it.sortOrder }.forEachIndexed { order, item ->
            val exercise = byId(item.exerciseId)
            val name = exercise?.displayName() ?: "未知动作"
            val prev = dao.previousSets(item.exerciseId, sessionId)
            val defaultReps = item.reps.trim().toIntOrNull() ?: 12
            repeat(item.sets.coerceAtLeast(1)) { idx ->
                val p = prev.getOrNull(idx) ?: prev.lastOrNull()
                dao.insertSet(
                    WorkoutSetEntity(
                        sessionId = sessionId,
                        exerciseId = item.exerciseId,
                        exerciseName = name,
                        setIndex = idx,
                        weightKg = p?.weightKg ?: 0.0,
                        reps = p?.reps ?: defaultReps,
                        done = false,
                        sortOrder = order
                    )
                )
            }
        }
        return sessionId
    }

    /** 空白（自由）训练：只建会话，动作由用户在执行页里加。 */
    suspend fun startFreeSession(): Long =
        database.workoutDao().insertSession(
            WorkoutSessionEntity(
                planId = null,
                planName = "自由训练",
                dateKey = DateKeys.today(),
                startedAt = System.currentTimeMillis()
            )
        )

    /** 往会话里加一个动作（默认 3 组，预填上次数值）。 */
    suspend fun addExerciseToSession(sessionId: Long, exerciseId: String, setCount: Int = 3) {
        val dao = database.workoutDao()
        val existing = dao.getSets(sessionId)
        val order = (existing.maxOfOrNull { it.sortOrder } ?: -1) + 1
        val exercise = byId(exerciseId)
        val name = exercise?.displayName() ?: "未知动作"
        val prev = dao.previousSets(exerciseId, sessionId)
        repeat(setCount.coerceAtLeast(1)) { idx ->
            val p = prev.getOrNull(idx) ?: prev.lastOrNull()
            dao.insertSet(
                WorkoutSetEntity(
                    sessionId = sessionId,
                    exerciseId = exerciseId,
                    exerciseName = name,
                    setIndex = idx,
                    weightKg = p?.weightKg ?: 0.0,
                    reps = p?.reps ?: 12,
                    done = false,
                    sortOrder = order
                )
            )
        }
    }

    /** 给会话内某动作加一组（复制该动作最后一组的数值）。 */
    suspend fun addSetToExercise(sessionId: Long, exerciseId: String) {
        val dao = database.workoutDao()
        val sets = dao.getSets(sessionId).filter { it.exerciseId == exerciseId }
        if (sets.isEmpty()) return
        val last = sets.maxByOrNull { it.setIndex } ?: return
        dao.insertSet(
            last.copy(
                id = 0,
                setIndex = last.setIndex + 1,
                done = false
            )
        )
    }

    suspend fun updateSet(entity: WorkoutSetEntity) = database.workoutDao().updateSet(entity)

    suspend fun removeSet(id: Long) = database.workoutDao().deleteSet(id)

    suspend fun removeExerciseFromSession(sessionId: Long, exerciseId: String) =
        database.workoutDao().deleteSetsByExercise(sessionId, exerciseId)

    /** 某动作上一次训练的组记录（执行页 Previous 列展示用）。 */
    suspend fun previousSets(exerciseId: String, excludeSessionId: Long): List<WorkoutSetEntity> =
        database.workoutDao().previousSets(exerciseId, excludeSessionId)

    /** 某动作在指定会话之前的历史最大重量（总结页 PR 判定用）。 */
    suspend fun bestWeightBefore(exerciseId: String, excludeSessionId: Long): Double? =
        database.workoutDao().bestWeightBefore(exerciseId, excludeSessionId)

    /** 一次会话的全部组记录（历史详情/总结计算用）。 */
    suspend fun sessionSets(sessionId: Long): List<WorkoutSetEntity> =
        database.workoutDao().getSets(sessionId)

    /** 结束会话：写入结束时间与总时长。 */
    suspend fun finishSession(session: WorkoutSessionEntity, durationSec: Long) {
        database.workoutDao().updateSession(
            session.copy(endedAt = System.currentTimeMillis(), durationSec = durationSec)
        )
    }

    suspend fun deleteSession(id: Long) {
        database.withTransaction {
            database.workoutDao().deleteSetsBySession(id)
            database.workoutDao().deleteSession(id)
        }
    }

    /** 把一次完成的自由训练反存为计划模板（Hevy 式）：按会话组记录生成计划与计划项。 */
    suspend fun saveSessionAsPlan(session: WorkoutSessionEntity, name: String): Long {
        val sets = database.workoutDao().getSets(session.id)
        val planId = createPlan(name)
        sets.groupBy { it.exerciseId }
            .entries.sortedBy { entry -> entry.value.minOf { it.sortOrder } }
            .forEach { (exerciseId, exSets) ->
                val doneSets = exSets.filter { it.done }.ifEmpty { exSets }
                addItem(
                    PlanItemEntity(
                        planId = planId,
                        exerciseId = exerciseId,
                        sets = doneSets.size,
                        reps = (doneSets.firstOrNull()?.reps ?: 12).toString(),
                        sortOrder = exSets.minOf { it.sortOrder }
                    )
                )
            }
        return planId
    }

    // ---------- 体重 ----------
    fun observeWeights(): Flow<List<WeightLogEntity>> = database.weightDao().observeAll()

    fun observeLatestWeight(): Flow<WeightLogEntity?> = database.weightDao().observeLatest()

    suspend fun setWeight(dateKey: String, weightKg: Double) =
        database.weightDao().upsert(WeightLogEntity(dateKey = dateKey, weightKg = weightKg))

    suspend fun removeWeight(dateKey: String) = database.weightDao().remove(dateKey)

    // ---------- 个人档案 ----------
    fun observeProfile(): Flow<ProfileEntity?> = database.profileDao().observe()

    suspend fun saveProfile(profile: ProfileEntity) = database.profileDao().upsert(profile)

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

    // ---------- 计划模板 ----------

    fun planTemplates(): List<PlanTemplate> = planTemplates

    fun planTemplateById(id: String): PlanTemplate? = planTemplates.firstOrNull { it.id == id }

    /**
     * 套用计划模板：按训练日各新建一个计划（多日模板命名为「模板名 · 日名」），
     * 返回新建的计划数量。套用后是普通计划，可自由改名、改动作、排日历。
     */
    suspend fun applyPlanTemplate(template: PlanTemplate): Int {
        var created = 0
        for (day in template.days) {
            val planName = if (template.days.size > 1) {
                "${template.title} · ${day.name}"
            } else {
                template.title
            }
            val planId = createPlan(planName, template.desc)
            day.items.forEachIndexed { index, ti ->
                addItem(
                    PlanItemEntity(
                        planId = planId,
                        exerciseId = ti.exerciseId,
                        sets = ti.sets,
                        reps = ti.reps,
                        sortOrder = index
                    )
                )
            }
            created++
        }
        return created
    }

    // ---------- 食谱 ----------

    fun recipes(): List<Recipe> = recipes

    fun recipeById(id: String): Recipe? = recipeByIdMap[id]

    /** 全部食谱（内置 + 用户自建）：UI 统一订阅这个流，自建增删改会实时刷新。 */
    fun observeAllRecipes(): Flow<List<Recipe>> =
        database.userRecipeDao().observeAll().map { userList ->
            recipes + userList.map { it.toRecipe() }
        }

    private fun UserRecipeWithItems.toRecipe(): Recipe = Recipe(
        id = "user_${recipe.id}",
        title = recipe.title,
        category = recipe.category,
        minutes = recipe.minutes,
        tags = recipe.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() },
        ingredients = items.sortedBy { it.sortOrder }
            .map { RecipeIngredient(it.foodId, it.gram, it.note) },
        steps = recipe.steps.split("\n").map { it.trim() }.filter { it.isNotEmpty() },
        editable = true
    )

    /** 保存用户食谱：id 形如 user_N 时更新（含整表替换食材），否则新建。返回数据库 id。 */
    suspend fun saveUserRecipe(recipe: Recipe): Long {
        val dao = database.userRecipeDao()
        val tagsJoined = recipe.tags.joinToString(",")
        val stepsJoined = recipe.steps.joinToString("\n")
        val dbId = if (recipe.id.startsWith("user_")) {
            val id = recipe.id.removePrefix("user_").toLong()
            val existing = dao.getRecipe(id) ?: return -1
            dao.updateRecipe(
                existing.copy(
                    title = recipe.title,
                    category = recipe.category,
                    minutes = recipe.minutes,
                    tags = tagsJoined,
                    steps = stepsJoined
                )
            )
            dao.deleteItems(id)
            id
        } else {
            dao.insertRecipe(
                UserRecipeEntity(
                    title = recipe.title,
                    category = recipe.category,
                    minutes = recipe.minutes,
                    tags = tagsJoined,
                    steps = stepsJoined
                )
            )
        }
        recipe.ingredients.forEachIndexed { index, ing ->
            dao.insertItem(
                UserRecipeItemEntity(
                    recipeId = dbId,
                    foodId = ing.foodId,
                    gram = ing.gram,
                    note = ing.note,
                    sortOrder = index
                )
            )
        }
        return dbId
    }

    /** 删除用户自建食谱（内置食谱 id 不以 user_ 开头，调用无副作用）。 */
    suspend fun deleteUserRecipe(recipe: Recipe) {
        if (!recipe.id.startsWith("user_")) return
        val dao = database.userRecipeDao()
        val id = recipe.id.removePrefix("user_").toLong()
        dao.deleteItems(id)
        dao.deleteRecipe(id)
    }

    /** 食谱营养汇总：逐食材查食物库，按克数与倍数加总（与食物日志同一口径）。 */
    fun recipeNutrition(recipe: Recipe, scale: Double = 1.0): NutritionTotals {
        var energy = 0.0
        var protein = 0.0
        var carbs = 0.0
        var fat = 0.0
        for (ing in recipe.ingredients) {
            val food = foodById(ing.foodId) ?: continue
            val f = ing.gram * scale / 100.0
            energy += food.energy * f
            protein += food.protein * f
            carbs += food.carbs * f
            fat += food.fat * f
        }
        return NutritionTotals(energy, protein, carbs, fat)
    }

    /**
     * 把食谱加入今日指定餐次：逐食材各写一条食物日志，返回实际写入条数。
     * 之后在今日页可逐条改克数或删除，与手工添加无区别。
     */
    suspend fun addRecipeToToday(recipe: Recipe, scale: Double, meal: String): Int {
        var written = 0
        for (ing in recipe.ingredients) {
            val food = foodById(ing.foodId) ?: continue
            addFoodLog(food, ing.gram * scale, meal = meal)
            written++
        }
        return written
    }

    companion object {
        /** 快速添加（只填热量）记录的 foodId 标记，不对应食物库中的真实食物。 */
        const val QUICK_ADD_FOOD_ID = "quick_add"
    }
}
