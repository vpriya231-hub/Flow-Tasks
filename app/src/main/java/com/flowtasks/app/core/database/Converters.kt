package com.flowtasks.app.core.database

import androidx.room.TypeConverter
import com.flowtasks.app.domain.model.TaskPriority

class Converters {
    @TypeConverter
    fun fromTaskPriority(priority: TaskPriority?): String {
        return priority?.name ?: TaskPriority.NONE.name
    }

    @TypeConverter
    fun toTaskPriority(value: String?): TaskPriority {
        return TaskPriority.fromString(value)
    }
}
