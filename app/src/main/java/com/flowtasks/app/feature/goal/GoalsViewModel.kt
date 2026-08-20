package com.flowtasks.app.feature.goal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.flowtasks.app.domain.model.Goal
import com.flowtasks.app.domain.model.GoalStatus
import com.flowtasks.app.domain.model.Project
import com.flowtasks.app.domain.usecase.CreateGoalUseCase
import com.flowtasks.app.domain.usecase.CreateProjectUseCase
import com.flowtasks.app.domain.usecase.DeleteGoalUseCase
import com.flowtasks.app.domain.usecase.DeleteProjectUseCase
import com.flowtasks.app.domain.usecase.GetGoalsUseCase
import com.flowtasks.app.domain.usecase.GetProjectsUseCase
import com.flowtasks.app.domain.usecase.ToggleGoalStatusUseCase
import com.flowtasks.app.domain.usecase.ToggleProjectStatusUseCase
import com.flowtasks.app.domain.usecase.UpdateGoalUseCase
import com.flowtasks.app.domain.usecase.UpdateProjectUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class GoalTabFilter {
    GOALS,
    PROJECTS
}

enum class StatusFilter {
    ALL,
    ACTIVE,
    COMPLETED
}

private data class GoalSheetState(
    val currentTab: GoalTabFilter,
    val statusFilter: StatusFilter,
    val isCreateGoalSheetOpen: Boolean,
    val isCreateProjectSheetOpen: Boolean,
    val selectedGoalForNewProject: Long?
)

private data class GoalEditDeleteState(
    val editingGoal: Goal?,
    val editingProject: Project?,
    val goalToDelete: Goal?,
    val projectToDelete: Project?
)

data class GoalsUiState(
    val goals: List<Goal> = emptyList(),
    val projects: List<Project> = emptyList(),
    val currentTab: GoalTabFilter = GoalTabFilter.GOALS,
    val statusFilter: StatusFilter = StatusFilter.ACTIVE,
    val isCreateGoalSheetOpen: Boolean = false,
    val isCreateProjectSheetOpen: Boolean = false,
    val editingGoal: Goal? = null,
    val editingProject: Project? = null,
    val goalToDelete: Goal? = null,
    val projectToDelete: Project? = null,
    val selectedGoalForNewProject: Long? = null
)

