package com.flowtasks.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.flowtasks.app.core.database.AppDatabase
import com.flowtasks.app.data.local.dao.TaskDao
import com.flowtasks.app.data.local.dao.TaskListDao
import com.flowtasks.app.data.local.entity.TaskEntity
import com.flowtasks.app.domain.model.TaskPriority
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TaskDatabaseTest {

    private lateinit var database: AppDatabase
    private lateinit var taskDao: TaskDao
    private lateinit var taskListDao: TaskListDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        taskDao = database.taskDao()
        taskListDao = database.taskListDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun database_is_empty_by_default() = runBlocking {
        val allTasks = taskDao.getAllRootTasks().first()
        val allLists = taskListDao.getAllLists().first()
        val totalCount = taskDao.getTotalTaskCount().first()

        assertEquals(0, allTasks.size)
        assertEquals(0, allLists.size)
        assertEquals(0, totalCount)
    }

    @Test
    fun insert_and_retrieve_task() = runBlocking {
        val task = TaskEntity(
            title = "Test Task 1",
            description = "Description 1",
            priority = TaskPriority.HIGH
        )
        val id = taskDao.insertTask(task)

        val retrieved = taskDao.getTaskByIdDirect(id)
        assertNotNull(retrieved)
        assertEquals("Test Task 1", retrieved?.title)
        assertEquals(TaskPriority.HIGH, retrieved?.priority)
        assertEquals(false, retrieved?.isCompleted)
    }

    @Test
    fun toggle_task_completion_and_subtasks() = runBlocking {
        val parentId = taskDao.insertTask(
            TaskEntity(title = "Parent Task")
        )
        val subtaskId = taskDao.insertTask(
            TaskEntity(title = "Subtask 1", parentTaskId = parentId)
        )

        // Toggle subtask completion
        taskDao.updateCompletionStatus(subtaskId, true, System.currentTimeMillis(), System.currentTimeMillis())
        val updatedSubtask = taskDao.getTaskByIdDirect(subtaskId)
        assertEquals(true, updatedSubtask?.isCompleted)

        // Verify parent subtasks
        val subtasks = taskDao.getSubtasksDirect(parentId)
        assertEquals(1, subtasks.size)
        assertEquals(true, subtasks.first().isCompleted)
    }

    @Test
    fun search_tasks_in_database() = runBlocking {
        taskDao.insertTask(TaskEntity(title = "Buy groceries", description = "Milk, Eggs, Bread"))
        taskDao.insertTask(TaskEntity(title = "Schedule dentist", description = "Annual checkup"))

        val searchResults = taskDao.searchTasks("groceries").first()
        assertEquals(1, searchResults.size)
        assertEquals("Buy groceries", searchResults.first().title)
    }
}
