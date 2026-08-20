package com.flowtasks.app.core.di

import android.content.Context
import com.flowtasks.app.core.ads.InterstitialAdManager
import com.flowtasks.app.core.ads.InterstitialAdManagerImpl
import com.flowtasks.app.core.common.DefaultDispatcherProvider
import com.flowtasks.app.core.common.DispatcherProvider
import com.flowtasks.app.core.database.AppDatabase
import com.flowtasks.app.core.datastore.UserPreferencesDataStore
import com.flowtasks.app.core.notification.TaskReminderScheduler
import com.flowtasks.app.core.security.AndroidKeystoreEncryptor
import com.flowtasks.app.core.security.SecureAIKeyStorage
import com.flowtasks.app.data.ai.context.AIContextProviderImpl
import com.flowtasks.app.data.ai.provider.AnthropicAIProvider
import com.flowtasks.app.data.ai.provider.CustomAIProvider
import com.flowtasks.app.data.ai.provider.GeminiAIProvider
import com.flowtasks.app.data.ai.provider.OpenAIAIProvider
import com.flowtasks.app.data.ai.repository.AIConfigRepositoryImpl
import com.flowtasks.app.data.ai.service.AIServiceImpl
import com.flowtasks.app.data.repository.FocusSessionRepositoryImpl
import com.flowtasks.app.data.repository.GoalRepositoryImpl
import com.flowtasks.app.data.repository.HabitRepositoryImpl
import com.flowtasks.app.data.repository.ProjectRepositoryImpl
import com.flowtasks.app.data.repository.SettingsRepositoryImpl
import com.flowtasks.app.data.repository.TaskListRepositoryImpl
import com.flowtasks.app.data.repository.TaskRepositoryImpl
import com.flowtasks.app.domain.ai.AIConfigRepository
import com.flowtasks.app.domain.ai.AIContextProvider
import com.flowtasks.app.domain.ai.AIKeyManager
import com.flowtasks.app.domain.ai.AIProviderType
import com.flowtasks.app.domain.ai.AIService
import com.flowtasks.app.domain.repository.FocusSessionRepository
import com.flowtasks.app.domain.repository.GoalRepository
import com.flowtasks.app.domain.repository.HabitRepository
import com.flowtasks.app.domain.repository.ProjectRepository
import com.flowtasks.app.domain.repository.SettingsRepository
import com.flowtasks.app.domain.repository.TaskListRepository
import com.flowtasks.app.domain.repository.TaskRepository
import com.flowtasks.app.domain.usecase.CreateGoalUseCase
import com.flowtasks.app.domain.usecase.CreateHabitUseCase
import com.flowtasks.app.domain.usecase.CreateProjectUseCase
import com.flowtasks.app.domain.usecase.CreateTaskListUseCase
import com.flowtasks.app.domain.usecase.CreateTaskUseCase
import com.flowtasks.app.domain.usecase.DeleteGoalUseCase
import com.flowtasks.app.domain.usecase.DeleteHabitUseCase
import com.flowtasks.app.domain.usecase.DeleteProjectUseCase
import com.flowtasks.app.domain.usecase.DeleteTaskListUseCase
import com.flowtasks.app.domain.usecase.DeleteTaskUseCase
import com.flowtasks.app.domain.usecase.GetFocusSessionsByTaskUseCase
import com.flowtasks.app.domain.usecase.GetGoalsUseCase
import com.flowtasks.app.domain.usecase.GetHabitsUseCase
import com.flowtasks.app.domain.usecase.GetProductivityStatsUseCase
import com.flowtasks.app.domain.usecase.GetProjectsByGoalUseCase
import com.flowtasks.app.domain.usecase.GetProjectsUseCase
import com.flowtasks.app.domain.usecase.GetSettingsUseCase
import com.flowtasks.app.domain.usecase.GetSubtasksUseCase
import com.flowtasks.app.domain.usecase.GetTaskByIdUseCase
import com.flowtasks.app.domain.usecase.GetTaskListsUseCase
import com.flowtasks.app.domain.usecase.GetTasksUseCase
import com.flowtasks.app.domain.usecase.ReorderTaskListsUseCase
import com.flowtasks.app.domain.usecase.ReorderTasksUseCase
import com.flowtasks.app.domain.usecase.SaveFocusSessionUseCase
import com.flowtasks.app.domain.usecase.SearchTasksUseCase
import com.flowtasks.app.domain.usecase.ToggleGoalStatusUseCase
import com.flowtasks.app.domain.usecase.ToggleHabitCompletionForDateUseCase
import com.flowtasks.app.domain.usecase.ToggleHabitCompletionTodayUseCase
import com.flowtasks.app.domain.usecase.ToggleProjectStatusUseCase
import com.flowtasks.app.domain.usecase.ToggleTaskCompletionUseCase
import com.flowtasks.app.domain.usecase.ToggleTaskStarUseCase
import com.flowtasks.app.domain.usecase.UpdateGoalUseCase
import com.flowtasks.app.domain.usecase.UpdateHabitUseCase
import com.flowtasks.app.domain.usecase.UpdateProjectUseCase
import com.flowtasks.app.domain.usecase.UpdateSettingsUseCase
import com.flowtasks.app.domain.usecase.UpdateTaskListUseCase
import com.flowtasks.app.domain.usecase.UpdateTaskUseCase
import com.flowtasks.app.domain.usecase.ai.AITaskAssistantUseCase
import com.flowtasks.app.domain.usecase.ai.AIProductivityUseCase
import com.flowtasks.app.domain.usecase.ai.GenerateAITextUseCase
import com.flowtasks.app.domain.usecase.ai.GetAIConfigUseCase
import com.flowtasks.app.domain.usecase.ai.GetAIContextUseCase
import com.flowtasks.app.domain.usecase.ai.ManageAIKeyUseCase
import com.flowtasks.app.domain.usecase.ai.UpdateAIConfigUseCase
import com.flowtasks.app.domain.usecase.ai.ValidateAIConnectionUseCase

