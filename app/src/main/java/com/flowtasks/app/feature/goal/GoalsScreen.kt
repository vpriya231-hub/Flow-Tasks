package com.flowtasks.app.feature.goal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flowtasks.app.core.designsystem.component.EmptyStateView
import com.flowtasks.app.core.designsystem.component.FlowTasksBottomNavBar
import com.flowtasks.app.core.designsystem.component.NavigationTab
import com.flowtasks.app.core.utils.DateUtils
import com.flowtasks.app.domain.model.Goal
import com.flowtasks.app.domain.model.GoalStatus
import com.flowtasks.app.domain.model.Project
import com.flowtasks.app.domain.model.ProjectStatus

private val PRESET_COLORS = listOf(
    "#4F46E5", // Indigo
    "#0EA5E9", // Sky
    "#10B981", // Emerald
    "#F59E0B", // Amber
    "#EF4444", // Red
    "#8B5CF6", // Purple
    "#EC4899", // Pink
    "#14B8A6"  // Teal
)

fun parseColorSafe(hex: String, defaultColor: Color = Color(0xFF4F46E5)): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        defaultColor
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    viewModel: GoalsViewModel,
    onTabSelected: (NavigationTab) -> Unit,
    onNavigateToProjectTasks: (Long, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Goals & Projects",
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        },
        bottomBar = {
            FlowTasksBottomNavBar(
                selectedTab = NavigationTab.GOALS,
                onTabSelected = onTabSelected
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (state.currentTab == GoalTabFilter.GOALS) {
                        viewModel.openCreateGoalSheet()
                    } else {
                        viewModel.openCreateProjectSheet()
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("goals_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = if (state.currentTab == GoalTabFilter.GOALS) "Add Goal" else "Add Project"
                )
            }
        },
        modifier = modifier.testTag("goals_screen")
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row: Goals vs Projects
            TabRow(
                selectedTabIndex = state.currentTab.ordinal,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = state.currentTab == GoalTabFilter.GOALS,
                    onClick = { viewModel.selectTab(GoalTabFilter.GOALS) },
                    text = { Text("Goals (${state.goals.size})") },
                    icon = { Icon(Icons.Filled.Flag, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_goals")
                )
                Tab(
                    selected = state.currentTab == GoalTabFilter.PROJECTS,
                    onClick = { viewModel.selectTab(GoalTabFilter.PROJECTS) },
                    text = { Text("Projects (${state.projects.size})") },
                    icon = { Icon(Icons.Filled.Folder, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_projects")
                )
            }

            // Status Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = state.statusFilter == filter,
                        onClick = { viewModel.selectStatusFilter(filter) },
                        label = {
                            Text(
                                when (filter) {
                                    StatusFilter.ALL -> "All"
                                    StatusFilter.ACTIVE -> "Active"
                                    StatusFilter.COMPLETED -> "Completed"
                                }
                            )
                        },
                        modifier = Modifier.testTag("filter_${filter.name.lowercase()}")
                    )
                }
            }

            // Content List
            if (state.currentTab == GoalTabFilter.GOALS) {
                if (state.goals.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Outlined.Flag,
                        title = "No Goals Yet",
                        subtitle = "Create high-level goals to guide your projects and daily tasks.",
                        actionLabel = "Create Goal",
                        onActionClick = { viewModel.openCreateGoalSheet() },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("goals_list")
                    ) {
                        items(state.goals, key = { it.id }) { goal ->
                            val linkedProjects = state.projects.filter { it.goalId == goal.id }
                            GoalCardItem(
                                goal = goal,
                                linkedProjects = linkedProjects,
                                onToggleCompletion = { isCompleted ->
                                    viewModel.toggleGoalCompletion(goal.id, isCompleted)
                                },
                                onEditGoal = { viewModel.openEditGoalSheet(goal) },
                                onDeleteGoal = { viewModel.confirmDeleteGoal(goal) },
                                onAddProject = { viewModel.openCreateProjectSheet(goal.id) },
                                onProjectClick = { project ->
                                    onNavigateToProjectTasks(project.id, project.title)
                                },
                                onEditProject = { project ->
                                    viewModel.openEditProjectSheet(project)
                                },
                                onDeleteProject = { project ->
                                    viewModel.confirmDeleteProject(project)
                                }
                            )
                        }
                    }
                }
            } else {
                if (state.projects.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Outlined.Folder,
                        title = "No Projects Yet",
                        subtitle = "Organize tasks under structured projects linked to your goals.",
                        actionLabel = "Create Project",
                        onActionClick = { viewModel.openCreateProjectSheet() },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("projects_list")
                    ) {
                        items(state.projects, key = { it.id }) { project ->
                            ProjectCardItem(
                                project = project,
                                onToggleCompletion = { isCompleted ->
                                    viewModel.toggleProjectCompletion(project.id, isCompleted)
                                },
                                onClick = {
                                    onNavigateToProjectTasks(project.id, project.title)
                                },
                                onEditProject = { viewModel.openEditProjectSheet(project) },
                                onDeleteProject = { viewModel.confirmDeleteProject(project) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Goal Create / Edit Sheet
    if (state.isCreateGoalSheetOpen) {
        GoalBottomSheet(
            editingGoal = state.editingGoal,
            onDismiss = { viewModel.closeGoalSheet() },
            onSave = { title, description, targetDate, colorHex ->
                viewModel.saveGoal(title, description, targetDate, colorHex)
            }
        )
    }

    // Project Create / Edit Sheet
    if (state.isCreateProjectSheetOpen) {
        ProjectBottomSheet(
            editingProject = state.editingProject,
            goals = state.goals,
            selectedGoalId = state.selectedGoalForNewProject,
            onDismiss = { viewModel.closeProjectSheet() },
            onSave = { title, goalId, description, targetDate, colorHex ->
                viewModel.saveProject(title, goalId, description, targetDate, colorHex)
            }
        )
    }

    // Delete Goal Confirmation Dialog (Safe Deletion)
    if (state.goalToDelete != null) {
        val goal = state.goalToDelete!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteGoalDialog() },
            title = { Text("Delete Goal?") },
            text = {
                Text(
                    "Are you sure you want to delete \"${goal.title}\"?\n\nNote: All associated projects and tasks will remain safe and will simply be unlinked."
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteGoalConfirmed() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_goal_button")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissDeleteGoalDialog() },
                    modifier = Modifier.testTag("cancel_delete_goal_button")
                ) {
                    Text("Cancel")
                }
            },
            modifier = Modifier.testTag("delete_goal_dialog")
        )
    }

    // Delete Project Confirmation Dialog (Safe Deletion)
    if (state.projectToDelete != null) {
        val project = state.projectToDelete!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteProjectDialog() },
            title = { Text("Delete Project?") },
            text = {
                Text(
                    "Are you sure you want to delete \"${project.title}\"?\n\nNote: All tasks in this project will remain safe in your tasks list."
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteProjectConfirmed() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_project_button")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissDeleteProjectDialog() },
                    modifier = Modifier.testTag("cancel_delete_project_button")
                ) {
                    Text("Cancel")
                }
            },
            modifier = Modifier.testTag("delete_project_dialog")
        )
    }
}

