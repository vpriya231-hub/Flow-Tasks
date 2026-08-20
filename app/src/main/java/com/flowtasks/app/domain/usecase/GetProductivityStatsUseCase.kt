package com.flowtasks.app.domain.usecase

import com.flowtasks.app.domain.model.DailyProductivityPoint
import com.flowtasks.app.domain.model.Goal
import com.flowtasks.app.domain.model.GoalStatus
import com.flowtasks.app.domain.model.Habit
import com.flowtasks.app.domain.model.ProductivityStats
import com.flowtasks.app.domain.model.Project
import com.flowtasks.app.domain.model.ProjectStatus
import com.flowtasks.app.domain.model.Task
import com.flowtasks.app.domain.model.TaskFilter
import com.flowtasks.app.domain.repository.FocusSessionRepository
import com.flowtasks.app.domain.repository.GoalRepository
import com.flowtasks.app.domain.repository.HabitRepository
import com.flowtasks.app.domain.repository.ProjectRepository
import com.flowtasks.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.Calendar

class GetProductivityStatsUseCase(
    private val taskRepository: TaskRepository,
    private val focusSessionRepository: FocusSessionRepository,
    private val habitRepository: HabitRepository,
    private val goalRepository: GoalRepository,
    private val projectRepository: ProjectRepository
) {
    operator fun invoke(): Flow<ProductivityStats> {
        val tasksAndSessions = combine(
            taskRepository.getTasks(TaskFilter.All),
            focusSessionRepository.getAllCompletedSessions()
        ) { tasks, sessions -> Pair(tasks, sessions) }

        val habitsGoalsProjects = combine(
            habitRepository.getHabits(includeArchived = false),
            goalRepository.getGoals(includeArchived = false),
            projectRepository.getProjects(includeArchived = false)
        ) { habits, goals, projects -> Triple(habits, goals, projects) }

        return combine(
            tasksAndSessions,
            habitsGoalsProjects
        ) { (allTasks, completedSessions), (habits, goals, projects) ->
            calculateStats(allTasks, completedSessions, habits, goals, projects)
        }
    }

    private fun calculateStats(
        allTasks: List<Task>,
        completedSessions: List<com.flowtasks.app.domain.model.FocusSession>,
        habits: List<Habit>,
        goals: List<Goal>,
        projects: List<Project>
    ): ProductivityStats {
        val now = System.currentTimeMillis()
        val (todayStart, todayEnd) = getTodayBounds()
        val (weekStart, weekEnd) = getWeekBounds()

        // Today metrics
        val tasksDueToday = allTasks.filter { task ->
            task.dueDate != null && task.dueDate in todayStart..todayEnd
        }
        val completedToday = allTasks.count { task ->
            task.completedAt != null && task.completedAt in todayStart..todayEnd
        }
        val remainingToday = tasksDueToday.count { !it.isCompleted }
        val overdueCount = allTasks.count { it.isOverdue(now) }

        // Week metrics
        val completedThisWeek = allTasks.count { task ->
            task.completedAt != null && task.completedAt in weekStart..weekEnd
        }
        val dueThisWeek = allTasks.count { task ->
            task.dueDate != null && task.dueDate in weekStart..weekEnd
        }
        val totalTasksThisWeek = maxOf(dueThisWeek, completedThisWeek)
        val completionRate = if (totalTasksThisWeek > 0) {
            ((completedThisWeek.toDouble() / totalTasksThisWeek.toDouble()) * 100).toInt()
        } else {
            if (completedThisWeek > 0) 100 else 0
        }

        // Focus time metrics
        val sessionsThisWeek = completedSessions.filter { session ->
            session.startedAt in weekStart..weekEnd
        }
        val focusSecondsThisWeek = sessionsThisWeek.sumOf { it.durationSeconds }
        val focusSecondsAllTime = completedSessions.sumOf { it.durationSeconds }

        // Habits metrics
        val habitsCompletedToday = habits.count { it.isCompletedToday }
        val longestStreak = habits.maxOfOrNull { it.bestStreak } ?: 0

        // Goals & Projects metrics
        val activeGoals = goals.count { it.status == GoalStatus.ACTIVE }
        val completedGoals = goals.count { it.status == GoalStatus.COMPLETED }
        val totalGoals = goals.size
        val goalsRate = if (totalGoals > 0) ((completedGoals.toDouble() / totalGoals.toDouble()) * 100).toInt() else 0

        val activeProjects = projects.count { it.status != ProjectStatus.COMPLETED }
        val completedProjects = projects.count { it.status == ProjectStatus.COMPLETED }
        val totalProjects = projects.size
        val projectsRate = if (totalProjects > 0) ((completedProjects.toDouble() / totalProjects.toDouble()) * 100).toInt() else 0

        // 7-day trend (Monday to Sunday)
        val weeklyTrend = calculateWeeklyTrend(allTasks, completedSessions)

        // Check if any real activity exists
        val hasAnyData = allTasks.isNotEmpty() || completedSessions.isNotEmpty() || habits.isNotEmpty() || goals.isNotEmpty() || projects.isNotEmpty()

        return ProductivityStats(
            tasksDueToday = tasksDueToday.size,
            completedToday = completedToday,
            remainingToday = remainingToday,
            overdueTasksCount = overdueCount,
            completedThisWeek = completedThisWeek,
            totalTasksThisWeek = totalTasksThisWeek,
            completionRatePercentage = completionRate.coerceIn(0, 100),
            totalFocusSecondsThisWeek = focusSecondsThisWeek,
            totalFocusSecondsAllTime = focusSecondsAllTime,
            totalFocusSessionsCompleted = completedSessions.size,
            totalTasksCount = allTasks.size,
            totalCompletedTasksCount = allTasks.count { it.isCompleted },
            totalHabitsCount = habits.size,
            habitsCompletedTodayCount = habitsCompletedToday,
            longestHabitStreak = longestStreak,
            activeGoalsCount = activeGoals,
            completedGoalsCount = completedGoals,
            totalGoalsCount = totalGoals,
            goalsCompletionRatePercentage = goalsRate,
            activeProjectsCount = activeProjects,
            completedProjectsCount = completedProjects,
            totalProjectsCount = totalProjects,
            projectsCompletionRatePercentage = projectsRate,
            weeklyTrend = weeklyTrend,
            hasAnyData = hasAnyData
        )
    }

    private fun calculateWeeklyTrend(
        allTasks: List<Task>,
        sessions: List<com.flowtasks.app.domain.model.FocusSession>
    ): List<DailyProductivityPoint> {
        val trend = mutableListOf<DailyProductivityPoint>()
        val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

        val calendar = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val todayCal = Calendar.getInstance()
        val todayYear = todayCal.get(Calendar.YEAR)
        val todayDayOfYear = todayCal.get(Calendar.DAY_OF_YEAR)

        for (i in 0 until 7) {
            val dayStart = calendar.timeInMillis
            val isToday = (calendar.get(Calendar.YEAR) == todayYear && calendar.get(Calendar.DAY_OF_YEAR) == todayDayOfYear)

            calendar.apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            val dayEnd = calendar.timeInMillis

            val completedOnDay = allTasks.count { task ->
                task.completedAt != null && task.completedAt in dayStart..dayEnd
            }

            val focusSecondsOnDay = sessions.filter { session ->
                session.startedAt in dayStart..dayEnd
            }.sumOf { it.durationSeconds }

            trend.add(
                DailyProductivityPoint(
                    dayOfWeek = i + 1,
                    dayLabel = dayLabels[i],
                    dateMillis = dayStart,
                    tasksCompleted = completedOnDay,
                    focusMinutes = (focusSecondsOnDay + 59) / 60,
                    isToday = isToday
                )
            )

            // Advance to next day
            calendar.timeInMillis = dayStart
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        return trend
    }

    private fun getTodayBounds(): Pair<Long, Long> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis
        calendar.apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val endOfDay = calendar.timeInMillis
        return Pair(startOfDay, endOfDay)
    }

    private fun getWeekBounds(): Pair<Long, Long> {
        val calendar = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfWeek = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, 6)
        calendar.apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val endOfWeek = calendar.timeInMillis
        return Pair(startOfWeek, endOfWeek)
    }
}