interface AppContainer {
    val dispatchers: DispatcherProvider
    val database: AppDatabase
    val taskRepository: TaskRepository
    val taskListRepository: TaskListRepository
    val goalRepository: GoalRepository
    val projectRepository: ProjectRepository
    val habitRepository: HabitRepository
    val focusSessionRepository: FocusSessionRepository
    val settingsRepository: SettingsRepository
    val reminderScheduler: TaskReminderScheduler
    val interstitialAdManager: InterstitialAdManager

    // AI Layer
    val aiKeyManager: AIKeyManager
    val aiConfigRepository: AIConfigRepository
    val aiService: AIService
    val aiContextProvider: AIContextProvider

    val getTasksUseCase: GetTasksUseCase
    val getTaskByIdUseCase: GetTaskByIdUseCase
    val getSubtasksUseCase: GetSubtasksUseCase
    val createTaskUseCase: CreateTaskUseCase
    val updateTaskUseCase: UpdateTaskUseCase
    val deleteTaskUseCase: DeleteTaskUseCase
    val toggleTaskCompletionUseCase: ToggleTaskCompletionUseCase
    val toggleTaskStarUseCase: ToggleTaskStarUseCase
    val searchTasksUseCase: SearchTasksUseCase
    val reorderTasksUseCase: ReorderTasksUseCase

    val getTaskListsUseCase: GetTaskListsUseCase
    val createTaskListUseCase: CreateTaskListUseCase
    val updateTaskListUseCase: UpdateTaskListUseCase
    val deleteTaskListUseCase: DeleteTaskListUseCase
    val reorderTaskListsUseCase: ReorderTaskListsUseCase

    val getGoalsUseCase: GetGoalsUseCase
    val createGoalUseCase: CreateGoalUseCase
    val updateGoalUseCase: UpdateGoalUseCase
    val deleteGoalUseCase: DeleteGoalUseCase
    val toggleGoalStatusUseCase: ToggleGoalStatusUseCase

    val getProjectsUseCase: GetProjectsUseCase
    val getProjectsByGoalUseCase: GetProjectsByGoalUseCase
    val createProjectUseCase: CreateProjectUseCase
    val updateProjectUseCase: UpdateProjectUseCase
    val deleteProjectUseCase: DeleteProjectUseCase
    val toggleProjectStatusUseCase: ToggleProjectStatusUseCase

    val getHabitsUseCase: GetHabitsUseCase
    val createHabitUseCase: CreateHabitUseCase
    val updateHabitUseCase: UpdateHabitUseCase
    val deleteHabitUseCase: DeleteHabitUseCase
    val toggleHabitCompletionTodayUseCase: ToggleHabitCompletionTodayUseCase
    val toggleHabitCompletionForDateUseCase: ToggleHabitCompletionForDateUseCase

    val saveFocusSessionUseCase: SaveFocusSessionUseCase
    val getFocusSessionsByTaskUseCase: GetFocusSessionsByTaskUseCase
    val getProductivityStatsUseCase: GetProductivityStatsUseCase

    val getSettingsUseCase: GetSettingsUseCase
    val updateSettingsUseCase: UpdateSettingsUseCase

    // AI Use Cases
    val generateAITextUseCase: GenerateAITextUseCase
    val getAIConfigUseCase: GetAIConfigUseCase
    val updateAIConfigUseCase: UpdateAIConfigUseCase
    val manageAIKeyUseCase: ManageAIKeyUseCase
    val getAIContextUseCase: GetAIContextUseCase
    val validateAIConnectionUseCase: ValidateAIConnectionUseCase
    val aiTaskAssistantUseCase: AITaskAssistantUseCase
    val aiProductivityUseCase: AIProductivityUseCase
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    override val dispatchers: DispatcherProvider by lazy {
        DefaultDispatcherProvider()
    }

