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

    @Query("DELETE FROM plans WHERE id = :id")
    suspend fun deletePlan(id: Long)
}
