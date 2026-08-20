package com.flowtasks.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.flowtasks.app.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE parent_task_id IS NULL ORDER BY sort_order ASC, created_at DESC")
    fun getAllRootTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE list_id = :listId AND parent_task_id IS NULL ORDER BY sort_order ASC, created_at DESC")
    fun getRootTasksByList(listId: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE project_id = :projectId AND parent_task_id IS NULL ORDER BY sort_order ASC, created_at DESC")
    fun getRootTasksByProject(projectId: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE goal_id = :goalId AND parent_task_id IS NULL ORDER BY sort_order ASC, created_at DESC")
    fun getRootTasksByGoal(goalId: Long): Flow<List<TaskEntity>>

    @Query("SELECT COUNT(*) FROM tasks WHERE project_id = :projectId AND parent_task_id IS NULL")
    fun getTaskCountByProject(projectId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM tasks WHERE project_id = :projectId AND is_completed = 1 AND parent_task_id IS NULL")
    fun getCompletedTaskCountByProject(projectId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM tasks WHERE goal_id = :goalId AND parent_task_id IS NULL")
    fun getTaskCountByGoal(goalId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM tasks WHERE goal_id = :goalId AND is_completed = 1 AND parent_task_id IS NULL")
    fun getCompletedTaskCountByGoal(goalId: Long): Flow<Int>

    @Query("UPDATE tasks SET project_id = NULL, updated_at = :updatedAt WHERE project_id = :projectId")
    suspend fun unlinkTasksFromProject(projectId: Long, updatedAt: Long)

    @Query("UPDATE tasks SET goal_id = NULL, updated_at = :updatedAt WHERE goal_id = :goalId")
    suspend fun unlinkTasksFromGoal(goalId: Long, updatedAt: Long)

    @Query("SELECT * FROM tasks WHERE parent_task_id = :parentId ORDER BY sort_order ASC, created_at ASC")
    fun getSubtasks(parentId: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE parent_task_id = :parentId")
    suspend fun getSubtasksDirect(parentId: Long): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun getTaskById(id: Long): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskByIdDirect(id: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE is_completed = 0 AND parent_task_id IS NULL ORDER BY due_date ASC, created_at DESC")
    fun getPendingRootTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE is_completed = 1 AND parent_task_id IS NULL ORDER BY completed_at DESC")
    fun getCompletedRootTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE is_starred = 1 AND parent_task_id IS NULL ORDER BY sort_order ASC, created_at DESC")
    fun getStarredRootTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE (title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%') ORDER BY updated_at DESC")
    fun searchTasks(query: String): Flow<List<TaskEntity>>

    @Query("SELECT COUNT(*) FROM tasks WHERE parent_task_id = :parentId")
    suspend fun getSubtaskCount(parentId: Long): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE parent_task_id = :parentId AND is_completed = 1")
    suspend fun getCompletedSubtaskCount(parentId: Long): Int

    @Query("SELECT COUNT(*) FROM tasks")
    fun getTotalTaskCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM tasks WHERE is_completed = 1")
    fun getCompletedTaskCount(): Flow<Int>

    @Query("SELECT * FROM tasks WHERE is_completed = 0 AND (reminder_type != 'NONE' OR reminder_time IS NOT NULL)")
    suspend fun getPendingTasksWithReminders(): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)

    @Query("DELETE FROM tasks WHERE parent_task_id = :parentId")
    suspend fun deleteSubtasksByParentId(parentId: Long)

    @Query("UPDATE tasks SET is_completed = :isCompleted, completed_at = :completedAt, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateCompletionStatus(id: Long, isCompleted: Boolean, completedAt: Long?, updatedAt: Long)

    @Query("UPDATE tasks SET is_starred = :isStarred, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateStarStatus(id: Long, isStarred: Boolean, updatedAt: Long)

    @Query("UPDATE tasks SET sort_order = :sortOrder, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateSortOrder(id: Long, sortOrder: Int, updatedAt: Long)

    @Query("UPDATE tasks SET list_id = NULL, updated_at = :updatedAt WHERE list_id = :listId")
    suspend fun moveTasksToInbox(listId: Long, updatedAt: Long)

    @Query("DELETE FROM tasks WHERE list_id = :listId")
    suspend fun deleteTasksByListId(listId: Long)
}
