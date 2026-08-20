package com.flowtasks.app

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.flowtasks.app.core.datastore.AppThemeMode
import com.flowtasks.app.core.designsystem.component.NavigationTab
import com.flowtasks.app.domain.model.TaskFilter
import com.flowtasks.app.feature.dashboard.DashboardScreen
import com.flowtasks.app.feature.dashboard.DashboardViewModel
import com.flowtasks.app.feature.focus.FocusScreen
import com.flowtasks.app.feature.focus.FocusViewModel
import com.flowtasks.app.feature.goal.GoalsScreen
import com.flowtasks.app.feature.goal.GoalsViewModel
import com.flowtasks.app.feature.habit.HabitsScreen
import com.flowtasks.app.feature.habit.HabitsViewModel
import com.flowtasks.app.feature.home.HomeScreen
import com.flowtasks.app.feature.home.HomeViewModel
import com.flowtasks.app.feature.lists.ListsScreen
import com.flowtasks.app.feature.lists.ListsViewModel
import com.flowtasks.app.feature.project.ProjectTasksScreen
import com.flowtasks.app.feature.project.ProjectTasksViewModel
import com.flowtasks.app.feature.search.SearchScreen
import com.flowtasks.app.feature.search.SearchViewModel
import com.flowtasks.app.feature.settings.SettingsScreen
import com.flowtasks.app.feature.settings.SettingsViewModel
import com.flowtasks.app.feature.task.TaskDetailScreen
import com.flowtasks.app.feature.task.TaskDetailViewModel
import com.flowtasks.app.ui.theme.FlowTasksTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as FlowTasksApplication).container

        setContent {
            val context = LocalContext.current
            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { /* Permission granted / denied */ }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            val settingsState by appContainer.settingsRepository.getUserSettings()
                .collectAsStateWithLifecycle(initialValue = com.flowtasks.app.core.datastore.UserSettings())

            val isDarkTheme = when (settingsState.themeMode) {
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }

            FlowTasksTheme(darkTheme = isDarkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    FlowTasksNavHost(appContainer = appContainer)
                }
            }
        }
    }
}

