package com.flowtasks.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.flowtasks.app.core.datastore.AppThemeMode
import com.flowtasks.app.domain.ai.AIConfig
import com.flowtasks.app.domain.ai.AIError
import com.flowtasks.app.domain.ai.AIProviderType
import com.flowtasks.app.domain.ai.AIResult
import com.flowtasks.app.domain.model.TaskPriority
import com.flowtasks.app.domain.model.TaskSortOrder
import com.flowtasks.app.domain.repository.TaskRepository
import com.flowtasks.app.domain.usecase.GetSettingsUseCase
import com.flowtasks.app.domain.usecase.UpdateSettingsUseCase
import com.flowtasks.app.domain.usecase.ai.GetAIConfigUseCase
import com.flowtasks.app.domain.usecase.ai.ManageAIKeyUseCase
import com.flowtasks.app.domain.usecase.ai.UpdateAIConfigUseCase
import com.flowtasks.app.domain.usecase.ai.ValidateAIConnectionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val getSettingsUseCase: GetSettingsUseCase,
    private val updateSettingsUseCase: UpdateSettingsUseCase,
    private val taskRepository: TaskRepository,
    private val getAIConfigUseCase: GetAIConfigUseCase,
    private val updateAIConfigUseCase: UpdateAIConfigUseCase,
    private val manageAIKeyUseCase: ManageAIKeyUseCase,
    private val validateAIConnectionUseCase: ValidateAIConnectionUseCase
) : ViewModel() {

    private val _aiInteractionState = MutableStateFlow(
        AIInteractionState()
    )

    data class AIInteractionState(
        val isTesting: Boolean = false,
        val testMessage: String? = null,
        val testSuccess: Boolean? = null,
        val keyVersion: Int = 0
    )

    private val userSettingsState = combine(
        getSettingsUseCase(),
        taskRepository.getTotalTaskCount(),
        taskRepository.getCompletedTaskCount()
    ) { settings, total, completed ->
        Triple(settings, total, completed)
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        userSettingsState,
        getAIConfigUseCase(),
        _aiInteractionState
    ) { (settings, totalCount, completedCount), aiConfig, interaction ->
        val hasKey = manageAIKeyUseCase.hasKey(aiConfig.provider)
        val maskedKey = if (hasKey) manageAIKeyUseCase.getMaskedKey(aiConfig.provider) else null

        SettingsUiState(
            settings = settings,
            totalTaskCount = totalCount,
            completedTaskCount = completedCount,
            aiConfig = aiConfig,
            hasAIKey = hasKey,
            maskedApiKey = maskedKey,
            isTestingConnection = interaction.isTesting,
            testConnectionMessage = interaction.testMessage,
            isTestConnectionSuccess = interaction.testSuccess,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun updateThemeMode(themeMode: AppThemeMode) {
        viewModelScope.launch {
            updateSettingsUseCase.updateTheme(themeMode)
        }
    }

    fun updateDefaultPriority(priority: TaskPriority) {
        viewModelScope.launch {
            updateSettingsUseCase.updateDefaultPriority(priority)
        }
    }

    fun updateDefaultSortOrder(sortOrder: TaskSortOrder) {
        viewModelScope.launch {
            updateSettingsUseCase.updateDefaultSortOrder(sortOrder)
        }
    }

    fun updateShowCompleted(show: Boolean) {
        viewModelScope.launch {
            updateSettingsUseCase.updateShowCompleted(show)
        }
    }

    // AI Settings Functions
    fun toggleAIEnabled(enabled: Boolean) {
        viewModelScope.launch {
            updateAIConfigUseCase.setEnabled(enabled)
        }
    }

    fun updateAIProvider(provider: AIProviderType) {
        viewModelScope.launch {
            updateAIConfigUseCase.setProvider(provider)
            _aiInteractionState.update { it.copy(keyVersion = it.keyVersion + 1, testMessage = null, testSuccess = null) }
        }
    }

    fun updateAIModel(modelName: String) {
        viewModelScope.launch {
            updateAIConfigUseCase.setModel(modelName)
        }
    }

    fun saveApiKey(apiKey: String) {
        if (apiKey.isBlank()) return
        viewModelScope.launch {
            try {
                val currentProvider = uiState.value.aiConfig.provider
                manageAIKeyUseCase.saveKey(currentProvider, apiKey.trim())
                _aiInteractionState.update {
                    it.copy(
                        keyVersion = it.keyVersion + 1,
                        testMessage = "API key saved securely in Android Keystore.",
                        testSuccess = true
                    )
                }
            } catch (e: Exception) {
                _aiInteractionState.update {
                    it.copy(
                        testMessage = "Failed to save key: ${e.localizedMessage}",
                        testSuccess = false
                    )
                }
            }
        }
    }

    fun removeApiKey() {
        viewModelScope.launch {
            try {
                val currentProvider = uiState.value.aiConfig.provider
                manageAIKeyUseCase.removeKey(currentProvider)
                _aiInteractionState.update {
                    it.copy(
                        keyVersion = it.keyVersion + 1,
                        testMessage = "API key removed.",
                        testSuccess = null
                    )
                }
            } catch (e: Exception) {
                _aiInteractionState.update {
                    it.copy(
                        testMessage = "Failed to remove key: ${e.localizedMessage}",
                        testSuccess = false
                    )
                }
            }
        }
    }

    fun testAIConnection() {
        viewModelScope.launch {
            _aiInteractionState.update { it.copy(isTesting = true, testMessage = null, testSuccess = null) }

            when (val result = validateAIConnectionUseCase()) {
                is AIResult.Success -> {
                    _aiInteractionState.update {
                        it.copy(
                            isTesting = false,
                            testSuccess = true,
                            testMessage = "Connection verified! Active provider is working properly."
                        )
                    }
                }
                is AIResult.Error -> {
                    val msg = when (val error = result.error) {
                        is AIError.MissingApiKey -> "API key is not configured. Please enter and save an API key."
                        is AIError.InvalidApiKey -> "Invalid API key. Please check your key in Google AI Studio."
                        is AIError.Unauthorized -> "Unauthorized API key or insufficient permissions."
                        is AIError.NetworkUnavailable -> "Network unreachable: ${error.message}"
                        is AIError.Timeout -> "Connection timed out."
                        is AIError.ProviderDisabled -> "AI features are currently disabled."
                        else -> error.message
                    }
                    _aiInteractionState.update {
                        it.copy(
                            isTesting = false,
                            testSuccess = false,
                            testMessage = msg
                        )
                    }
                }
                else -> {
                    _aiInteractionState.update { it.copy(isTesting = false) }
                }
            }
        }
    }

    fun clearTestConnectionResult() {
        _aiInteractionState.update { it.copy(testMessage = null, testSuccess = null) }
    }

    class Factory(
        private val getSettingsUseCase: GetSettingsUseCase,
        private val updateSettingsUseCase: UpdateSettingsUseCase,
        private val taskRepository: TaskRepository,
        private val getAIConfigUseCase: GetAIConfigUseCase,
        private val updateAIConfigUseCase: UpdateAIConfigUseCase,
        private val manageAIKeyUseCase: ManageAIKeyUseCase,
        private val validateAIConnectionUseCase: ValidateAIConnectionUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(
                getSettingsUseCase,
                updateSettingsUseCase,
                taskRepository,
                getAIConfigUseCase,
                updateAIConfigUseCase,
                manageAIKeyUseCase,
                validateAIConnectionUseCase
            ) as T
        }
    }
}

