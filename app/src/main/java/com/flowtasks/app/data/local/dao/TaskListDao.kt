package com.flowtasks.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.flowtasks.app.data.local.entity.TaskListEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskListDao {
    @Query("SELECT * FROM task_lists ORDER BY sort_order ASC, created_at ASC")
    fun getAllLists(): Flow<List<TaskListEntity>>

    @Query("SELECT * FROM task_lists WHERE id = :id")
    fun getListById(id: Long): Flow<TaskListEntity?>

    @Query("SELECT * FROM task_lists WHERE id = :id")
    suspend fun getListByIdDirect(id: Long): TaskListEntity?

    @Query("SELECT COUNT(*) FROM tasks WHERE list_id = :listId AND parent_task_id IS NULL")
    fun getTaskCountForList(listId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM tasks WHERE list_id = :listId AND parent_task_id IS NULL")
    suspend fun getTaskCountForListDirect(listId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertList(taskList: TaskListEntity): Long

    @Update
    suspend fun updateList(taskList: TaskListEntity)

    @Delete
    suspend fun deleteList(taskList: TaskListEntity)

    @Query("DELETE FROM task_lists WHERE id = :id")
    suspend fun deleteListById(id: Long)

    @Query("UPDATE task_lists SET sort_order = :sortOrder, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateSortOrder(id: Long, sortOrder: Int, updatedAt: Long)
}