    override val database: AppDatabase by lazy {
        AppDatabase.getInstance(context)
    }

    override val taskRepository: TaskRepository by lazy {
        TaskRepositoryImpl(database.taskDao(), dispatchers)
    }

    override val taskListRepository: TaskListRepository by lazy {
        TaskListRepositoryImpl(database.taskListDao(), dispatchers)
    }

    override val goalRepository: GoalRepository by lazy {
        GoalRepositoryImpl(database.goalDao(), database.projectDao(), database.taskDao(), dispatchers)
    }

    override val projectRepository: ProjectRepository by lazy {
        ProjectRepositoryImpl(database.projectDao(), database.goalDao(), database.taskDao(), dispatchers)
    }

    override val habitRepository: HabitRepository by lazy {
        HabitRepositoryImpl(database.habitDao(), database.habitCompletionDao(), dispatchers)
    }

    override val focusSessionRepository: FocusSessionRepository by lazy {
        FocusSessionRepositoryImpl(database.focusSessionDao(), database.taskDao(), dispatchers)
    }

    override val reminderScheduler: TaskReminderScheduler by lazy {
        TaskReminderScheduler(context)
    }

    private val preferencesDataStore: UserPreferencesDataStore by lazy {
        UserPreferencesDataStore(context)
    }

    override val settingsRepository: SettingsRepository by lazy {
        SettingsRepositoryImpl(preferencesDataStore)
    }

    override val interstitialAdManager: InterstitialAdManager by lazy {
        InterstitialAdManagerImpl()
    }

    // AI Foundation
    private val keystoreEncryptor: AndroidKeystoreEncryptor by lazy {
        AndroidKeystoreEncryptor()
    }

    override val aiKeyManager: AIKeyManager by lazy {
        SecureAIKeyStorage(context, keystoreEncryptor)
    }

    override val aiConfigRepository: AIConfigRepository by lazy {
        AIConfigRepositoryImpl(context)
    }

    private val geminiAIProvider: GeminiAIProvider by lazy {
        GeminiAIProvider()
    }

    private val openAIAIProvider: OpenAIAIProvider by lazy {
        OpenAIAIProvider()
    }

    private val anthropicAIProvider: AnthropicAIProvider by lazy {
        AnthropicAIProvider()
    }

    private val customAIProvider: CustomAIProvider by lazy {
        CustomAIProvider()
    }

    override val aiService: AIService by lazy {
        AIServiceImpl(
            aiConfigRepository = aiConfigRepository,
            aiKeyManager = aiKeyManager,
            providers = mapOf(
                AIProviderType.GEMINI to geminiAIProvider,
                AIProviderType.OPENAI to openAIAIProvider,
                AIProviderType.ANTHROPIC to anthropicAIProvider,
                AIProviderType.CUSTOM to customAIProvider
            )
        )
    }

    override val aiContextProvider: AIContextProvider by lazy {
        AIContextProviderImpl(
            taskRepository = taskRepository,
            goalRepository = goalRepository,
            projectRepository = projectRepository,
            habitRepository = habitRepository,
            getProductivityStatsUseCase = getProductivityStatsUseCase
        )
    }

    override val getTasksUseCase: GetTasksUseCase by lazy { GetTasksUseCase(taskRepository) }
    override val getTaskByIdUseCase: GetTaskByIdUseCase by lazy { GetTaskByIdUseCase(taskRepository) }
    override val getSubtasksUseCase: GetSubtasksUseCase by lazy { GetSubtasksUseCase(taskRepository) }
    override val createTaskUseCase: CreateTaskUseCase by lazy { CreateTaskUseCase(taskRepository, reminderScheduler) }
    override val updateTaskUseCase: UpdateTaskUseCase by lazy { UpdateTaskUseCase(taskRepository, reminderScheduler) }
    override val deleteTaskUseCase: DeleteTaskUseCase by lazy { DeleteTaskUseCase(taskRepository, reminderScheduler) }
    override val toggleTaskCompletionUseCase: ToggleTaskCompletionUseCase by lazy { ToggleTaskCompletionUseCase(taskRepository, reminderScheduler) }
    override val toggleTaskStarUseCase: ToggleTaskStarUseCase by lazy { ToggleTaskStarUseCase(taskRepository) }
    override val searchTasksUseCase: SearchTasksUseCase by lazy { SearchTasksUseCase(taskRepository) }
    override val reorderTasksUseCase: ReorderTasksUseCase by lazy { ReorderTasksUseCase(taskRepository) }

