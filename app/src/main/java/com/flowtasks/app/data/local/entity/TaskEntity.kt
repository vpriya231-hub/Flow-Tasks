package com.flowtasks.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.flowtasks.app.domain.model.TaskPriority

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = TaskListEntity::class,
            parentColumns = ["id"],
            childColumns = ["list_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["project_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = GoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goal_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["parent_task_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["list_id"]),
        Index(value = ["project_id"]),
        Index(value = ["goal_id"]),
        Index(value = ["parent_task_id"]),
        Index(value = ["is_completed"]),
        Index(value = ["due_date"]),
        Index(value = ["sort_order"])
    ]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "list_id")
    val listId: Long? = null,

    @ColumnInfo(name = "project_id")
    val projectId: Long? = null,

    @ColumnInfo(name = "goal_id")
    val goalId: Long? = null,

    @ColumnInfo(name = "parent_task_id")
    val parentTaskId: Long? = null,

    val title: String,

    val description: String = "",

    val notes: String = "",

    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false,

    val priority: TaskPriority = TaskPriority.NONE,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "due_date")
    val dueDate: Long? = null,

    @ColumnInfo(name = "due_time")
    val dueTime: String? = null,

    @ColumnInfo(name = "completed_at")
    val completedAt: Long? = null,

    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,

    @ColumnInfo(name = "is_starred")
    val isStarred: Boolean = false,

    @ColumnInfo(name = "recurrence_type")
    val recurrenceType: String? = null,

    @ColumnInfo(name = "recurrence_interval")
    val recurrenceInterval: Int = 1,

    @ColumnInfo(name = "recurrence_days_of_week")
    val recurrenceDaysOfWeek: String? = null,

    @ColumnInfo(name = "reminder_type")
    val reminderType: String? = null,

    @ColumnInfo(name = "reminder_time")
    val reminderTime: Long? = null,

    @ColumnInfo(name = "estimated_duration_minutes")
    val estimatedDurationMinutes: Int? = null,

    @ColumnInfo(name = "actual_duration_minutes")
    val actualDurationMinutes: Int? = null
)
