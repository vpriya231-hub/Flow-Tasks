package com.flowtasks.app.domain.model

data class TaskList(
    val id: Long = 0,
    val name: String,
    val colorHex: String? = null,
    val iconName: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val sortOrder: Int = 0,
    val taskCount: Int = 0
)