class GoalsViewModel(
    private val getGoalsUseCase: GetGoalsUseCase,
    private val createGoalUseCase: CreateGoalUseCase,
    private val updateGoalUseCase: UpdateGoalUseCase,
    private val deleteGoalUseCase: DeleteGoalUseCase,
    private val toggleGoalStatusUseCase: ToggleGoalStatusUseCase,
    private val getProjectsUseCase: GetProjectsUseCase,
    private val createProjectUseCase: CreateProjectUseCase,
    private val updateProjectUseCase: UpdateProjectUseCase,
    private val deleteProjectUseCase: DeleteProjectUseCase,
    private val toggleProjectStatusUseCase: ToggleProjectStatusUseCase
) : ViewModel() {

    private val _currentTab = MutableStateFlow(GoalTabFilter.GOALS)
    private val _statusFilter = MutableStateFlow(StatusFilter.ACTIVE)
    private val _isCreateGoalSheetOpen = MutableStateFlow(false)
    private val _isCreateProjectSheetOpen = MutableStateFlow(false)
    private val _editingGoal = MutableStateFlow<Goal?>(null)
    private val _editingProject = MutableStateFlow<Project?>(null)
    private val _goalToDelete = MutableStateFlow<Goal?>(null)
    private val _projectToDelete = MutableStateFlow<Project?>(null)
    private val _selectedGoalForNewProject = MutableStateFlow<Long?>(null)

    private val _sheetState = combine(
        _currentTab,
        _statusFilter,
        _isCreateGoalSheetOpen,
        _isCreateProjectSheetOpen,
        _selectedGoalForNewProject
    ) { tab, filter, isGoalOpen, isProjectOpen, selectedGoal ->
        GoalSheetState(
            currentTab = tab,
            statusFilter = filter,
            isCreateGoalSheetOpen = isGoalOpen,
            isCreateProjectSheetOpen = isProjectOpen,
            selectedGoalForNewProject = selectedGoal
        )
    }

    private val _editDeleteState = combine(
        _editingGoal,
        _editingProject,
        _goalToDelete,
        _projectToDelete
    ) { editGoal, editProject, goalDelete, projDelete ->
        GoalEditDeleteState(
            editingGoal = editGoal,
            editingProject = editProject,
            goalToDelete = goalDelete,
            projectToDelete = projDelete
        )
    }

    val uiState: StateFlow<GoalsUiState> = combine(
        getGoalsUseCase(includeArchived = true),
        getProjectsUseCase(includeArchived = true),
        _sheetState,
        _editDeleteState
    ) { goals, projects, sheet, editDelete ->
        val filteredGoals = when (sheet.statusFilter) {
            StatusFilter.ALL -> goals
            StatusFilter.ACTIVE -> goals.filter { it.status == GoalStatus.ACTIVE }
            StatusFilter.COMPLETED -> goals.filter { it.status == GoalStatus.COMPLETED }
        }

        val filteredProjects = when (sheet.statusFilter) {
            StatusFilter.ALL -> projects
            StatusFilter.ACTIVE -> projects.filter { it.status != com.flowtasks.app.domain.model.ProjectStatus.COMPLETED }
            StatusFilter.COMPLETED -> projects.filter { it.status == com.flowtasks.app.domain.model.ProjectStatus.COMPLETED }
        }

        GoalsUiState(
            goals = filteredGoals,
            projects = filteredProjects,
            currentTab = sheet.currentTab,
            statusFilter = sheet.statusFilter,
            isCreateGoalSheetOpen = sheet.isCreateGoalSheetOpen,
            isCreateProjectSheetOpen = sheet.isCreateProjectSheetOpen,
            editingGoal = editDelete.editingGoal,
            editingProject = editDelete.editingProject,
            goalToDelete = editDelete.goalToDelete,
            projectToDelete = editDelete.projectToDelete,
            selectedGoalForNewProject = sheet.selectedGoalForNewProject
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GoalsUiState()
    )

    fun selectTab(tab: GoalTabFilter) {
        _currentTab.value = tab
    }

    fun selectStatusFilter(filter: StatusFilter) {
        _statusFilter.value = filter
    }

    fun openCreateGoalSheet() {
        _editingGoal.value = null
        _isCreateGoalSheetOpen.value = true
    }

    fun openEditGoalSheet(goal: Goal) {
        _editingGoal.value = goal
        _isCreateGoalSheetOpen.value = true
    }

    fun closeGoalSheet() {
        _isCreateGoalSheetOpen.value = false
        _editingGoal.value = null
    }

    fun saveGoal(
        title: String,
        description: String,
        targetDate: Long?,
        colorHex: String
    ) {
        viewModelScope.launch {
            val editing = _editingGoal.value
            if (editing != null) {
                updateGoalUseCase(
                    editing.copy(
                        title = title,
                        description = description,
                        targetDate = targetDate,
                        colorHex = colorHex
                    )
                )
            } else {
                createGoalUseCase(
                    title = title,
                    description = description,
                    targetDate = targetDate,
                    colorHex = colorHex
                )
            }
            closeGoalSheet()
        }
    }

    fun toggleGoalCompletion(goalId: Long, isCompleted: Boolean) {
        viewModelScope.launch {
            toggleGoalStatusUseCase(goalId, isCompleted)
        }
    }

    fun confirmDeleteGoal(goal: Goal) {
        _goalToDelete.value = goal
    }

    fun dismissDeleteGoalDialog() {
        _goalToDelete.value = null
    }

    fun deleteGoalConfirmed() {
        val goal = _goalToDelete.value ?: return
        viewModelScope.launch {
            deleteGoalUseCase(goal.id)
            _goalToDelete.value = null
        }
    }

    fun openCreateProjectSheet(goalId: Long? = null) {
        _selectedGoalForNewProject.value = goalId
        _editingProject.value = null
        _isCreateProjectSheetOpen.value = true
    }

    fun openEditProjectSheet(project: Project) {
        _editingProject.value = project
        _selectedGoalForNewProject.value = project.goalId
        _isCreateProjectSheetOpen.value = true
    }

    fun closeProjectSheet() {
        _isCreateProjectSheetOpen.value = false
        _editingProject.value = null
        _selectedGoalForNewProject.value = null
    }

    fun saveProject(
        title: String,
        goalId: Long?,
        description: String,
        targetDate: Long?,
        colorHex: String
    ) {
        viewModelScope.launch {
            val editing = _editingProject.value
            if (editing != null) {
                updateProjectUseCase(
                    editing.copy(
                        title = title,
                        goalId = goalId,
                        description = description,
                        targetDate = targetDate,
                        colorHex = colorHex
                    )
                )
            } else {
                createProjectUseCase(
                    title = title,
                    goalId = goalId,
                    description = description,
                    targetDate = targetDate,
                    colorHex = colorHex
                )
            }
            closeProjectSheet()
        }
    }

    fun toggleProjectCompletion(projectId: Long, isCompleted: Boolean) {
        viewModelScope.launch {
            toggleProjectStatusUseCase(projectId, isCompleted)
        }
    }

    fun confirmDeleteProject(project: Project) {
        _projectToDelete.value = project
    }

    fun dismissDeleteProjectDialog() {
        _projectToDelete.value = null
    }

    fun deleteProjectConfirmed() {
        val project = _projectToDelete.value ?: return
        viewModelScope.launch {
            deleteProjectUseCase(project.id)
            _projectToDelete.value = null
        }
    }

    class Factory(
        private val getGoalsUseCase: GetGoalsUseCase,
        private val createGoalUseCase: CreateGoalUseCase,
        private val updateGoalUseCase: UpdateGoalUseCase,
        private val deleteGoalUseCase: DeleteGoalUseCase,
        private val toggleGoalStatusUseCase: ToggleGoalStatusUseCase,
        private val getProjectsUseCase: GetProjectsUseCase,
        private val createProjectUseCase: CreateProjectUseCase,
        private val updateProjectUseCase: UpdateProjectUseCase,
        private val deleteProjectUseCase: DeleteProjectUseCase,
        private val toggleProjectStatusUseCase: ToggleProjectStatusUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GoalsViewModel(
                getGoalsUseCase = getGoalsUseCase,
                createGoalUseCase = createGoalUseCase,
                updateGoalUseCase = updateGoalUseCase,
                deleteGoalUseCase = deleteGoalUseCase,
                toggleGoalStatusUseCase = toggleGoalStatusUseCase,
                getProjectsUseCase = getProjectsUseCase,
                createProjectUseCase = createProjectUseCase,
                updateProjectUseCase = updateProjectUseCase,
                deleteProjectUseCase = deleteProjectUseCase,
                toggleProjectStatusUseCase = toggleProjectStatusUseCase
            ) as T
        }
    }
}
