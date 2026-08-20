package com.flowtasks.app.domain.usecase

import com.flowtasks.app.domain.model.FocusSession
import com.flowtasks.app.domain.repository.FocusSessionRepository

class SaveFocusSessionUseCase(
    private val focusSessionRepository: FocusSessionRepository
) {
    suspend operator fun invoke(session: FocusSession): Long {
        return focusSessionRepository.saveSession(session)
    }
}
