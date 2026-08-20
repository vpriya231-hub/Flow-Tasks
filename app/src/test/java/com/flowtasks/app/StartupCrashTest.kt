package com.flowtasks.app

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.flowtasks.app.core.database.AppDatabase
import com.flowtasks.app.domain.model.Task
import com.flowtasks.app.domain.model.TaskPriority
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = FlowTasksApplication::class)
class StartupCrashTest {

    @Test
    fun testAppLaunch() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { activity ->
            assertNotNull(activity)
        }
        scenario.close()
    }

    @Test
    fun testDatabaseStartupOnDisk() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getInstance(context)
        assertNotNull(db)
        val taskDao = db.taskDao()
        val goalDao = db.goalDao()
        val projectDao = db.projectDao()
        val habitDao = db.habitDao()
        val completionDao = db.habitCompletionDao()

        assertEquals(0, taskDao.getAllRootTasks().first().size)
        assertEquals(0, goalDao.getAllGoals().first().size)
        assertEquals(0, projectDao.getAllProjects().first().size)
        assertEquals(0, habitDao.getAllHabits().first().size)
        assertEquals(0, completionDao.getCompletionsInDateRange(0, Long.MAX_VALUE).first().size)
    }

    @Test
    fun testMigrationFromV1ToV3() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbFile = context.getDatabasePath("test_migration_v1_v3.db")
        if (dbFile.exists()) dbFile.delete()

        // Create V1 schema manually
        val config = Configuration.builder(context)
            .name("test_migration_v1_v3.db")
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS task_lists (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            name TEXT NOT NULL,
                            color_hex TEXT DEFAULT NULL,
                            icon_name TEXT DEFAULT NULL,
                            created_at INTEGER NOT NULL DEFAULT 0,
                            updated_at INTEGER NOT NULL DEFAULT 0,
                            sort_order INTEGER NOT NULL DEFAULT 0
                        )
                    """.trimIndent())

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS tasks (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            list_id INTEGER DEFAULT NULL,
                            parent_task_id INTEGER DEFAULT NULL,
                            title TEXT NOT NULL,
                            description TEXT NOT NULL DEFAULT '',
                            notes TEXT NOT NULL DEFAULT '',
                            is_completed INTEGER NOT NULL DEFAULT 0,
                            priority TEXT NOT NULL DEFAULT 'NONE',
                            created_at INTEGER NOT NULL DEFAULT 0,
                            updated_at INTEGER NOT NULL DEFAULT 0,
                            due_date INTEGER DEFAULT NULL,
                            due_time TEXT DEFAULT NULL,
                            completed_at INTEGER DEFAULT NULL,
                            sort_order INTEGER NOT NULL DEFAULT 0,
                            is_starred INTEGER NOT NULL DEFAULT 0,
                            FOREIGN KEY(list_id) REFERENCES task_lists(id) ON DELETE SET NULL,
                            FOREIGN KEY(parent_task_id) REFERENCES tasks(id) ON DELETE CASCADE
                        )
                    """.trimIndent())
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val v1Db = helper.writableDatabase

        // Insert V1 data
        v1Db.execSQL("INSERT INTO task_lists (id, name, color_hex) VALUES (1, 'Inbox', '#3B82F6')")
        v1Db.execSQL("INSERT INTO tasks (id, list_id, title, description, is_completed, priority) VALUES (1, 1, 'V1 Task', 'From V1', 0, 'HIGH')")
        v1Db.execSQL("INSERT INTO tasks (id, parent_task_id, title, is_completed) VALUES (2, 1, 'V1 Subtask', 0)")
        v1Db.close()
        helper.close()

        // Now open with Room and apply migrations
        val roomDb = androidx.room.Room.databaseBuilder(context, AppDatabase::class.java, "test_migration_v1_v3.db")
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4)
            .build()

        val tasks = roomDb.taskDao().getAllRootTasks().first()
        assertEquals(1, tasks.size)
        assertEquals("V1 Task", tasks[0].title)
        assertEquals(1L, tasks[0].listId)
        assertNull(tasks[0].projectId)
        assertNull(tasks[0].goalId)

        val subtasks = roomDb.taskDao().getSubtasksDirect(1)
        assertEquals(1, subtasks.size)
        assertEquals("V1 Subtask", subtasks[0].title)

        roomDb.close()
    }

    @Test
    fun testMigrationFromV2ToV3() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbFile = context.getDatabasePath("test_migration_v2_v3.db")
        if (dbFile.exists()) dbFile.delete()

        // Create V2 schema manually
        val config = Configuration.builder(context)
            .name("test_migration_v2_v3.db")
            .callback(object : SupportSQLiteOpenHelper.Callback(2) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS task_lists (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            name TEXT NOT NULL,
                            color_hex TEXT DEFAULT NULL,
                            icon_name TEXT DEFAULT NULL,
                            created_at INTEGER NOT NULL DEFAULT 0,
                            updated_at INTEGER NOT NULL DEFAULT 0,
                            sort_order INTEGER NOT NULL DEFAULT 0
                        )
                    """.trimIndent())

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS tasks (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            list_id INTEGER DEFAULT NULL,
                            parent_task_id INTEGER DEFAULT NULL,
                            title TEXT NOT NULL,
                            description TEXT NOT NULL DEFAULT '',
                            notes TEXT NOT NULL DEFAULT '',
                            is_completed INTEGER NOT NULL DEFAULT 0,
                            priority TEXT NOT NULL DEFAULT 'NONE',
                            created_at INTEGER NOT NULL DEFAULT 0,
                            updated_at INTEGER NOT NULL DEFAULT 0,
                            due_date INTEGER DEFAULT NULL,
                            due_time TEXT DEFAULT NULL,
                            completed_at INTEGER DEFAULT NULL,
                            sort_order INTEGER NOT NULL DEFAULT 0,
                            is_starred INTEGER NOT NULL DEFAULT 0,
                            recurrence_type TEXT DEFAULT NULL,
                            recurrence_interval INTEGER NOT NULL DEFAULT 1,
                            recurrence_days_of_week TEXT DEFAULT NULL,
                            reminder_type TEXT DEFAULT NULL,
                            reminder_time INTEGER DEFAULT NULL,
                            estimated_duration_minutes INTEGER DEFAULT NULL,
                            actual_duration_minutes INTEGER DEFAULT NULL,
                            FOREIGN KEY(list_id) REFERENCES task_lists(id) ON DELETE SET NULL,
                            FOREIGN KEY(parent_task_id) REFERENCES tasks(id) ON DELETE CASCADE
                        )
                    """.trimIndent())
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val v2Db = helper.writableDatabase

        // Insert V2 data with reminders and recurrence
        v2Db.execSQL("INSERT INTO task_lists (id, name, color_hex) VALUES (1, 'Work', '#10B981')")
        v2Db.execSQL("INSERT INTO tasks (id, list_id, title, recurrence_type, reminder_type, reminder_time) VALUES (1, 1, 'Recurring Work Task', 'DAILY', 'AT_TIME', 1700000000000)")
        v2Db.close()
        helper.close()

        // Open with Room and apply migration 2->3->4
        val roomDb = androidx.room.Room.databaseBuilder(context, AppDatabase::class.java, "test_migration_v2_v3.db")
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4)
            .build()

        val tasks = roomDb.taskDao().getAllRootTasks().first()
        assertEquals(1, tasks.size)
        assertEquals("Recurring Work Task", tasks[0].title)
        assertEquals("DAILY", tasks[0].recurrenceType)
        assertEquals("AT_TIME", tasks[0].reminderType)
        assertEquals(1700000000000L, tasks[0].reminderTime)

        // Verify inserting goal, project, habit into migrated DB works
        val goalEntity = com.flowtasks.app.data.local.entity.GoalEntity(title = "Healthy Life")
        val goalId = roomDb.goalDao().insertGoal(goalEntity)
        assertTrue(goalId > 0)

        val projectEntity = com.flowtasks.app.data.local.entity.ProjectEntity(goalId = goalId, title = "Gym Routine")
        val projId = roomDb.projectDao().insertProject(projectEntity)
        assertTrue(projId > 0)

        val habitEntity = com.flowtasks.app.data.local.entity.HabitEntity(title = "Morning Run")
        val habitId = roomDb.habitDao().insertHabit(habitEntity)
        assertTrue(habitId > 0)

        val completionEntity = com.flowtasks.app.data.local.entity.HabitCompletionEntity(habitId = habitId, completedDate = 1700000000000L)
        val compId = roomDb.habitCompletionDao().insertCompletion(completionEntity)
        assertTrue(compId > 0)

        roomDb.close()
    }
}

