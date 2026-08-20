package com.flowtasks.app.feature.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.flowtasks.app.domain.model.FocusSession
import com.flowtasks.app.domain.model.FocusSessionStatus
import com.flowtasks.app.domain.usecase.GetTaskByIdUseCase
import com.flowtasks.app.domain.usecase.SaveFocusSessionUseCase
import com.flowtasks.app.domain.usecase.ToggleTaskCompletionUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FocusUiState(
    val taskId: Long? = null,
    val taskTitle: String? = null,
    val targetDurationMinutes: Int = 25,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val isFinished: Boolean = false,
    val elapsedSeconds: Int = 0,
    val remainingSeconds: Int = 25 * 60,
    val showExitConfirmDialog: Boolean = false,
    val showCompleteTaskPrompt: Boolean = false
) {
    val progress: Float
        get() {
            val totalSeconds = targetDurationMinutes * 60
            return if (totalSeconds > 0) {
                (elapsedSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
            } else 0f
        }

    val formattedRemainingTime: String
        get() {
            val mins = remainingSeconds / 60
            val secs = remainingSeconds % 60
            return String.format("%02d:%02d", mins, secs)
        }

    val formattedElapsedTime: String
        get() {
            val mins = elapsedSeconds / 60
            val secs = elapsedSeconds % 60
            return String.format("%02d:%02d", mins, secs)
        }
}

class FocusViewModel(
    private val taskId: Long?,
    private val taskTitle: String? = null,
    private val initialDurationMinutes: Int?,
    private val getTaskByIdUseCase: GetTaskByIdUseCase,
    private val saveFocusSessionUseCase: SaveFocusSessionUseCase,
    private val toggleTaskCompletionUseCase: ToggleTaskCompletionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        FocusUiState(
            taskId = taskId,
            taskTitle = taskTitle,
            targetDurationMinutes = (initialDurationMinutes ?: 25).coerceAtLeast(1),
            remainingSeconds = ((initialDurationMinutes ?: 25).coerceAtLeast(1)) * 60
        )
    )
    val uiState: StateFlow<FocusUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var sessionStartTimeMillis: Long = 0L
    private var accumulatedSeconds: Int = 0
    private var segmentStartTimeMillis: Long = 0L

    init {
        if (taskId != null) {
            viewModelScope.launch {
                val task = getTaskByIdUseCase.getDirect(taskId)
                if (task != null) {
                    val duration = if (initialDurationMinutes != null && initialDurationMinutes > 0) {
                        initialDurationMinutes
                    } else if (task.estimatedDurationMinutes != null && task.estimatedDurationMinutes > 0) {
                        task.estimatedDurationMinutes
                    } else {
                        25
                    }
                    _uiState.update {
                        it.copy(
                            taskTitle = task.title,
                            targetDurationMinutes = duration,
                            remainingSeconds = duration * 60
                        )
                    }
                }
            }
        }
    }

    fun setTargetDuration(minutes: Int) {
        if (_uiState.value.isRunning || _uiState.value.isPaused) return
        val validMins = minutes.coerceIn(1, 180)
        _uiState.update {
            it.copy(
                targetDurationMinutes = validMins,
                remainingSeconds = validMins * 60
            )
        }
    }

    fun startTimer() {
        if (_uiState.value.isRunning) return

        sessionStartTimeMillis = System.currentTimeMillis()
        segmentStartTimeMillis = System.currentTimeMillis()
        accumulatedSeconds = 0

        _uiState.update {
            it.copy(
                isRunning = true,
                isPaused = false,
                isFinished = false,
                elapsedSeconds = 0,
                remainingSeconds = it.targetDurationMinutes * 60
            )
        }

        startTicker()
    }

    fun pauseTimer() {
        if (!_uiState.value.isRunning || _uiState.value.isPaused) return

        val now = System.currentTimeMillis()
        accumulatedSeconds += ((now - segmentStartTimeMillis) / 1000).toInt()
        timerJob?.cancel()

        _uiState.update {
            it.copy(
                isPaused = true,
                elapsedSeconds = accumulatedSeconds,
                remainingSeconds = maxOf(0, (it.targetDurationMinutes * 60) - accumulatedSeconds)
            )
        }
    }

    fun resumeTimer() {
        if (!_uiState.value.isRunning || !_uiState.value.isPaused) return

        segmentStartTimeMillis = System.currentTimeMillis()
        _uiState.update {
            it.copy(isPaused = false)
        }

        startTicker()
    }

    private fun startTicker() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val now = System.currentTimeMillis()
                val currentSegmentSeconds = ((now - segmentStartTimeMillis) / 1000).toInt()
                val totalElapsed = accumulatedSeconds + currentSegmentSeconds
                val targetTotal = _uiState.value.targetDurationMinutes * 60
                val remaining = maxOf(0, targetTotal - totalElapsed)

                _uiState.update {
                    it.copy(
                        elapsedSeconds = totalElapsed,
                        remainingSeconds = remaining
                    )
                }

                // If timer reached 0, auto complete focus session or continue counting
                if (remaining <= 0 && totalElapsed >= targetTotal) {
                    // Target reached
                }
            }
        }
    }

    fun finishSession(onFinished: () -> Unit) {
        timerJob?.cancel()
        val now = System.currentTimeMillis()
        val totalElapsed = if (_uiState.value.isRunning && !_uiState.value.isPaused) {
            accumulatedSeconds + ((now - segmentStartTimeMillis) / 1000).toInt()
        } else {
            accumulatedSeconds
        }.coerceAtLeast(1)

        viewModelScope.launch {
            val session = FocusSession(
                taskId = _uiState.value.taskId,
                taskTitle = _uiState.value.taskTitle,
                startedAt = if (sessionStartTimeMillis > 0) sessionStartTimeMillis else now - (totalElapsed * 1000L),
                endedAt = now,
                durationSeconds = totalElapsed,
                targetDurationMinutes = _uiState.value.targetDurationMinutes,
                status = FocusSessionStatus.COMPLETED
            )
            saveFocusSessionUseCase(session)

            _uiState.update {
                it.copy(
                    isRunning = false,
                    isPaused = false,
                    isFinished = true,
                    elapsedSeconds = totalElapsed,
                    showCompleteTaskPrompt = it.taskId != null
                )
            }

            if (_uiState.value.taskId == null) {
                onFinished()
            }
        }
    }

    fun completeTaskAndDismiss(onDismiss: () -> Unit) {
        val taskId = _uiState.value.taskId
        if (taskId != null) {
            viewModelScope.launch {
                toggleTaskCompletionUseCase(taskId, isCompleted = true)
                onDismiss()
            }
        } else {
            onDismiss()
        }
    }

    fun dismissWithoutCompletingTask(onDismiss: () -> Unit) {
        onDismiss()
    }

    fun requestExit(onDirectExit: () -> Unit) {
        if (_uiState.value.isRunning) {
            _uiState.update { it.copy(showExitConfirmDialog = true) }
        } else {
            onDirectExit()
        }
    }

    fun dismissExitDialog() {
        _uiState.update { it.copy(showExitConfirmDialog = false) }
    }

    fun confirmExitWithoutSaving(onExit: () -> Unit) {
        timerJob?.cancel()
        _uiState.update { it.copy(showExitConfirmDialog = false, isRunning = false) }
        onExit()
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    class Factory(
        private val taskId: Long?,
        private val taskTitle: String? = null,
        private val initialDurationMinutes: Int?,
        private val getTaskByIdUseCase: GetTaskByIdUseCase,
        private val saveFocusSessionUseCase: SaveFocusSessionUseCase,
        private val toggleTaskCompletionUseCase: ToggleTaskCompletionUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FocusViewModel(
                taskId = taskId,
                taskTitle = taskTitle,
                initialDurationMinutes = initialDurationMinutes,
                getTaskByIdUseCase = getTaskByIdUseCase,
                saveFocusSessionUseCase = saveFocusSessionUseCase,
                toggleTaskCompletionUseCase = toggleTaskCompletionUseCase
            ) as T
        }
    }
}
