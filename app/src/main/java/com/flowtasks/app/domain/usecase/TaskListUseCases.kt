package com.flowtasks.app.domain.usecase

import com.flowtasks.app.domain.model.TaskList
import com.flowtasks.app.domain.repository.TaskListRepository
import com.flowtasks.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow

enum class DeleteListStrategy {
    MOVE_TO_INBOX,
    DELETE_TASKS
}

class GetTaskListsUseCase(private val repository: TaskListRepository) {
    operator fun invoke(): Flow<List<TaskList>> = repository.getAllLists()
}

class CreateTaskListUseCase(private val repository: TaskListRepository) {
    suspend operator fun invoke(name: String, colorHex: String? = null): Long {
        require(name.isNotBlank()) { "List name cannot be blank" }
        val taskList = TaskList(name = name.trim(), colorHex = colorHex)
        return repository.createList(taskList)
    }
}

class UpdateTaskListUseCase(private val repository: TaskListRepository) {
    suspend operator fun invoke(taskList: TaskList) {
        require(taskList.name.isNotBlank()) { "List name cannot be blank" }
        repository.updateList(taskList.copy(name = taskList.name.trim()))
    }
}

class DeleteTaskListUseCase(
    private val taskListRepository: TaskListRepository,
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(listId: Long, strategy: DeleteListStrategy = DeleteListStrategy.MOVE_TO_INBOX) {
        when (strategy) {
            DeleteListStrategy.MOVE_TO_INBOX -> taskRepository.moveTasksToInbox(listId)
            DeleteListStrategy.DELETE_TASKS -> taskRepository.deleteTasksByListId(listId)
        }
        taskListRepository.deleteList(listId)
    }
}

class ReorderTaskListsUseCase(private val repository: TaskListRepository) {
    suspend operator fun invoke(listIds: List<Long>) {
        repository.reorderLists(listIds)
    }
}
