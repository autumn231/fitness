package com.fitness.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE exerciseId = :id)")
    fun observeIsFavorite(id: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE exerciseId = :id)")
    suspend fun isFavorite(id: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(entity: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE exerciseId = :id")
    suspend fun remove(id: String)

    @Query("DELETE FROM favorites")
    suspend fun clear()
}

@Dao
interface RecentDao {
    @Query("SELECT * FROM recents ORDER BY viewedAt DESC LIMIT :limit")
    fun observeAll(limit: Int = 50): Flow<List<RecentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: RecentEntity)

    @Query("DELETE FROM recents")
    suspend fun clear()
}

@Dao
interface PlanDao {
    @Transaction
    @Query("SELECT * FROM plans ORDER BY createdAt DESC")
    fun observePlans(): Flow<List<PlanWithItems>>

    @Transaction
    @Query("SELECT * FROM plans WHERE id = :id")
    fun observePlan(id: Long): Flow<PlanWithItems?>

    @Insert
    suspend fun insertPlan(plan: PlanEntity): Long

    @Update
    suspend fun updatePlan(plan: PlanEntity)

    @Insert
    suspend fun insertItem(item: PlanItemEntity): Long

    @Update
    suspend fun updateItem(item: PlanItemEntity)

    @Query("DELETE FROM plan_items WHERE id = :itemId")
    suspend fun deleteItem(itemId: Long)

    @Query("DELETE FROM plan_items WHERE planId = :planId")
    suspend fun deleteItemsByPlan(planId: Long)

    @Query("DELETE FROM plans WHERE id = :id")
    suspend fun deletePlan(id: Long)
}

@Dao
interface CalendarDao {
    @Query("SELECT * FROM calendar_plans ORDER BY dateKey ASC")
    fun observeAll(): Flow<List<CalendarPlanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CalendarPlanEntity)

    @Query("DELETE FROM calendar_plans WHERE dateKey = :dateKey")
    suspend fun remove(dateKey: String)

    @Query("DELETE FROM calendar_plans WHERE planId = :planId")
    suspend fun removeByPlan(planId: Long)

    @Query("DELETE FROM calendar_plans")
    suspend fun clear()
}

@Dao
interface FoodLogDao {
    @Query("SELECT * FROM food_logs WHERE dateKey = :dateKey ORDER BY addedAt DESC")
    fun observeByDate(dateKey: String): Flow<List<FoodLogEntity>>

    @Query("SELECT * FROM food_logs WHERE dateKey = :dateKey AND meal = :meal ORDER BY addedAt ASC")
    suspend fun getByDateAndMeal(dateKey: String, meal: String): List<FoodLogEntity>

    /** 最近记录（供「最近/最常吃」聚合）：按时间倒序取最近 N 条。 */
    @Query("SELECT * FROM food_logs ORDER BY addedAt DESC LIMIT :limit")
    suspend fun recent(limit: Int = 300): List<FoodLogEntity>

    /** 全部记录（趋势统计按天聚合用）。 */
    @Query("SELECT * FROM food_logs ORDER BY dateKey ASC, addedAt ASC")
    fun observeAll(): Flow<List<FoodLogEntity>>

    @Insert
    suspend fun insert(entity: FoodLogEntity): Long

    @Update
    suspend fun update(entity: FoodLogEntity)

    @Query("DELETE FROM food_logs WHERE id = :id")
    suspend fun remove(id: Long)

    @Query("DELETE FROM food_logs WHERE dateKey = :dateKey")
    suspend fun clearDate(dateKey: String)

    @Query("DELETE FROM food_logs")
    suspend fun clear()
}

@Dao
interface MealTemplateDao {
    @Transaction
    @Query("SELECT * FROM meal_templates ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<MealTemplateWithItems>>

    @Insert
    suspend fun insertTemplate(entity: MealTemplateEntity): Long

    @Insert
    suspend fun insertItem(entity: MealTemplateItemEntity): Long

    @Query("DELETE FROM meal_templates WHERE id = :id")
    suspend fun deleteTemplate(id: Long)

    @Query("DELETE FROM meal_template_items WHERE templateId = :templateId")
    suspend fun deleteItems(templateId: Long)
}

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workout_sessions ORDER BY startedAt DESC")
    fun observeSessions(): Flow<List<WorkoutSessionEntity>>

