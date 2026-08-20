package com.flowtasks.app.domain.model

enum class ProjectStatus {
    ACTIVE,
    COMPLETED,
    ARCHIVED
}

data class Project(
    val id: Long = 0,
    val goalId: Long? = null,
    val goalTitle: String? = null,
    val title: String,
    val description: String = "",
    val targetDate: Long? = null,
    val status: ProjectStatus = ProjectStatus.ACTIVE,
    val colorHex: String = "#0EA5E9",
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val totalTasksCount: Int = 0,
    val completedTasksCount: Int = 0
) {
    val progress: Float
        get() = if (totalTasksCount > 0) (completedTasksCount.toFloat() / totalTasksCount.toFloat()) else 0f
}