@Composable
fun FlowTasksNavHost(
    appContainer: com.flowtasks.app.core.di.AppContainer,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    fun navigateToTab(tab: NavigationTab) {
        when (tab) {
            NavigationTab.TASKS -> {
                navController.navigate("home") {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
            NavigationTab.GOALS -> {
                navController.navigate("goals") {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
            NavigationTab.HABITS -> {
                navController.navigate("habits") {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
            NavigationTab.DASHBOARD -> {
                navController.navigate("dashboard") {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
            NavigationTab.LISTS -> {
                navController.navigate("lists") {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
            NavigationTab.STARRED -> {
                navController.navigate("starred") {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
            NavigationTab.SETTINGS -> {
                navController.navigate("settings") {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = modifier
    ) {
        composable("home") {
            val homeViewModel: HomeViewModel = viewModel(
                factory = HomeViewModel.Factory(
                    getTasksUseCase = appContainer.getTasksUseCase,
                    getTaskListsUseCase = appContainer.getTaskListsUseCase,
                    createTaskUseCase = appContainer.createTaskUseCase,
                    deleteTaskUseCase = appContainer.deleteTaskUseCase,
                    toggleTaskCompletionUseCase = appContainer.toggleTaskCompletionUseCase,
                    toggleTaskStarUseCase = appContainer.toggleTaskStarUseCase,
                    createTaskListUseCase = appContainer.createTaskListUseCase,
                    deleteTaskListUseCase = appContainer.deleteTaskListUseCase,
                    getSettingsUseCase = appContainer.getSettingsUseCase,
                    reorderTasksUseCase = appContainer.reorderTasksUseCase
                )
            )

            HomeScreen(
                viewModel = homeViewModel,
                onNavigateToTaskDetail = { taskId ->
                    if (taskId != null) {
                        navController.navigate("task_detail?taskId=$taskId")
                    } else {
                        navController.navigate("task_detail")
                    }
                },
                onNavigateToSearch = {
                    navController.navigate("search")
                },
                onNavigateToSettings = {
                    navController.navigate("settings")
                },
                onNavigateToLists = {
                    navController.navigate("lists")
                },
                onNavigateToFocus = {
                    navController.navigate("focus")
                },
                onTabSelected = { tab ->
                    navigateToTab(tab)
                }
            )
        }

        composable("goals") {
            val goalsViewModel: GoalsViewModel = viewModel(
                factory = GoalsViewModel.Factory(
                    getGoalsUseCase = appContainer.getGoalsUseCase,
                    createGoalUseCase = appContainer.createGoalUseCase,
                    updateGoalUseCase = appContainer.updateGoalUseCase,
                    deleteGoalUseCase = appContainer.deleteGoalUseCase,
                    toggleGoalStatusUseCase = appContainer.toggleGoalStatusUseCase,
                    getProjectsUseCase = appContainer.getProjectsUseCase,
                    createProjectUseCase = appContainer.createProjectUseCase,
                    updateProjectUseCase = appContainer.updateProjectUseCase,
                    deleteProjectUseCase = appContainer.deleteProjectUseCase,
                    toggleProjectStatusUseCase = appContainer.toggleProjectStatusUseCase
                )
            )

            GoalsScreen(
                viewModel = goalsViewModel,
                onTabSelected = { tab -> navigateToTab(tab) },
                onNavigateToProjectTasks = { projectId, projectTitle ->
                    navController.navigate("project_tasks?projectId=$projectId&projectTitle=$projectTitle")
                }
            )
        }

        composable("habits") {
            val habitsViewModel: HabitsViewModel = viewModel(
                factory = HabitsViewModel.Factory(
                    getHabitsUseCase = appContainer.getHabitsUseCase,
                    createHabitUseCase = appContainer.createHabitUseCase,
                    updateHabitUseCase = appContainer.updateHabitUseCase,
                    deleteHabitUseCase = appContainer.deleteHabitUseCase,
                    toggleHabitCompletionTodayUseCase = appContainer.toggleHabitCompletionTodayUseCase,
                    toggleHabitCompletionForDateUseCase = appContainer.toggleHabitCompletionForDateUseCase
                )
            )

            HabitsScreen(
                viewModel = habitsViewModel,
                onTabSelected = { tab -> navigateToTab(tab) }
            )
        }

        composable(
            route = "project_tasks?projectId={projectId}&projectTitle={projectTitle}",
            arguments = listOf(
                navArgument("projectId") {
                    type = NavType.LongType
                    defaultValue = -1L
                },
                navArgument("projectTitle") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId") ?: -1L
            val projectTitle = backStackEntry.arguments?.getString("projectTitle") ?: "Project"

            val projectTasksViewModel: ProjectTasksViewModel = viewModel(
                key = "project_tasks_$projectId",
                factory = ProjectTasksViewModel.Factory(
                    projectId = projectId,
                    projectTitle = projectTitle,
                    getTasksUseCase = appContainer.getTasksUseCase,
                    createTaskUseCase = appContainer.createTaskUseCase,
                    toggleTaskCompletionUseCase = appContainer.toggleTaskCompletionUseCase,
                    toggleTaskStarUseCase = appContainer.toggleTaskStarUseCase,
                    deleteTaskUseCase = appContainer.deleteTaskUseCase
                )
            )

            ProjectTasksScreen(
                viewModel = projectTasksViewModel,
                onNavigateBack = { navController.popBackStack() },
                onTaskClick = { task ->
                    navController.navigate("task_detail?taskId=${task.id}")
                }
            )
        }

        composable("starred") {
            val starredViewModel: HomeViewModel = viewModel(
                key = "starred_view_model",
                factory = HomeViewModel.Factory(
                    getTasksUseCase = appContainer.getTasksUseCase,
                    getTaskListsUseCase = appContainer.getTaskListsUseCase,
                    createTaskUseCase = appContainer.createTaskUseCase,
                    deleteTaskUseCase = appContainer.deleteTaskUseCase,
                    toggleTaskCompletionUseCase = appContainer.toggleTaskCompletionUseCase,
                    toggleTaskStarUseCase = appContainer.toggleTaskStarUseCase,
                    createTaskListUseCase = appContainer.createTaskListUseCase,
                    deleteTaskListUseCase = appContainer.deleteTaskListUseCase,
                    getSettingsUseCase = appContainer.getSettingsUseCase,
                    reorderTasksUseCase = appContainer.reorderTasksUseCase
                )
            )
            starredViewModel.setFilter(TaskFilter.Starred)

            HomeScreen(
                viewModel = starredViewModel,
                onNavigateToTaskDetail = { taskId ->
                    if (taskId != null) {
                        navController.navigate("task_detail?taskId=$taskId")
                    } else {
                        navController.navigate("task_detail")
                    }
                },
                onNavigateToSearch = {
                    navController.navigate("search")
                },
                onNavigateToSettings = {
                    navController.navigate("settings")
                },
                onNavigateToLists = {
                    navController.navigate("lists")
                },
                onNavigateToFocus = {
                    navController.navigate("focus")
                },
                onTabSelected = { tab ->
                    navigateToTab(tab)
                }
            )
        }

        composable("lists") {
            val listsViewModel: ListsViewModel = viewModel(
                factory = ListsViewModel.Factory(
                    getTaskListsUseCase = appContainer.getTaskListsUseCase,
                    createTaskListUseCase = appContainer.createTaskListUseCase,
                    updateTaskListUseCase = appContainer.updateTaskListUseCase,
                    deleteTaskListUseCase = appContainer.deleteTaskListUseCase,
                    getTasksUseCase = appContainer.getTasksUseCase,
                    reorderTaskListsUseCase = appContainer.reorderTaskListsUseCase
                )
            )

            ListsScreen(
                viewModel = listsViewModel,
                onNavigateToHomeWithList = { listId, listName ->
                    navController.navigate("home_list?listId=$listId&listName=$listName")
                },
                onNavigateToSearch = {
                    navController.navigate("search")
                },
                onTabSelected = { tab ->
                    navigateToTab(tab)
                }
            )
        }

        composable(
            route = "home_list?listId={listId}&listName={listName}",
            arguments = listOf(
                navArgument("listId") {
                    type = NavType.LongType
                    defaultValue = -1L
                },
                navArgument("listName") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val listId = backStackEntry.arguments?.getLong("listId") ?: -1L
            val listName = backStackEntry.arguments?.getString("listName") ?: ""

            val listHomeViewModel: HomeViewModel = viewModel(
                key = "list_home_$listId",
                factory = HomeViewModel.Factory(
                    getTasksUseCase = appContainer.getTasksUseCase,
                    getTaskListsUseCase = appContainer.getTaskListsUseCase,
                    createTaskUseCase = appContainer.createTaskUseCase,
                    deleteTaskUseCase = appContainer.deleteTaskUseCase,
                    toggleTaskCompletionUseCase = appContainer.toggleTaskCompletionUseCase,
                    toggleTaskStarUseCase = appContainer.toggleTaskStarUseCase,
                    createTaskListUseCase = appContainer.createTaskListUseCase,
                    deleteTaskListUseCase = appContainer.deleteTaskListUseCase,
                    getSettingsUseCase = appContainer.getSettingsUseCase,
                    reorderTasksUseCase = appContainer.reorderTasksUseCase
                )
            )
            listHomeViewModel.setFilter(TaskFilter.ByList(listId, listName))

            HomeScreen(
                viewModel = listHomeViewModel,
                onNavigateToTaskDetail = { taskId ->
                    if (taskId != null) {
                        navController.navigate("task_detail?taskId=$taskId")
                    } else {
                        navController.navigate("task_detail")
                    }
                },
                onNavigateToSearch = {
                    navController.navigate("search")
                },
                onNavigateToSettings = {
                    navController.navigate("settings")
                },
                onNavigateToLists = {
                    navController.navigate("lists")
                },
                onNavigateToFocus = {
                    navController.navigate("focus")
                },
                onTabSelected = { tab ->
                    navigateToTab(tab)
                }
            )
        }

        composable(
            route = "task_detail?taskId={taskId}",
            arguments = listOf(
                navArgument("taskId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val taskIdArg = backStackEntry.arguments?.getLong("taskId") ?: -1L
            val taskId = if (taskIdArg > 0) taskIdArg else null

            val taskDetailViewModel: TaskDetailViewModel = viewModel(
                key = "task_detail_${taskId ?: "new"}",
                factory = TaskDetailViewModel.Factory(
                    taskId = taskId,
                    getTaskByIdUseCase = appContainer.getTaskByIdUseCase,
                    getSubtasksUseCase = appContainer.getSubtasksUseCase,
                    getTaskListsUseCase = appContainer.getTaskListsUseCase,
                    createTaskUseCase = appContainer.createTaskUseCase,
                    updateTaskUseCase = appContainer.updateTaskUseCase,
                    deleteTaskUseCase = appContainer.deleteTaskUseCase,
                    toggleTaskCompletionUseCase = appContainer.toggleTaskCompletionUseCase,
                    reminderScheduler = appContainer.reminderScheduler,
                    aiTaskAssistantUseCase = appContainer.aiTaskAssistantUseCase,
                    getAIConfigUseCase = appContainer.getAIConfigUseCase
                )
            )

            TaskDetailScreen(
                viewModel = taskDetailViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onStartFocus = { taskId, taskTitle, durationMins ->
                    navController.navigate("focus?taskId=$taskId&taskTitle=$taskTitle&durationMinutes=${durationMins ?: 25}")
                }
            )
        }

        composable("dashboard") {
            val dashboardViewModel: DashboardViewModel = viewModel(
                factory = DashboardViewModel.Factory(
                    getProductivityStatsUseCase = appContainer.getProductivityStatsUseCase,
                    aiProductivityUseCase = appContainer.aiProductivityUseCase,
                    getAIConfigUseCase = appContainer.getAIConfigUseCase
                )
            )

            DashboardScreen(
                viewModel = dashboardViewModel,
                onTabSelected = { tab -> navigateToTab(tab) },
                onStartFocus = {
                    navController.navigate("focus")
                }
            )
        }

        composable(
            route = "focus?taskId={taskId}&taskTitle={taskTitle}&durationMinutes={durationMinutes}",
            arguments = listOf(
                navArgument("taskId") {
                    type = NavType.LongType
                    defaultValue = -1L
                },
                navArgument("taskTitle") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("durationMinutes") {
                    type = NavType.IntType
                    defaultValue = 25
                }
            )
        ) { backStackEntry ->
            val taskIdArg = backStackEntry.arguments?.getLong("taskId") ?: -1L
            val taskId = if (taskIdArg > 0) taskIdArg else null
            val taskTitleArg = backStackEntry.arguments?.getString("taskTitle") ?: ""
            val taskTitle = taskTitleArg.ifEmpty { null }
            val durationMinutes = backStackEntry.arguments?.getInt("durationMinutes") ?: 25

            val focusViewModel: FocusViewModel = viewModel(
                key = "focus_${taskId ?: "quick"}",
                factory = FocusViewModel.Factory(
                    taskId = taskId,
                    taskTitle = taskTitle,
                    initialDurationMinutes = durationMinutes,
                    getTaskByIdUseCase = appContainer.getTaskByIdUseCase,
                    saveFocusSessionUseCase = appContainer.saveFocusSessionUseCase,
                    toggleTaskCompletionUseCase = appContainer.toggleTaskCompletionUseCase
                )
            )

            val context = LocalContext.current
            FocusScreen(
                viewModel = focusViewModel,
                onNavigateBack = {
                    val activity = context as? Activity
                    if (activity != null) {
                        appContainer.interstitialAdManager.showAdIfAvailable(activity) {
                            navController.popBackStack()
                        }
                    } else {
                        navController.popBackStack()
                    }
                }
            )
        }

        composable("search") {
            val searchViewModel: SearchViewModel = viewModel(
                factory = SearchViewModel.Factory(
                    searchTasksUseCase = appContainer.searchTasksUseCase,
                    toggleTaskCompletionUseCase = appContainer.toggleTaskCompletionUseCase,
                    toggleTaskStarUseCase = appContainer.toggleTaskStarUseCase
                )
            )

            SearchScreen(
                viewModel = searchViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToTaskDetail = { taskId ->
                    navController.navigate("task_detail?taskId=$taskId")
                }
            )
        }

        composable("settings") {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.Factory(
                    getSettingsUseCase = appContainer.getSettingsUseCase,
                    updateSettingsUseCase = appContainer.updateSettingsUseCase,
                    taskRepository = appContainer.taskRepository,
                    getAIConfigUseCase = appContainer.getAIConfigUseCase,
                    updateAIConfigUseCase = appContainer.updateAIConfigUseCase,
                    manageAIKeyUseCase = appContainer.manageAIKeyUseCase,
                    validateAIConnectionUseCase = appContainer.validateAIConnectionUseCase
                )
            )

            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onTabSelected = { tab ->
                    navigateToTab(tab)
                }
            )
        }
    }
}
