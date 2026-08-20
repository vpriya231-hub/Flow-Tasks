package com.flowtasks.app.data.repository

import com.flowtasks.app.core.datastore.AppThemeMode
import com.flowtasks.app.core.datastore.UserPreferencesDataStore
import com.flowtasks.app.core.datastore.UserSettings
import com.flowtasks.app.domain.model.TaskPriority
import com.flowtasks.app.domain.model.TaskSortOrder
import com.flowtasks.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

class SettingsRepositoryImpl(
    private val preferencesDataStore: UserPreferencesDataStore
) : SettingsRepository {

    override fun getUserSettings(): Flow<UserSettings> = preferencesDataStore.userSettingsFlow

    override suspend fun updateThemeMode(themeMode: AppThemeMode) {
        preferencesDataStore.updateThemeMode(themeMode)
    }

    override suspend fun updateDefaultPriority(priority: TaskPriority) {
        preferencesDataStore.updateDefaultPriority(priority)
    }

    override suspend fun updateDefaultSortOrder(sortOrder: TaskSortOrder) {
        preferencesDataStore.updateDefaultSortOrder(sortOrder)
    }

    override suspend fun updateShowCompletedTasks(show: Boolean) {
        preferencesDataStore.updateShowCompletedTasks(show)
    }
}
