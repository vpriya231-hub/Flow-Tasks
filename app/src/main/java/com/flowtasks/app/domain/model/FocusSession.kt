package com.flowtasks.app.domain.model

enum class FocusSessionStatus {
    COMPLETED,
    CANCELLED
}

data class FocusSession(
    val id: Long = 0,
    val taskId: Long? = null,
    val taskTitle: String? = null,
    val startedAt: Long = System.currentTimeMillis(),
    val endedAt: Long? = null,
    val durationSeconds: Int = 0,
    val targetDurationMinutes: Int = 25,
    val status: FocusSessionStatus = FocusSessionStatus.COMPLETED,
    val notes: String? = null
) {
    val durationMinutes: Int
        get() = (durationSeconds + 59) / 60
}
