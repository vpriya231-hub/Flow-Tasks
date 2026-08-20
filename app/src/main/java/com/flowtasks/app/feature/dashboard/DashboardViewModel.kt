package com.flowtasks.app.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.flowtasks.app.domain.ai.AIError
import com.flowtasks.app.domain.ai.AIResult
import com.flowtasks.app.domain.model.ProductivityStats
import com.flowtasks.app.domain.usecase.GetProductivityStatsUseCase
import com.flowtasks.app.domain.usecase.ai.AIProductivityUseCase
import com.flowtasks.app.domain.usecase.ai.GetAIConfigUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = true,
    val stats: ProductivityStats = ProductivityStats(),
    val isAIEnabled: Boolean = false,
    val isAILoading: Boolean = false,
    val aiActionMessage: String? = null,
    val dailyPlanSummary: String? = null,
    val dailyPlanSuggestions: List<String> = emptyList(),
    val productivitySummary: String? = null,
    val aiErrorMessage: String? = null
)

class DashboardViewModel(
    private val getProductivityStatsUseCase: GetProductivityStatsUseCase,
    private val aiProductivityUseCase: AIProductivityUseCase? = null,
    private val getAIConfigUseCase: GetAIConfigUseCase? = null
) : ViewModel() {

    private val _aiState = MutableStateFlow(
        object {
            var isAIEnabled = false
            var isAILoading = false
            var aiActionMessage: String? = null
            var dailyPlanSummary: String? = null
            var dailyPlanSuggestions = listOf<String>()
            var productivitySummary: String? = null
            var aiErrorMessage: String? = null
        }
    )

    private val _internalAIState = MutableStateFlow(
        InternalAIState()
    )

    data class InternalAIState(
        val isAIEnabled: Boolean = false,
        val isAILoading: Boolean = false,
        val aiActionMessage: String? = null,
        val dailyPlanSummary: String? = null,
        val dailyPlanSuggestions: List<String> = emptyList(),
        val productivitySummary: String? = null,
        val aiErrorMessage: String? = null
    )

    init {
        if (getAIConfigUseCase != null) {
            viewModelScope.launch {
                getAIConfigUseCase().collect { config ->
                    _internalAIState.update { it.copy(isAIEnabled = config.isEnabled) }
                }
            }
        }
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        getProductivityStatsUseCase(),
        _internalAIState
    ) { stats, ai ->
        DashboardUiState(
            isLoading = false,
            stats = stats,
            isAIEnabled = ai.isAIEnabled,
            isAILoading = ai.isAILoading,
            aiActionMessage = ai.aiActionMessage,
            dailyPlanSummary = ai.dailyPlanSummary,
            dailyPlanSuggestions = ai.dailyPlanSuggestions,
            productivitySummary = ai.productivitySummary,
            aiErrorMessage = ai.aiErrorMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    fun generateDailyPlan() {
        if (aiProductivityUseCase == null) return
        viewModelScope.launch {
            _internalAIState.update {
                it.copy(
                    isAILoading = true,
                    aiActionMessage = "Synthesizing today's tasks and workload...",
                    aiErrorMessage = null
                )
            }
            when (val result = aiProductivityUseCase.generateDailyPlan()) {
                is AIResult.Success -> {
                    val plan = result.data
                    _internalAIState.update {
                        it.copy(
                            isAILoading = false,
                            aiActionMessage = null,
                            dailyPlanSummary = plan.summary,
                            dailyPlanSuggestions = plan.suggestedSchedule
                        )
                    }
                }
                is AIResult.Error -> {
                    _internalAIState.update {
                        it.copy(
                            isAILoading = false,
                            aiActionMessage = null,
                            aiErrorMessage = formatAIError(result.error)
                        )
                    }
                }
                else -> {}
            }
        }
    }

    fun generateProductivitySummary() {
        if (aiProductivityUseCase == null) return
        viewModelScope.launch {
            _internalAIState.update {
                it.copy(
                    isAILoading = true,
                    aiActionMessage = "Analyzing productivity trends...",
                    aiErrorMessage = null
                )
            }
            when (val result = aiProductivityUseCase.generateProductivitySummary()) {
                is AIResult.Success -> {
                    _internalAIState.update {
                        it.copy(
                            isAILoading = false,
                            aiActionMessage = null,
                            productivitySummary = result.data
                        )
                    }
                }
                is AIResult.Error -> {
                    _internalAIState.update {
                        it.copy(
                            isAILoading = false,
                            aiActionMessage = null,
                            aiErrorMessage = formatAIError(result.error)
                        )
                    }
                }
                else -> {}
            }
        }
    }

    fun dismissDailyPlan() {
        _internalAIState.update { it.copy(dailyPlanSummary = null, dailyPlanSuggestions = emptyList()) }
    }

    fun dismissProductivitySummary() {
        _internalAIState.update { it.copy(productivitySummary = null) }
    }

    fun dismissAIError() {
        _internalAIState.update { it.copy(aiErrorMessage = null) }
    }

    private fun formatAIError(error: AIError): String {
        return when (error) {
            is AIError.MissingApiKey -> "API key not configured. Open Settings to configure Gemini API."
            is AIError.InvalidApiKey -> "Invalid API key. Please check your settings."
            is AIError.ProviderDisabled -> "AI features are disabled in Settings."
            is AIError.RateLimitExceeded -> "Rate limit reached. Please wait a moment."
            is AIError.NetworkUnavailable -> "Network error: ${error.message}"
            is AIError.Timeout -> "Request timed out."
            else -> "AI error: ${error.message}"
        }
    }

    class Factory(
        private val getProductivityStatsUseCase: GetProductivityStatsUseCase,
        private val aiProductivityUseCase: AIProductivityUseCase? = null,
        private val getAIConfigUseCase: GetAIConfigUseCase? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(
                getProductivityStatsUseCase,
                aiProductivityUseCase,
                getAIConfigUseCase
            ) as T
        }
    }
}
