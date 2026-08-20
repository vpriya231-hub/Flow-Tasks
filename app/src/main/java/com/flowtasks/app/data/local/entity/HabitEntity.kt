package com.flowtasks.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "habits",
    indices = [
        Index(value = ["status"]),
        Index(value = ["sort_order"])
    ]
)
data class HabitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val title: String,

    val description: String = "",

    @ColumnInfo(name = "frequency_type")
    val frequencyType: String = "DAILY",

    @ColumnInfo(name = "frequency_days")
    val frequencyDays: String? = null, // e.g. "1,3,5"

    @ColumnInfo(name = "target_count_per_period")
    val targetCountPerPeriod: Int = 1,

    val status: String = "ACTIVE",

    @ColumnInfo(name = "color_hex")
    val colorHex: String = "#10B981",

    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