    override val getTaskListsUseCase: GetTaskListsUseCase by lazy { GetTaskListsUseCase(taskListRepository) }
    override val createTaskListUseCase: CreateTaskListUseCase by lazy { CreateTaskListUseCase(taskListRepository) }
    override val updateTaskListUseCase: UpdateTaskListUseCase by lazy { UpdateTaskListUseCase(taskListRepository) }
    override val deleteTaskListUseCase: DeleteTaskListUseCase by lazy { DeleteTaskListUseCase(taskListRepository, taskRepository) }
    override val reorderTaskListsUseCase: ReorderTaskListsUseCase by lazy { ReorderTaskListsUseCase(taskListRepository) }

    override val getGoalsUseCase: GetGoalsUseCase by lazy { GetGoalsUseCase(goalRepository) }
    override val createGoalUseCase: CreateGoalUseCase by lazy { CreateGoalUseCase(goalRepository) }
    override val updateGoalUseCase: UpdateGoalUseCase by lazy { UpdateGoalUseCase(goalRepository) }
    override val deleteGoalUseCase: DeleteGoalUseCase by lazy { DeleteGoalUseCase(goalRepository) }
    override val toggleGoalStatusUseCase: ToggleGoalStatusUseCase by lazy { ToggleGoalStatusUseCase(goalRepository) }

    override val getProjectsUseCase: GetProjectsUseCase by lazy { GetProjectsUseCase(projectRepository) }
    override val getProjectsByGoalUseCase: GetProjectsByGoalUseCase by lazy { GetProjectsByGoalUseCase(projectRepository) }
    override val createProjectUseCase: CreateProjectUseCase by lazy { CreateProjectUseCase(projectRepository) }
    override val updateProjectUseCase: UpdateProjectUseCase by lazy { UpdateProjectUseCase(projectRepository) }
    override val deleteProjectUseCase: DeleteProjectUseCase by lazy { DeleteProjectUseCase(projectRepository) }
    override val toggleProjectStatusUseCase: ToggleProjectStatusUseCase by lazy { ToggleProjectStatusUseCase(projectRepository) }

    override val getHabitsUseCase: GetHabitsUseCase by lazy { GetHabitsUseCase(habitRepository) }
    override val createHabitUseCase: CreateHabitUseCase by lazy { CreateHabitUseCase(habitRepository) }
    override val updateHabitUseCase: UpdateHabitUseCase by lazy { UpdateHabitUseCase(habitRepository) }
    override val deleteHabitUseCase: DeleteHabitUseCase by lazy { DeleteHabitUseCase(habitRepository) }
    override val toggleHabitCompletionTodayUseCase: ToggleHabitCompletionTodayUseCase by lazy { ToggleHabitCompletionTodayUseCase(habitRepository) }
    override val toggleHabitCompletionForDateUseCase: ToggleHabitCompletionForDateUseCase by lazy { ToggleHabitCompletionForDateUseCase(habitRepository) }

    override val saveFocusSessionUseCase: SaveFocusSessionUseCase by lazy { SaveFocusSessionUseCase(focusSessionRepository) }
    override val getFocusSessionsByTaskUseCase: GetFocusSessionsByTaskUseCase by lazy { GetFocusSessionsByTaskUseCase(focusSessionRepository) }
    override val getProductivityStatsUseCase: GetProductivityStatsUseCase by lazy {
        GetProductivityStatsUseCase(taskRepository, focusSessionRepository, habitRepository, goalRepository, projectRepository)
    }

    override val getSettingsUseCase: GetSettingsUseCase by lazy { GetSettingsUseCase(settingsRepository) }
    override val updateSettingsUseCase: UpdateSettingsUseCase by lazy { UpdateSettingsUseCase(settingsRepository) }

    // AI Use Cases
    override val generateAITextUseCase: GenerateAITextUseCase by lazy {
        GenerateAITextUseCase(aiService)
    }
    override val getAIConfigUseCase: GetAIConfigUseCase by lazy {
        GetAIConfigUseCase(aiConfigRepository)
    }
    override val updateAIConfigUseCase: UpdateAIConfigUseCase by lazy {
        UpdateAIConfigUseCase(aiConfigRepository)
    }
    override val manageAIKeyUseCase: ManageAIKeyUseCase by lazy {
        ManageAIKeyUseCase(aiKeyManager)
    }
    override val getAIContextUseCase: GetAIContextUseCase by lazy {
        GetAIContextUseCase(aiContextProvider)
    }
    override val validateAIConnectionUseCase: ValidateAIConnectionUseCase by lazy {
        ValidateAIConnectionUseCase(aiService)
    }
    override val aiTaskAssistantUseCase: AITaskAssistantUseCase by lazy {
        AITaskAssistantUseCase(aiService)
    }
    override val aiProductivityUseCase: AIProductivityUseCase by lazy {
        AIProductivityUseCase(aiService)
    }
}