@Composable
fun GoalCardItem(
    goal: Goal,
    linkedProjects: List<Project>,
    onToggleCompletion: (Boolean) -> Unit,
    onEditGoal: () -> Unit,
    onDeleteGoal: () -> Unit,
    onAddProject: () -> Unit,
    onProjectClick: (Project) -> Unit,
    onEditProject: (Project) -> Unit = {},
    onDeleteProject: (Project) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    val isCompleted = goal.status == GoalStatus.COMPLETED
    val accentColor = parseColorSafe(goal.colorHex)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCompleted) 0.dp else 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("goal_card_${goal.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Color Accent Bar
                Box(
                    modifier = Modifier
                        .size(width = 4.dp, height = 28.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(accentColor)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (goal.description.isNotBlank()) {
                        Text(
                            text = goal.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                // Completion Toggle
                IconButton(
                    onClick = { onToggleCompletion(!isCompleted) },
                    modifier = Modifier.testTag("goal_complete_toggle_${goal.id}")
                ) {
                    Icon(
                        imageVector = if (isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircleOutline,
                        contentDescription = if (isCompleted) "Completed" else "Mark Complete",
                        tint = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }

                // Overflow Menu
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Goal options")
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Goal") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                onEditGoal()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Add Project") },
                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                onAddProject()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Goal", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                menuOpen = false
                                onDeleteGoal()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Metadata row: Projects count & Target Date
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Project count badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${linkedProjects.size} Projects",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Target date badge
                if (goal.targetDate != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = DateUtils.formatRelativeDueDate(goal.targetDate, null),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Expand / Collapse projects button
                if (linkedProjects.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { expanded = !expanded }
                            .padding(4.dp)
                    ) {
                        Text(
                            text = if (expanded) "Hide Projects" else "View Projects",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Linked Projects list dropdown
            AnimatedVisibility(visible = expanded && linkedProjects.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
                    Text(
                        text = "Projects under this Goal:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    linkedProjects.forEach { project ->
                        var projectMenuOpen by remember { mutableStateOf(false) }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onProjectClick(project) }
                                .padding(vertical = 6.dp, horizontal = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(parseColorSafe(project.colorHex))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = project.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "Tasks →",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Box {
                                IconButton(
                                    onClick = { projectMenuOpen = true },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Project options",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                DropdownMenu(
                                    expanded = projectMenuOpen,
                                    onDismissRequest = { projectMenuOpen = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Open Tasks") },
                                        onClick = {
                                            projectMenuOpen = false
                                            onProjectClick(project)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Edit Project") },
                                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                        onClick = {
                                            projectMenuOpen = false
                                            onEditProject(project)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete Project", color = MaterialTheme.colorScheme.error) },
                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                        onClick = {
                                            projectMenuOpen = false
                                            onDeleteProject(project)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProjectCardItem(
    project: Project,
    onToggleCompletion: (Boolean) -> Unit,
    onClick: () -> Unit,
    onEditProject: () -> Unit,
    onDeleteProject: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuOpen by remember { mutableStateOf(false) }
    val isCompleted = project.status == ProjectStatus.COMPLETED
    val accentColor = parseColorSafe(project.colorHex)

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCompleted) 0.dp else 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("project_card_${project.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Color Accent Bar
                Box(
                    modifier = Modifier
                        .size(width = 4.dp, height = 28.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(accentColor)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = project.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (project.description.isNotBlank()) {
                        Text(
                            text = project.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                // Completion Toggle
                IconButton(
                    onClick = { onToggleCompletion(!isCompleted) },
                    modifier = Modifier.testTag("project_complete_toggle_${project.id}")
                ) {
                    Icon(
                        imageVector = if (isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircleOutline,
                        contentDescription = if (isCompleted) "Completed" else "Mark Complete",
                        tint = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }

                // Overflow Menu
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Project options")
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Project") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                onEditProject()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Project", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                menuOpen = false
                                onDeleteProject()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Metadata row: Goal connection & Target Date
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Goal badge if linked
                if (!project.goalTitle.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = project.goalTitle,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Target Date
                if (project.targetDate != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = DateUtils.formatRelativeDueDate(project.targetDate, null),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "Open Tasks →",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalBottomSheet(
    editingGoal: Goal?,
    onDismiss: () -> Unit,
    onSave: (String, String, Long?, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf(editingGoal?.title ?: "") }
    var description by remember { mutableStateOf(editingGoal?.description ?: "") }
    var selectedColor by remember { mutableStateOf(editingGoal?.colorHex ?: PRESET_COLORS.first()) }
    var targetDate by remember { mutableStateOf(editingGoal?.targetDate) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.testTag("goal_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = if (editingGoal != null) "Edit Goal" else "Create New Goal",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Goal Title") },
                placeholder = { Text("e.g. Master Android Architecture") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("goal_title_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (Optional)") },
                placeholder = { Text("e.g. Build modular projects and master Room persistence") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("goal_description_input")
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Target Date Selector
            var showDatePicker by remember { mutableStateOf(false) }
            Text("Target Date (Optional)", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("goal_target_date_picker_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Pick Target Date",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (targetDate != null) DateUtils.formatShortDate(targetDate!!) else "Set Target Date"
                    )
                }

                if (targetDate != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { targetDate = null },
                        modifier = Modifier.testTag("clear_goal_target_date_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear Target Date",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (showDatePicker) {
                val initialDate = targetDate?.let { DateUtils.localDateMillisToUtcMidnight(it) } ?: System.currentTimeMillis()
                val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDate)
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val localStartOfDay = datePickerState.selectedDateMillis?.let {
                                    DateUtils.utcDateMillisToLocalStartOfDay(it)
                                }
                                targetDate = localStartOfDay
                                showDatePicker = false
                            }
                        ) {
                            Text("OK")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                targetDate = null
                                showDatePicker = false
                            }
                        ) {
                            Text("Clear")
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Color Theme", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                PRESET_COLORS.forEach { hex ->
                    val color = parseColorSafe(hex)
                    val isSelected = selectedColor.equals(hex, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color)
                            .clickable { selectedColor = hex }
                            .padding(2.dp)
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.4f))
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            onSave(title, description, targetDate, selectedColor)
                        }
                    },
                    enabled = title.isNotBlank(),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("save_goal_button")
                ) {
                    Text("Save Goal")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectBottomSheet(
    editingProject: Project?,
    goals: List<Goal>,
    selectedGoalId: Long?,
    onDismiss: () -> Unit,
    onSave: (String, Long?, String, Long?, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf(editingProject?.title ?: "") }
    var description by remember { mutableStateOf(editingProject?.description ?: "") }
    var chosenGoalId by remember { mutableStateOf(editingProject?.goalId ?: selectedGoalId) }
    var selectedColor by remember { mutableStateOf(editingProject?.colorHex ?: PRESET_COLORS[1]) }
    var targetDate by remember { mutableStateOf(editingProject?.targetDate) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.testTag("project_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = if (editingProject != null) "Edit Project" else "Create New Project",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Project Title") },
                placeholder = { Text("e.g. Redesign Database Layer") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("project_title_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Goal Selection Chips
            if (goals.isNotEmpty()) {
                Text("Link to Goal (Optional)", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = chosenGoalId == null,
                        onClick = { chosenGoalId = null },
                        label = { Text("None") }
                    )
                    goals.take(4).forEach { goal ->
                        FilterChip(
                            selected = chosenGoalId == goal.id,
                            onClick = { chosenGoalId = goal.id },
                            label = { Text(goal.title, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (Optional)") },
                placeholder = { Text("e.g. Implement Room entities and migration scripts") },
                minLines = 2,
                maxLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("project_description_input")
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Target Date Selector
            var showDatePicker by remember { mutableStateOf(false) }
            Text("Target Date (Optional)", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("project_target_date_picker_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Pick Target Date",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (targetDate != null) DateUtils.formatShortDate(targetDate!!) else "Set Target Date"
                    )
                }

                if (targetDate != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { targetDate = null },
                        modifier = Modifier.testTag("clear_project_target_date_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear Target Date",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (showDatePicker) {
                val initialDate = targetDate?.let { DateUtils.localDateMillisToUtcMidnight(it) } ?: System.currentTimeMillis()
                val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDate)
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val localStartOfDay = datePickerState.selectedDateMillis?.let {
                                    DateUtils.utcDateMillisToLocalStartOfDay(it)
                                }
                                targetDate = localStartOfDay
                                showDatePicker = false
                            }
                        ) {
                            Text("OK")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                targetDate = null
                                showDatePicker = false
                            }
                        ) {
                            Text("Clear")
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Color Theme", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                PRESET_COLORS.forEach { hex ->
                    val color = parseColorSafe(hex)
                    val isSelected = selectedColor.equals(hex, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color)
                            .clickable { selectedColor = hex }
                            .padding(2.dp)
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.4f))
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            onSave(title, chosenGoalId, description, targetDate, selectedColor)
                        }
                    },
                    enabled = title.isNotBlank(),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("save_project_button")
                ) {
                    Text("Save Project")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
