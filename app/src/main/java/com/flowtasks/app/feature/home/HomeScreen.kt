package com.flowtasks.app.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flowtasks.app.core.designsystem.component.EmptyStateView
import com.flowtasks.app.core.designsystem.component.FlowTasksBottomNavBar
import com.flowtasks.app.core.designsystem.component.NavigationTab
import com.flowtasks.app.core.designsystem.component.TaskItemCard
import com.flowtasks.app.domain.model.Task
import com.flowtasks.app.domain.model.TaskFilter
import com.flowtasks.app.domain.model.TaskSortOrder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToTaskDetail: (taskId: Long?) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToLists: () -> Unit,
    onTabSelected: (NavigationTab) -> Unit,
    onNavigateToFocus: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showSortMenu by remember { mutableStateOf(false) }
    var showListMenu by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.dismissError()
        }
    }

    val selectedTab = when (uiState.currentFilter) {
        is TaskFilter.Starred -> NavigationTab.STARRED
        else -> NavigationTab.TASKS
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Flow Tasks",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = when (val filter = uiState.currentFilter) {
                                is TaskFilter.All -> "${uiState.tasks.size} tasks"
                                is TaskFilter.Today -> "Today's Tasks (${uiState.tasks.size})"
                                is TaskFilter.Starred -> "Starred (${uiState.tasks.size})"
                                is TaskFilter.Overdue -> "Overdue (${uiState.tasks.size})"
                                is TaskFilter.Completed -> "Completed (${uiState.tasks.size})"
                                is TaskFilter.ByList -> "${filter.listName} (${uiState.tasks.size})"
                                is TaskFilter.ByProject -> "${filter.projectTitle} (${uiState.tasks.size})"
                                is TaskFilter.ByGoal -> "${filter.goalTitle} (${uiState.tasks.size})"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    if (onNavigateToFocus != null) {
                        IconButton(
                            onClick = onNavigateToFocus,
                            modifier = Modifier.testTag("home_focus_icon_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.HourglassEmpty,
                                contentDescription = "Focus Mode",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    IconButton(
                        onClick = onNavigateToSearch,
                        modifier = Modifier.testTag("search_icon_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Tasks"
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showSortMenu = true },
                            modifier = Modifier.testTag("sort_icon_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = "Sort Tasks"
                            )
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Default Order") },
                                onClick = {
                                    viewModel.setSortOrder(TaskSortOrder.DEFAULT_SORT_ORDER)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Due Date") },
                                onClick = {
                                    viewModel.setSortOrder(TaskSortOrder.DUE_DATE_ASC)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Priority") },
                                onClick = {
                                    viewModel.setSortOrder(TaskSortOrder.PRIORITY_DESC)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Created Date") },
                                onClick = {
                                    viewModel.setSortOrder(TaskSortOrder.CREATED_AT_DESC)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Title (A-Z)") },
                                onClick = {
                                    viewModel.setSortOrder(TaskSortOrder.TITLE_ASC)
                                    showSortMenu = false
                                }
                            )
                        }
                    }

                    // Extra options for custom list
                    if (uiState.currentFilter is TaskFilter.ByList) {
                        val currentList = uiState.currentFilter as TaskFilter.ByList
                        Box {
                            IconButton(
                                onClick = { showListMenu = true },
                                modifier = Modifier.testTag("list_options_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "List Options"
                                )
                            }
                            DropdownMenu(
                                expanded = showListMenu,
                                onDismissRequest = { showListMenu = false }
                            ) {
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete List",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    text = {
                                        Text(
                                            text = "Delete List",
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        viewModel.deleteList(currentList.listId)
                                        showListMenu = false
                                    }
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("settings_icon_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            FlowTasksBottomNavBar(
                selectedTab = selectedTab,
                onTabSelected = onTabSelected
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToTaskDetail(null) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_task_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Task"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Filter Chips Scrollable Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // All Filter
                FilterChip(
                    selected = uiState.currentFilter is TaskFilter.All,
                    onClick = { viewModel.setFilter(TaskFilter.All) },
                    label = { Text("All") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.testTag("filter_all")
                )

                // Today Filter
                FilterChip(
                    selected = uiState.currentFilter is TaskFilter.Today,
                    onClick = { viewModel.setFilter(TaskFilter.Today) },
                    label = { Text("Today") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Today,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("filter_today")
                )

                // Starred Filter
                FilterChip(
                    selected = uiState.currentFilter is TaskFilter.Starred,
                    onClick = { viewModel.setFilter(TaskFilter.Starred) },
                    label = { Text("Starred") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("filter_starred")
                )

                // Overdue Filter
                FilterChip(
                    selected = uiState.currentFilter is TaskFilter.Overdue,
                    onClick = { viewModel.setFilter(TaskFilter.Overdue) },
                    label = { Text("Overdue") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("filter_overdue")
                )

                // Completed Filter
                FilterChip(
                    selected = uiState.currentFilter is TaskFilter.Completed,
                    onClick = { viewModel.setFilter(TaskFilter.Completed) },
                    label = { Text("Completed") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("filter_completed")
                )

                // User Created Lists
                uiState.taskLists.forEach { list ->
                    val isSelected = (uiState.currentFilter as? TaskFilter.ByList)?.listId == list.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setFilter(TaskFilter.ByList(list.id, list.name)) },
                        label = { Text(list.name) },
                        modifier = Modifier.testTag("filter_list_${list.id}")
                    )
                }

                // Add List Action Button
                FilterChip(
                    selected = false,
                    onClick = { viewModel.showCreateListDialog(true) },
                    label = { Text("+ New List") },
                    modifier = Modifier.testTag("add_list_chip")
                )
            }

            // Task List or Clean Empty State
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.testTag("home_loading_indicator"))
                }
            } else if (uiState.tasks.isEmpty()) {
                val (emptyIcon, emptyTitle, emptySubtitle) = when (val filter = uiState.currentFilter) {
                    is TaskFilter.All -> Triple(
                        Icons.Default.TaskAlt,
                        "No tasks yet",
                        "Tap the + button to create your first task."
                    )
                    is TaskFilter.Today -> Triple(
                        Icons.Default.Today,
                        "No tasks due today",
                        "Enjoy your clear schedule or add a task for today."
                    )
                    is TaskFilter.Starred -> Triple(
                        Icons.Default.Star,
                        "No starred tasks",
                        "Star important tasks to keep them easily accessible."
                    )
                    is TaskFilter.Overdue -> Triple(
                        Icons.Default.CheckCircle,
                        "No overdue tasks",
                        "You're all caught up with your deadlines."
                    )
                    is TaskFilter.Completed -> Triple(
                        Icons.Default.CheckCircle,
                        "No completed tasks",
                        "Completed tasks will be recorded here."
                    )
                    is TaskFilter.ByList -> Triple(
                        Icons.Default.TaskAlt,
                        "No tasks in ${filter.listName}",
                        "Add tasks to this list using the + button."
                    )
                    is TaskFilter.ByProject -> Triple(
                        Icons.Default.TaskAlt,
                        "No tasks in ${filter.projectTitle}",
                        "Add tasks to this project using the + button."
                    )
                    is TaskFilter.ByGoal -> Triple(
                        Icons.Default.TaskAlt,
                        "No tasks in ${filter.goalTitle}",
                        "Add tasks linked to this goal using the + button."
                    )
                }

                EmptyStateView(
                    icon = emptyIcon,
                    title = emptyTitle,
                    subtitle = emptySubtitle,
                    actionLabel = "Create First Task",
                    onActionClick = { onNavigateToTaskDetail(null) },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("tasks_lazy_column"),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = uiState.tasks,
                        key = { it.id }
                    ) { task ->
                        val listName = task.listId?.let { listId ->
                            uiState.taskLists.firstOrNull { it.id == listId }?.name
                        }

                        TaskItemCard(
                            task = task,
                            listName = listName,
                            onTaskClick = { onNavigateToTaskDetail(task.id) },
                            onToggleCompletion = { t, checked -> viewModel.toggleTaskCompletion(t, checked) },
                            onToggleStar = { t, starred -> viewModel.toggleTaskStar(t, starred) }
                        )
                    }
                }
            }
        }
    }

    // Quick Add Bottom Sheet
    if (uiState.isQuickAddSheetOpen) {
        val currentListId = (uiState.currentFilter as? TaskFilter.ByList)?.listId
        QuickAddTaskSheet(
            taskLists = uiState.taskLists,
            currentListId = currentListId,
            onDismiss = { viewModel.showQuickAddSheet(false) },
            onCreateTask = { title, description, dueDate, dueTime, priority, listId ->
                viewModel.quickCreateTask(
                    title = title,
                    description = description,
                    dueDate = dueDate,
                    dueTime = dueTime,
                    priority = priority,
                    listId = listId
                )
            }
        )
    }

    // Create List Dialog
    if (uiState.isCreateListDialogOpen) {
        CreateListDialog(
            onDismiss = { viewModel.showCreateListDialog(false) },
            onConfirm = { name, colorHex ->
                viewModel.createList(name, colorHex)
            }
        )
    }
}
