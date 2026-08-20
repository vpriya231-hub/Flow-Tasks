package com.flowtasks.app.domain.model

enum class GoalStatus {
    ACTIVE,
    COMPLETED,
    ARCHIVED
}

data class Goal(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val targetDate: Long? = null,
    val status: GoalStatus = GoalStatus.ACTIVE,
    val colorHex: String = "#4F46E5",
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val projectCount: Int = 0,
    val totalTasksCount: Int = 0,
    val completedTasksCount: Int = 0
) {
    val progress: Float
        get() = if (totalTasksCount > 0) (completedTasksCount.toFloat() / totalTasksCount.toFloat()) else 0f
}
