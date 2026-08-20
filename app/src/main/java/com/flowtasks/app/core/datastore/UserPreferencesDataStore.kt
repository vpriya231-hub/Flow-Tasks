package com.flowtasks.app.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.flowtasks.app.domain.model.TaskPriority
import com.flowtasks.app.domain.model.TaskSortOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "flow_tasks_preferences")

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

data class UserSettings(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val defaultPriority: TaskPriority = TaskPriority.NONE,
    val defaultSortOrder: TaskSortOrder = TaskSortOrder.DEFAULT_SORT_ORDER,
    val showCompletedTasks: Boolean = true
)

class UserPreferencesDataStore(private val context: Context) {
    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DEFAULT_PRIORITY = stringPreferencesKey("default_priority")
        val DEFAULT_SORT_ORDER = stringPreferencesKey("default_sort_order")
        val SHOW_COMPLETED_TASKS = booleanPreferencesKey("show_completed_tasks")
    }

    val userSettingsFlow: Flow<UserSettings> = context.dataStore.data.map { preferences ->
        val themeModeStr = preferences[PreferencesKeys.THEME_MODE] ?: AppThemeMode.SYSTEM.name
        val themeMode = runCatching { AppThemeMode.valueOf(themeModeStr) }.getOrDefault(AppThemeMode.SYSTEM)

        val priorityStr = preferences[PreferencesKeys.DEFAULT_PRIORITY] ?: TaskPriority.NONE.name
        val defaultPriority = TaskPriority.fromString(priorityStr)

        val sortOrderStr = preferences[PreferencesKeys.DEFAULT_SORT_ORDER] ?: TaskSortOrder.DEFAULT_SORT_ORDER.name
        val defaultSortOrder = runCatching { TaskSortOrder.valueOf(sortOrderStr) }.getOrDefault(TaskSortOrder.DEFAULT_SORT_ORDER)

        val showCompleted = preferences[PreferencesKeys.SHOW_COMPLETED_TASKS] ?: true

        UserSettings(
            themeMode = themeMode,
            defaultPriority = defaultPriority,
            defaultSortOrder = defaultSortOrder,
            showCompletedTasks = showCompleted
        )
    }

    suspend fun updateThemeMode(themeMode: AppThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = themeMode.name
        }
    }

    suspend fun updateDefaultPriority(priority: TaskPriority) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_PRIORITY] = priority.name
        }
    }

    suspend fun updateDefaultSortOrder(sortOrder: TaskSortOrder) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_SORT_ORDER] = sortOrder.name
        }
    }

    suspend fun updateShowCompletedTasks(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_COMPLETED_TASKS] = show
        }
    }
}
