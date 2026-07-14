package com.fitness.app.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val exerciseId: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "recents")
data class RecentEntity(
    @PrimaryKey val exerciseId: String,
    val viewedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "plans")
data class PlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "plan_items")
data class PlanItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long,
    val exerciseId: String,
    val sets: Int = 3,
    val reps: String = "12",
    val sortOrder: Int = 0
)

data class PlanWithItems(
    @Embedded val plan: PlanEntity,
    @Relation(parentColumn = "id", entityColumn = "planId")
    val items: List<PlanItemEntity>
)
