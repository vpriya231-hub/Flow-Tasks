package com.flowtasks.app.domain.usecase

import com.flowtasks.app.core.datastore.AppThemeMode
import com.flowtasks.app.core.datastore.UserSettings
import com.flowtasks.app.domain.model.TaskPriority
import com.flowtasks.app.domain.model.TaskSortOrder
import com.flowtasks.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

class GetSettingsUseCase(private val repository: SettingsRepository) {
    operator fun invoke(): Flow<UserSettings> = repository.getUserSettings()
}

class UpdateSettingsUseCase(private val repository: SettingsRepository) {
    suspend fun updateTheme(themeMode: AppThemeMode) = repository.updateThemeMode(themeMode)
    suspend fun updateDefaultPriority(priority: TaskPriority) = repository.updateDefaultPriority(priority)
    suspend fun updateDefaultSortOrder(sortOrder: TaskSortOrder) = repository.updateDefaultSortOrder(sortOrder)
    suspend fun updateShowCompleted(show: Boolean) = repository.updateShowCompletedTasks(show)
}
