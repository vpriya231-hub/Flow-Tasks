package com.flowtasks.app.domain.model

sealed interface TaskFilter {
    data object All : TaskFilter
    data object Today : TaskFilter
    data object Starred : TaskFilter
    data object Overdue : TaskFilter
    data object Completed : TaskFilter
    data class ByList(val listId: Long, val listName: String) : TaskFilter
    data class ByProject(val projectId: Long, val projectTitle: String) : TaskFilter
    data class ByGoal(val goalId: Long, val goalTitle: String) : TaskFilter
}

enum class TaskSortOrder {
    DEFAULT_SORT_ORDER,
    DUE_DATE_ASC,
    PRIORITY_DESC,
    CREATED_AT_DESC,
    TITLE_ASC
}
