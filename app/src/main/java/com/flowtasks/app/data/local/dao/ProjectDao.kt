package com.flowtasks.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.flowtasks.app.data.local.entity.ProjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects WHERE status != 'ARCHIVED' ORDER BY sort_order ASC, created_at DESC")
    fun getAllActiveProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects ORDER BY sort_order ASC, created_at DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE goal_id = :goalId AND status != 'ARCHIVED' ORDER BY sort_order ASC, created_at DESC")
    fun getProjectsByGoal(goalId: Long): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE goal_id = :goalId")
    suspend fun getProjectsByGoalDirect(goalId: Long): List<ProjectEntity>

    @Query("SELECT * FROM projects WHERE id = :id")
    fun getProjectById(id: Long): Flow<ProjectEntity?>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectByIdDirect(id: Long): ProjectEntity?

    @Query("SELECT COUNT(*) FROM projects WHERE goal_id = :goalId AND status != 'ARCHIVED'")
    fun getProjectCountByGoal(goalId: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Delete
    suspend fun deleteProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: Long)

    @Query("UPDATE projects SET goal_id = NULL, updated_at = :updatedAt WHERE goal_id = :goalId")
    suspend fun unlinkProjectsFromGoal(goalId: Long, updatedAt: Long)

    @Query("UPDATE projects SET sort_order = :sortOrder, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateSortOrder(id: Long, sortOrder: Int, updatedAt: Long)

    @Query("UPDATE projects SET status = :status, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateProjectStatus(id: Long, status: String, updatedAt: Long)
}
