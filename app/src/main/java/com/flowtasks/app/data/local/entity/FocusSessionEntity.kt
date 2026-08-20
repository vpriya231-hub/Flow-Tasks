package com.flowtasks.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "focus_sessions",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["task_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["task_id"]),
        Index(value = ["started_at"]),
        Index(value = ["status"])
    ]
)
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "task_id")
    val taskId: Long? = null,
    @ColumnInfo(name = "started_at")
    val startedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "ended_at")
    val endedAt: Long? = null,
    @ColumnInfo(name = "duration_seconds")
    val durationSeconds: Int = 0,
    @ColumnInfo(name = "target_duration_minutes")
    val targetDurationMinutes: Int = 25,
    @ColumnInfo(name = "status")
    val status: String = "COMPLETED", // COMPLETED, CANCELLED
    @ColumnInfo(name = "notes")
    val notes: String? = null
)
