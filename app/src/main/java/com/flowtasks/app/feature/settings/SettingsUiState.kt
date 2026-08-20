package com.flowtasks.app.feature.settings

import com.flowtasks.app.core.datastore.AppThemeMode
import com.flowtasks.app.core.datastore.UserSettings
import com.flowtasks.app.domain.ai.AIConfig
import com.flowtasks.app.domain.model.TaskPriority
import com.flowtasks.app.domain.model.TaskSortOrder

data class SettingsUiState(
    val settings: UserSettings = UserSettings(),
    val totalTaskCount: Int = 0,
    val completedTaskCount: Int = 0,
    val aiConfig: AIConfig = AIConfig(),
    val hasAIKey: Boolean = false,
    val maskedApiKey: String? = null,
    val isTestingConnection: Boolean = false,
    val testConnectionMessage: String? = null,
    val isTestConnectionSuccess: Boolean? = null,
    val isLoading: Boolean = true
) {
    val completionRate: Int
        get() = if (totalTaskCount > 0) (completedTaskCount * 100) / totalTaskCount else 0

    val pendingTaskCount: Int
        get() = (totalTaskCount - completedTaskCount).coerceAtLeast(0)
}
