package com.flowtasks.app.domain.repository

import com.flowtasks.app.core.datastore.AppThemeMode
import com.flowtasks.app.core.datastore.UserSettings
import com.flowtasks.app.domain.model.TaskPriority
import com.flowtasks.app.domain.model.TaskSortOrder
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getUserSettings(): Flow<UserSettings>
    suspend fun updateThemeMode(themeMode: AppThemeMode)
    suspend fun updateDefaultPriority(priority: TaskPriority)
    suspend fun updateDefaultSortOrder(sortOrder: TaskSortOrder)
    suspend fun updateShowCompletedTasks(show: Boolean)
}
