package com.flowtasks.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.flowtasks.app.core.database.AppDatabase
import com.flowtasks.app.data.local.dao.TaskDao
import com.flowtasks.app.data.local.dao.TaskListDao
import com.flowtasks.app.data.repository.TaskListRepositoryImpl
import com.flowtasks.app.data.repository.TaskRepositoryImpl
import com.flowtasks.app.domain.model.RecurrenceRule
import com.flowtasks.app.domain.model.RecurrenceType
import com.flowtasks.app.domain.model.ReminderType
import com.flowtasks.app.domain.model.Task
import com.flowtasks.app.domain.model.TaskFilter
import com.flowtasks.app.domain.model.TaskPriority
import com.flowtasks.app.domain.model.TaskSortOrder
import com.flowtasks.app.domain.usecase.CreateTaskListUseCase
import com.flowtasks.app.domain.usecase.CreateTaskUseCase
import com.flowtasks.app.domain.usecase.DeleteListStrategy
import com.flowtasks.app.domain.usecase.DeleteTaskListUseCase
import com.flowtasks.app.domain.usecase.DeleteTaskUseCase
import com.flowtasks.app.domain.usecase.GetSubtasksUseCase
import com.flowtasks.app.domain.usecase.GetTaskByIdUseCase
import com.flowtasks.app.domain.usecase.GetTaskListsUseCase
import com.flowtasks.app.domain.usecase.GetTasksUseCase
import com.flowtasks.app.domain.usecase.ReorderTaskListsUseCase
import com.flowtasks.app.domain.usecase.ReorderTasksUseCase
import com.flowtasks.app.domain.usecase.ToggleTaskCompletionUseCase
import com.flowtasks.app.domain.usecase.ToggleTaskStarUseCase
import com.flowtasks.app.domain.usecase.UpdateTaskUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FlowTasksFunctionalTest {

    private lateinit var database: AppDatabase
    private lateinit var taskDao: TaskDao
    private lateinit var taskListDao: TaskListDao

    private lateinit var taskRepository: TaskRepositoryImpl
    private lateinit var taskListRepository: TaskListRepositoryImpl

    private lateinit var getTasksUseCase: GetTasksUseCase
    private lateinit var createTaskUseCase: CreateTaskUseCase
    private lateinit var updateTaskUseCase: UpdateTaskUseCase
    private lateinit var deleteTaskUseCase: DeleteTaskUseCase
    private lateinit var toggleTaskCompletionUseCase: ToggleTaskCompletionUseCase
    private lateinit var toggleTaskStarUseCase: ToggleTaskStarUseCase
    private lateinit var getTaskByIdUseCase: GetTaskByIdUseCase
    private lateinit var getSubtasksUseCase: GetSubtasksUseCase
    private lateinit var getTaskListsUseCase: GetTaskListsUseCase
    private lateinit var createTaskListUseCase: CreateTaskListUseCase
    private lateinit var deleteTaskListUseCase: DeleteTaskListUseCase
    private lateinit var reorderTasksUseCase: ReorderTasksUseCase
    private lateinit var reorderTaskListsUseCase: ReorderTaskListsUseCase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        taskDao = database.taskDao()
        taskListDao = database.taskListDao()

        val dispatchers = com.flowtasks.app.core.common.DefaultDispatcherProvider()
        val reminderScheduler = com.flowtasks.app.core.notification.TaskReminderScheduler(context)
        taskRepository = TaskRepositoryImpl(taskDao, dispatchers)
        taskListRepository = TaskListRepositoryImpl(taskListDao, dispatchers)

        getTasksUseCase = GetTasksUseCase(taskRepository)
        createTaskUseCase = CreateTaskUseCase(taskRepository, reminderScheduler)
        updateTaskUseCase = UpdateTaskUseCase(taskRepository, reminderScheduler)
        deleteTaskUseCase = DeleteTaskUseCase(taskRepository, reminderScheduler)
        toggleTaskCompletionUseCase = ToggleTaskCompletionUseCase(taskRepository, reminderScheduler)
        toggleTaskStarUseCase = ToggleTaskStarUseCase(taskRepository)
        getTaskByIdUseCase = GetTaskByIdUseCase(taskRepository)
        getSubtasksUseCase = GetSubtasksUseCase(taskRepository)
        getTaskListsUseCase = GetTaskListsUseCase(taskListRepository)
        createTaskListUseCase = CreateTaskListUseCase(taskListRepository)
        deleteTaskListUseCase = DeleteTaskListUseCase(taskListRepository, taskRepository)
        reorderTasksUseCase = ReorderTasksUseCase(taskRepository)
        reorderTaskListsUseCase = ReorderTaskListsUseCase(taskListRepository)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun test1_initialLaunch_databaseIsEmpty() = runBlocking {
        val tasks = getTasksUseCase(TaskFilter.All, TaskSortOrder.DEFAULT_SORT_ORDER).first()
        val lists = getTaskListsUseCase().first()

        assertEquals(0, tasks.size)
        assertEquals(0, lists.size)
    }

    @Test
    fun test_taskReminder_triggerTimeCalculation() = runBlocking {
        val futureDate = System.currentTimeMillis() + 86400000L
        val taskId = createTaskUseCase(
            Task(
                title = "Reminder Task",
                dueDate = futureDate,
                dueTime = "14:00",
                reminderType = ReminderType.AT_TIME
            )
        )
        val task = getTaskByIdUseCase(taskId).first()
        assertNotNull(task)
        assertEquals(ReminderType.AT_TIME, task?.reminderType)
        val triggerTime = task?.calculateReminderTriggerTime()
        assertNotNull(triggerTime)
        assertTrue(triggerTime!! > System.currentTimeMillis())
    }

    @Test
    fun test2_and_3_createTask_persistsToRoom() = runBlocking {
        val taskId = createTaskUseCase(
            Task(
                title = "Test Task",
                description = "Test Description",
                priority = TaskPriority.HIGH
            )
        )

        assertTrue(taskId > 0)

        val tasks = getTasksUseCase(TaskFilter.All, TaskSortOrder.DEFAULT_SORT_ORDER).first()
        assertEquals(1, tasks.size)
        assertEquals("Test Task", tasks.first().title)
        assertEquals(false, tasks.first().isCompleted)
        assertEquals(TaskPriority.HIGH, tasks.first().priority)
    }

    @Test
    fun test4_and_5_completeTask_updatesRoom() = runBlocking {
        val taskId = createTaskUseCase(
            Task(title = "Task To Complete")
        )

        // Mark as completed
        toggleTaskCompletionUseCase(taskId, true)

        val task = getTaskByIdUseCase(taskId).first()
        assertNotNull(task)
        assertEquals(true, task?.isCompleted)
        assertNotNull(task?.completedAt)

        // Toggle back to incomplete
        toggleTaskCompletionUseCase(taskId, false)
        val taskIncomplete = getTaskByIdUseCase(taskId).first()
        assertEquals(false, taskIncomplete?.isCompleted)
        assertNull(taskIncomplete?.completedAt)
    }

    @Test
    fun test6_and_7_editTask_persistsChanges() = runBlocking {
        val taskId = createTaskUseCase(
            Task(title = "Original Title", description = "Original Desc")
        )

        updateTaskUseCase(
            Task(
                id = taskId,
                title = "Updated Title",
                description = "Updated Desc",
                priority = TaskPriority.MEDIUM
            )
        )

        val updated = getTaskByIdUseCase(taskId).first()
        assertNotNull(updated)
        assertEquals("Updated Title", updated?.title)
        assertEquals("Updated Desc", updated?.description)
        assertEquals(TaskPriority.MEDIUM, updated?.priority)
    }

    @Test
    fun test8_and_9_deleteTask_removesFromRoom() = runBlocking {
        val taskId = createTaskUseCase(
            Task(title = "Task To Delete")
        )

        val beforeDelete = getTasksUseCase(TaskFilter.All, TaskSortOrder.DEFAULT_SORT_ORDER).first()
        assertEquals(1, beforeDelete.size)

        deleteTaskUseCase(taskId)

        val afterDelete = getTasksUseCase(TaskFilter.All, TaskSortOrder.DEFAULT_SORT_ORDER).first()
        assertEquals(0, afterDelete.size)
    }

    @Test
    fun test10_starredFilter_displaysOnlyStarredTasks() = runBlocking {
        val task1 = createTaskUseCase(Task(title = "Normal Task", isStarred = false))
        val task2 = createTaskUseCase(Task(title = "Starred Task", isStarred = true))

        val starredTasks = getTasksUseCase(TaskFilter.Starred, TaskSortOrder.DEFAULT_SORT_ORDER).first()
        assertEquals(1, starredTasks.size)
        assertEquals("Starred Task", starredTasks.first().title)

        // Star the normal task
        toggleTaskStarUseCase(task1, true)
        val allStarred = getTasksUseCase(TaskFilter.Starred, TaskSortOrder.DEFAULT_SORT_ORDER).first()
        assertEquals(2, allStarred.size)
    }

    @Test
    fun test11_customLists_manageAndFilter() = runBlocking {
        val listId = createTaskListUseCase("Work", "#3B82F6")
        assertTrue(listId > 0)

        val lists = getTaskListsUseCase().first()
        assertEquals(1, lists.size)
        assertEquals("Work", lists.first().name)

        createTaskUseCase(Task(title = "Work Task 1", listId = listId))
        createTaskUseCase(Task(title = "Personal Task 1", listId = null))

        val workTasks = getTasksUseCase(TaskFilter.ByList(listId, "Work"), TaskSortOrder.DEFAULT_SORT_ORDER).first()
        assertEquals(1, workTasks.size)
        assertEquals("Work Task 1", workTasks.first().title)
    }

    @Test
    fun test12_subtasks_hierarchyAndCounts() = runBlocking {
        val parentId = createTaskUseCase(Task(title = "Parent Task"))
        val subtask1Id = createTaskUseCase(Task(title = "Subtask 1", parentTaskId = parentId))
        val subtask2Id = createTaskUseCase(Task(title = "Subtask 2", parentTaskId = parentId))

        val subtasks = getSubtasksUseCase(parentId).first()
        assertEquals(2, subtasks.size)

        val parentTask = getTaskByIdUseCase(parentId).first()
        assertNotNull(parentTask)
        assertEquals(2, parentTask?.subtaskCount)
        assertEquals(0, parentTask?.completedSubtaskCount)

        // Complete subtask 1
        toggleTaskCompletionUseCase(subtask1Id, true)

        val updatedParent = getTaskByIdUseCase(parentId).first()
        assertEquals(1, updatedParent?.completedSubtaskCount)
    }

    @Test
    fun test13_recurringTask_completionRollsOverToNextDueDate() = runBlocking {
        val calendar = Calendar.getInstance()
        val originalDueDate = calendar.timeInMillis

        val taskId = createTaskUseCase(
            Task(
                title = "Daily Workout",
                dueDate = originalDueDate,
                recurrence = RecurrenceRule(type = RecurrenceType.DAILY, interval = 1)
            )
        )

        // Complete recurring task -> should advance due date and stay incomplete
        toggleTaskCompletionUseCase(taskId, true)

        val taskAfter = getTaskByIdUseCase(taskId).first()
        assertNotNull(taskAfter)
        assertFalse(taskAfter!!.isCompleted) // stays active for next cycle
        assertTrue(taskAfter.dueDate!! > originalDueDate)
    }

    @Test
    fun test14_reorderTasks_persistsSortOrder() = runBlocking {
        val id1 = createTaskUseCase(Task(title = "Task 1", sortOrder = 0))
        val id2 = createTaskUseCase(Task(title = "Task 2", sortOrder = 1))
        val id3 = createTaskUseCase(Task(title = "Task 3", sortOrder = 2))

        // Swap order: id3, id1, id2
        reorderTasksUseCase(listOf(id3, id1, id2))

        val tasks = getTasksUseCase(TaskFilter.All, TaskSortOrder.DEFAULT_SORT_ORDER).first()
        assertEquals(3, tasks.size)
        assertEquals(id3, tasks[0].id)
        assertEquals(id1, tasks[1].id)
        assertEquals(id2, tasks[2].id)
    }

    @Test
    fun test15_deleteListStrategy_moveToInbox() = runBlocking {
        val listId = createTaskListUseCase("Projects")
        val taskId = createTaskUseCase(Task(title = "Project Item", listId = listId))

        deleteTaskListUseCase(listId, DeleteListStrategy.MOVE_TO_INBOX)

        val task = getTaskByIdUseCase(taskId).first()
        assertNotNull(task)
        assertNull(task?.listId) // Moved to inbox (null listId)
    }

    @Test
    fun test16_deleteListStrategy_deleteTasks() = runBlocking {
        val listId = createTaskListUseCase("Temporary")
        val taskId = createTaskUseCase(Task(title = "Temp Item", listId = listId))

        deleteTaskListUseCase(listId, DeleteListStrategy.DELETE_TASKS)

        val task = getTaskByIdUseCase(taskId).first()
        assertNull(task) // Deleted with list
    }
}
