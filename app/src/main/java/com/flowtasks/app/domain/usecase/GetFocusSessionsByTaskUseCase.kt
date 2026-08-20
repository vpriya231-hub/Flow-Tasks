package com.flowtasks.app.domain.usecase

import com.flowtasks.app.domain.model.FocusSession
import com.flowtasks.app.domain.repository.FocusSessionRepository
import kotlinx.coroutines.flow.Flow

class GetFocusSessionsByTaskUseCase(
    private val focusSessionRepository: FocusSessionRepository
) {
    operator fun invoke(taskId: Long): Flow<List<FocusSession>> {
        return focusSessionRepository.getCompletedSessionsByTask(taskId)
    }
}