    /** 进行中的会话（endedAt 为空）至多一条，训练页靠它恢复现场。 */
    @Query("SELECT * FROM workout_sessions WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    fun observeActiveSession(): Flow<WorkoutSessionEntity?>

    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    suspend fun getSession(id: Long): WorkoutSessionEntity?

    @Insert
    suspend fun insertSession(entity: WorkoutSessionEntity): Long

    @Update
    suspend fun updateSession(entity: WorkoutSessionEntity)

    @Query("DELETE FROM workout_sessions WHERE id = :id")
    suspend fun deleteSession(id: Long)

    @Query("SELECT * FROM workout_sets WHERE sessionId = :sessionId ORDER BY sortOrder ASC, setIndex ASC")
    fun observeSets(sessionId: Long): Flow<List<WorkoutSetEntity>>

    @Query("SELECT * FROM workout_sets WHERE sessionId = :sessionId ORDER BY sortOrder ASC, setIndex ASC")
    suspend fun getSets(sessionId: Long): List<WorkoutSetEntity>

    @Insert
    suspend fun insertSet(entity: WorkoutSetEntity): Long

    @Update
    suspend fun updateSet(entity: WorkoutSetEntity)

    @Query("DELETE FROM workout_sets WHERE id = :id")
    suspend fun deleteSet(id: Long)

    @Query("DELETE FROM workout_sets WHERE sessionId = :sessionId")
    suspend fun deleteSetsBySession(sessionId: Long)

    @Query("DELETE FROM workout_sets WHERE sessionId = :sessionId AND exerciseId = :exerciseId")
    suspend fun deleteSetsByExercise(sessionId: Long, exerciseId: String)

    /**
     * 某动作在指定会话之前的最近一次已完成会话里的组记录（Previous 内联/预填用）。
     * 先定位最近一次包含该动作的已结束会话，再取它在该会话的全部组。
     */
    @Query(
        """
        SELECT * FROM workout_sets
        WHERE exerciseId = :exerciseId
          AND sessionId = (
              SELECT ws.sessionId FROM workout_sets ws
              JOIN workout_sessions s ON s.id = ws.sessionId
              WHERE ws.exerciseId = :exerciseId AND s.endedAt IS NOT NULL AND s.id != :excludeSessionId
              ORDER BY s.startedAt DESC LIMIT 1
          )
        ORDER BY setIndex ASC
        """
    )
    suspend fun previousSets(exerciseId: String, excludeSessionId: Long): List<WorkoutSetEntity>

    /** 某动作的历史最大重量（PR 判定用，不含指定会话）。 */
    @Query(
        """
        SELECT MAX(ws.weightKg) FROM workout_sets ws
        JOIN workout_sessions s ON s.id = ws.sessionId
        WHERE ws.exerciseId = :exerciseId AND ws.done = 1 AND s.endedAt IS NOT NULL AND s.id != :excludeSessionId
        """
    )
    suspend fun bestWeightBefore(exerciseId: String, excludeSessionId: Long): Double?
}

@Dao
interface WeightDao {
    @Query("SELECT * FROM weight_logs ORDER BY dateKey ASC")
    fun observeAll(): Flow<List<WeightLogEntity>>

    @Query("SELECT * FROM weight_logs ORDER BY dateKey DESC LIMIT 1")
    fun observeLatest(): Flow<WeightLogEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: WeightLogEntity)

    @Query("DELETE FROM weight_logs WHERE dateKey = :dateKey")
    suspend fun remove(dateKey: String)
}

@Dao
interface ProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun observe(): Flow<ProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun get(): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ProfileEntity)
}

@Dao
interface UserRecipeDao {
    @Transaction
    @Query("SELECT * FROM user_recipes ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<UserRecipeWithItems>>

    @Query("SELECT * FROM user_recipes WHERE id = :id")
    suspend fun getRecipe(id: Long): UserRecipeEntity?

    @Insert
    suspend fun insertRecipe(entity: UserRecipeEntity): Long

    @Update
    suspend fun updateRecipe(entity: UserRecipeEntity)

    @Insert
    suspend fun insertItem(entity: UserRecipeItemEntity): Long

    @Query("DELETE FROM user_recipes WHERE id = :id")
    suspend fun deleteRecipe(id: Long)

    @Query("DELETE FROM user_recipe_items WHERE recipeId = :recipeId")
    suspend fun deleteItems(recipeId: Long)
}
