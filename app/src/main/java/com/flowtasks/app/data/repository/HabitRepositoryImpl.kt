package com.flowtasks.app.data.repository

import com.flowtasks.app.core.common.DispatcherProvider
import com.flowtasks.app.core.utils.HabitCalculator
import com.flowtasks.app.data.local.dao.HabitCompletionDao
import com.flowtasks.app.data.local.dao.HabitDao
import com.flowtasks.app.data.local.entity.HabitCompletionEntity
import com.flowtasks.app.data.mapper.HabitMapper
import com.flowtasks.app.domain.model.Habit
import com.flowtasks.app.domain.model.HabitCompletion
import com.flowtasks.app.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class HabitRepositoryImpl(
    private val habitDao: HabitDao,
    private val habitCompletionDao: HabitCompletionDao,
    private val dispatchers: DispatcherProvider
) : HabitRepository {

    override fun getHabits(includeArchived: Boolean): Flow<List<Habit>> {
        val habitsFlow = if (includeArchived) habitDao.getAllHabits() else habitDao.getActiveHabits()
        val completionsFlow = habitCompletionDao.getAllCompletionsFlow()

        return combine(habitsFlow, completionsFlow) { entities, completions ->
            val completionsByHabit = completions.groupBy { it.habitId }
            entities.map { entity ->
                val habitCompletions = completionsByHabit[entity.id] ?: emptyList()
                val metrics = HabitCalculator.calculateMetrics(
                    habit = entity,
                    completions = habitCompletions
                )
                HabitMapper.toDomain(entity, metrics)
            }
        }.flowOn(dispatchers.io)
    }

    override fun getHabitById(id: Long): Flow<Habit?> {
        val habitFlow = habitDao.getHabitById(id)
        val completionsFlow = habitCompletionDao.getCompletionsByHabit(id)

        return combine(habitFlow, completionsFlow) { entity, completions ->
            if (entity == null) return@combine null
            val metrics = HabitCalculator.calculateMetrics(
                habit = entity,
                completions = completions
            )
            HabitMapper.toDomain(entity, metrics)
        }.flowOn(dispatchers.io)
    }

    override suspend fun getHabitByIdDirect(id: Long): Habit? = withContext(dispatchers.io) {
        val entity = habitDao.getHabitByIdDirect(id) ?: return@withContext null
        val completions = habitCompletionDao.getCompletionsByHabitDirect(entity.id)
        val metrics = HabitCalculator.calculateMetrics(
            habit = entity,
            completions = completions
        )
        HabitMapper.toDomain(entity, metrics)
    }

    override fun getCompletionsByHabit(habitId: Long): Flow<List<HabitCompletion>> {
        return habitCompletionDao.getCompletionsByHabit(habitId).map { entities ->
            entities.map { HabitMapper.toDomain(it) }
        }.flowOn(dispatchers.io)
    }

    override suspend fun createHabit(habit: Habit): Long = withContext(dispatchers.io) {
        val entity = HabitMapper.toEntity(habit).copy(
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        habitDao.insertHabit(entity)
    }

    override suspend fun updateHabit(habit: Habit) = withContext(dispatchers.io) {
        val entity = HabitMapper.toEntity(habit).copy(
            updatedAt = System.currentTimeMillis()
        )
        habitDao.updateHabit(entity)
    }

    override suspend fun deleteHabit(id: Long) = withContext(dispatchers.io) {
        habitCompletionDao.deleteCompletionsByHabitId(id)
        habitDao.deleteHabitById(id)
    }

    override suspend fun toggleHabitCompletionToday(habitId: Long): Unit = withContext(dispatchers.io) {
        val todayStart = HabitCalculator.normalizeToStartOfDay()
        val existing = habitCompletionDao.getCompletionForDate(habitId, todayStart)
        if (existing != null) {
            habitCompletionDao.deleteCompletionByDate(habitId, todayStart)
        } else {
            habitCompletionDao.insertCompletion(
                HabitCompletionEntity(
                    habitId = habitId,
                    completedDate = todayStart,
                    completedAt = System.currentTimeMillis()
                )
            )
        }
        Unit
    }

    override suspend fun toggleHabitCompletionForDate(habitId: Long, dateMillis: Long): Unit = withContext(dispatchers.io) {
        val targetDate = HabitCalculator.normalizeToStartOfDay(dateMillis)
        val existing = habitCompletionDao.getCompletionForDate(habitId, targetDate)
        if (existing != null) {
            habitCompletionDao.deleteCompletionByDate(habitId, targetDate)
        } else {
            habitCompletionDao.insertCompletion(
                HabitCompletionEntity(
                    habitId = habitId,
                    completedDate = targetDate,
                    completedAt = System.currentTimeMillis()
                )
            )
        }
        Unit
    }

    override suspend fun updateSortOrder(id: Long, sortOrder: Int) = withContext(dispatchers.io) {
        habitDao.updateSortOrder(id, sortOrder, System.currentTimeMillis())
    }
}
