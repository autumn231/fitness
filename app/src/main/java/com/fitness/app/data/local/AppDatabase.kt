package com.fitness.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        FavoriteEntity::class,
        RecentEntity::class,
        PlanEntity::class,
        PlanItemEntity::class,
        CalendarPlanEntity::class,
        FoodLogEntity::class,
        MealTemplateEntity::class,
        MealTemplateItemEntity::class,
        WorkoutSessionEntity::class,
        WorkoutSetEntity::class,
        WeightLogEntity::class,
        ProfileEntity::class,
        UserRecipeEntity::class,
        UserRecipeItemEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun recentDao(): RecentDao
    abstract fun planDao(): PlanDao
    abstract fun calendarDao(): CalendarDao
    abstract fun foodLogDao(): FoodLogDao
    abstract fun mealTemplateDao(): MealTemplateDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun weightDao(): WeightDao
    abstract fun profileDao(): ProfileDao
    abstract fun userRecipeDao(): UserRecipeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * v3 → v4：食物日志加餐次列；新增常用餐、训练会话/组记录、体重、个人档案五张表。
         * 全部为加列/建新表，用户已有的计划与记录原样保留。
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `food_logs` ADD COLUMN `meal` TEXT NOT NULL DEFAULT '加餐'")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `meal_templates` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `meal_template_items` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`templateId` INTEGER NOT NULL, " +
                        "`foodId` TEXT NOT NULL, " +
                        "`foodName` TEXT NOT NULL, " +
                        "`amountGram` REAL NOT NULL, " +
                        "`energy` REAL NOT NULL, " +
                        "`protein` REAL NOT NULL, " +
                        "`carbs` REAL NOT NULL, " +
                        "`fat` REAL NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `workout_sessions` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`planId` INTEGER, " +
                        "`planName` TEXT NOT NULL, " +
                        "`dateKey` TEXT NOT NULL, " +
                        "`startedAt` INTEGER NOT NULL, " +
                        "`endedAt` INTEGER, " +
                        "`durationSec` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `workout_sets` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`sessionId` INTEGER NOT NULL, " +
                        "`exerciseId` TEXT NOT NULL, " +
                        "`exerciseName` TEXT NOT NULL, " +
                        "`setIndex` INTEGER NOT NULL, " +
                        "`weightKg` REAL NOT NULL, " +
                        "`reps` INTEGER NOT NULL, " +
                        "`done` INTEGER NOT NULL, " +
                        "`sortOrder` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `weight_logs` (" +
                        "`dateKey` TEXT NOT NULL, " +
                        "`weightKg` REAL NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`dateKey`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `user_profile` (" +
                        "`id` INTEGER NOT NULL, " +
                        "`sex` TEXT NOT NULL, " +
                        "`birthYear` INTEGER NOT NULL, " +
                        "`heightCm` REAL NOT NULL, " +
                        "`activityFactor` REAL NOT NULL, " +
                        "PRIMARY KEY(`id`))"
                )
            }
        }

        /** v4 → v5：新增用户自建食谱两张表，其余数据不动。 */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `user_recipes` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`title` TEXT NOT NULL, " +
                        "`category` TEXT NOT NULL, " +
                        "`minutes` INTEGER NOT NULL, " +
                        "`tags` TEXT NOT NULL, " +
                        "`steps` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `user_recipe_items` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`recipeId` INTEGER NOT NULL, " +
                        "`foodId` TEXT NOT NULL, " +
                        "`gram` REAL NOT NULL, " +
                        "`note` TEXT NOT NULL, " +
                        "`sortOrder` INTEGER NOT NULL)"
                )
            }
        }

        fun get(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "fitness.db"
            )
                .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
                // v3 之前的版本当年就没有迁移路径，继续沿用旧行为；
                // v3 → v4 走上面的正式迁移，不动用户数据。
                .fallbackToDestructiveMigration()
                .build().also { INSTANCE = it }
        }
    }
}
