package com.flowtasks.app.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.flowtasks.app.data.local.dao.FocusSessionDao
import com.flowtasks.app.data.local.dao.GoalDao
import com.flowtasks.app.data.local.dao.HabitCompletionDao
import com.flowtasks.app.data.local.dao.HabitDao
import com.flowtasks.app.data.local.dao.ProjectDao
import com.flowtasks.app.data.local.dao.TaskDao
import com.flowtasks.app.data.local.dao.TaskListDao
import com.flowtasks.app.data.local.entity.FocusSessionEntity
import com.flowtasks.app.data.local.entity.GoalEntity
import com.flowtasks.app.data.local.entity.HabitCompletionEntity
import com.flowtasks.app.data.local.entity.HabitEntity
import com.flowtasks.app.data.local.entity.ProjectEntity
import com.flowtasks.app.data.local.entity.TaskEntity
import com.flowtasks.app.data.local.entity.TaskListEntity

@Database(
    entities = [
        TaskEntity::class,
        TaskListEntity::class,
        GoalEntity::class,
        ProjectEntity::class,
        HabitEntity::class,
        HabitCompletionEntity::class,
        FocusSessionEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun taskListDao(): TaskListDao
    abstract fun goalDao(): GoalDao
    abstract fun projectDao(): ProjectDao
    abstract fun habitDao(): HabitDao
    abstract fun habitCompletionDao(): HabitCompletionDao
    abstract fun focusSessionDao(): FocusSessionDao

    companion object {
        private const val DATABASE_NAME = "flow_tasks_database"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN recurrence_type TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE tasks ADD COLUMN recurrence_interval INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE tasks ADD COLUMN recurrence_days_of_week TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE tasks ADD COLUMN reminder_type TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE tasks ADD COLUMN reminder_time INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE tasks ADD COLUMN estimated_duration_minutes INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE tasks ADD COLUMN actual_duration_minutes INTEGER DEFAULT NULL")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create goals table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `goals` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL DEFAULT '',
                        `target_date` INTEGER DEFAULT NULL,
                        `status` TEXT NOT NULL DEFAULT 'ACTIVE',
                        `color_hex` TEXT NOT NULL DEFAULT '#4F46E5',
                        `sort_order` INTEGER NOT NULL DEFAULT 0,
                        `created_at` INTEGER NOT NULL DEFAULT 0,
                        `updated_at` INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_goals_status` ON `goals` (`status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_goals_sort_order` ON `goals` (`sort_order`)")

                // 2. Create projects table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `projects` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `goal_id` INTEGER DEFAULT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL DEFAULT '',
                        `target_date` INTEGER DEFAULT NULL,
                        `status` TEXT NOT NULL DEFAULT 'ACTIVE',
                        `color_hex` TEXT NOT NULL DEFAULT '#0EA5E9',
                        `sort_order` INTEGER NOT NULL DEFAULT 0,
                        `created_at` INTEGER NOT NULL DEFAULT 0,
                        `updated_at` INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(`goal_id`) REFERENCES `goals`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_projects_goal_id` ON `projects` (`goal_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_projects_status` ON `projects` (`status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_projects_sort_order` ON `projects` (`sort_order`)")

                // 3. Create habits table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `habits` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL DEFAULT '',
                        `frequency_type` TEXT NOT NULL DEFAULT 'DAILY',
                        `frequency_days` TEXT DEFAULT NULL,
                        `target_count_per_period` INTEGER NOT NULL DEFAULT 1,
                        `status` TEXT NOT NULL DEFAULT 'ACTIVE',
                        `color_hex` TEXT NOT NULL DEFAULT '#10B981',
                        `sort_order` INTEGER NOT NULL DEFAULT 0,
                        `created_at` INTEGER NOT NULL DEFAULT 0,
                        `updated_at` INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_habits_status` ON `habits` (`status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_habits_sort_order` ON `habits` (`sort_order`)")

                // 4. Create habit_completions table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `habit_completions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `habit_id` INTEGER NOT NULL,
                        `completed_date` INTEGER NOT NULL,
                        `completed_at` INTEGER NOT NULL DEFAULT 0,
                        `notes` TEXT DEFAULT NULL,
                        FOREIGN KEY(`habit_id`) REFERENCES `habits`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_habit_completions_habit_id_completed_date` ON `habit_completions` (`habit_id`, `completed_date`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_habit_completions_habit_id` ON `habit_completions` (`habit_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_habit_completions_completed_date` ON `habit_completions` (`completed_date`)")

                // 5. Recreate tasks table to safely add foreign keys (projects, goals) and all required indices
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `tasks_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `list_id` INTEGER DEFAULT NULL,
                        `project_id` INTEGER DEFAULT NULL,
                        `goal_id` INTEGER DEFAULT NULL,
                        `parent_task_id` INTEGER DEFAULT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL DEFAULT '',
                        `notes` TEXT NOT NULL DEFAULT '',
                        `is_completed` INTEGER NOT NULL DEFAULT 0,
                        `priority` TEXT NOT NULL DEFAULT 'NONE',
                        `created_at` INTEGER NOT NULL DEFAULT 0,
                        `updated_at` INTEGER NOT NULL DEFAULT 0,
                        `due_date` INTEGER DEFAULT NULL,
                        `due_time` TEXT DEFAULT NULL,
                        `completed_at` INTEGER DEFAULT NULL,
                        `sort_order` INTEGER NOT NULL DEFAULT 0,
                        `is_starred` INTEGER NOT NULL DEFAULT 0,
                        `recurrence_type` TEXT DEFAULT NULL,
                        `recurrence_interval` INTEGER NOT NULL DEFAULT 1,
                        `recurrence_days_of_week` TEXT DEFAULT NULL,
                        `reminder_type` TEXT DEFAULT NULL,
                        `reminder_time` INTEGER DEFAULT NULL,
                        `estimated_duration_minutes` INTEGER DEFAULT NULL,
                        `actual_duration_minutes` INTEGER DEFAULT NULL,
                        FOREIGN KEY(`list_id`) REFERENCES `task_lists`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL,
                        FOREIGN KEY(`project_id`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL,
                        FOREIGN KEY(`goal_id`) REFERENCES `goals`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL,
                        FOREIGN KEY(`parent_task_id`) REFERENCES `tasks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())

                // Copy existing task data into tasks_new
                db.execSQL("""
                    INSERT INTO `tasks_new` (
                        `id`, `list_id`, `project_id`, `goal_id`, `parent_task_id`,
                        `title`, `description`, `notes`, `is_completed`, `priority`,
                        `created_at`, `updated_at`, `due_date`, `due_time`, `completed_at`,
                        `sort_order`, `is_starred`, `recurrence_type`, `recurrence_interval`,
                        `recurrence_days_of_week`, `reminder_type`, `reminder_time`,
                        `estimated_duration_minutes`, `actual_duration_minutes`
                    )
                    SELECT
                        `id`, `list_id`, NULL, NULL, `parent_task_id`,
                        `title`, `description`, `notes`, `is_completed`, `priority`,
                        `created_at`, `updated_at`, `due_date`, `due_time`, `completed_at`,
                        `sort_order`, `is_starred`, `recurrence_type`, `recurrence_interval`,
                        `recurrence_days_of_week`, `reminder_type`, `reminder_time`,
                        `estimated_duration_minutes`, `actual_duration_minutes`
                    FROM `tasks`
                """.trimIndent())

                db.execSQL("DROP TABLE `tasks`")
                db.execSQL("ALTER TABLE `tasks_new` RENAME TO `tasks`")

                // Create all indices on tasks table
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_list_id` ON `tasks` (`list_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_project_id` ON `tasks` (`project_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_goal_id` ON `tasks` (`goal_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_parent_task_id` ON `tasks` (`parent_task_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_is_completed` ON `tasks` (`is_completed`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_due_date` ON `tasks` (`due_date`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_sort_order` ON `tasks` (`sort_order`)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `focus_sessions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `task_id` INTEGER DEFAULT NULL,
                        `started_at` INTEGER NOT NULL DEFAULT 0,
                        `ended_at` INTEGER DEFAULT NULL,
                        `duration_seconds` INTEGER NOT NULL DEFAULT 0,
                        `target_duration_minutes` INTEGER NOT NULL DEFAULT 25,
                        `status` TEXT NOT NULL DEFAULT 'COMPLETED',
                        `notes` TEXT DEFAULT NULL,
                        FOREIGN KEY(`task_id`) REFERENCES `tasks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_focus_sessions_task_id` ON `focus_sessions` (`task_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_focus_sessions_started_at` ON `focus_sessions` (`started_at`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_focus_sessions_status` ON `focus_sessions` (`status`)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
        }
    }
}
