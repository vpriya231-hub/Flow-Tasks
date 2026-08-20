package com.flowtasks.app.feature.task

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flowtasks.app.core.utils.DateUtils
import com.flowtasks.app.domain.model.RecurrenceRule
import com.flowtasks.app.domain.model.RecurrenceType
import com.flowtasks.app.domain.model.ReminderType
import com.flowtasks.app.domain.model.TaskPriority
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskDetailScreen(
    viewModel: TaskDetailViewModel,
    onNavigateBack: () -> Unit,
    onStartFocus: ((taskId: Long, taskTitle: String, durationMinutes: Int?) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var newSubtaskTitle by remember { mutableStateOf("") }
    var listDropdownExpanded by remember { mutableStateOf(false) }
    var reminderDropdownExpanded by remember { mutableStateOf(false) }
    var showPromptDialog by remember { mutableStateOf(false) }
    var promptInputText by remember { mutableStateOf("") }

    LaunchedEffect(uiState.isSaved, uiState.isDeleted) {
        if (uiState.isSaved || uiState.isDeleted) {
            onNavigateBack()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.dismissError()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("task_detail_screen"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.taskId == null || uiState.taskId == 0L) "New Task" else "Task Details",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Star button
                    IconButton(
                        onClick = { viewModel.toggleStarred() },
                        modifier = Modifier.testTag("detail_star_button")
                    ) {
                        Icon(
                            imageVector = if (uiState.isStarred) Icons.Filled.Star else Icons.Outlined.StarOutline,
                            contentDescription = if (uiState.isStarred) "Starred" else "Unstarred",
                            tint = if (uiState.isStarred) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }

                    // Delete button (only when editing existing task)
                    if (uiState.taskId != null && uiState.taskId!! > 0) {
                        IconButton(
                            onClick = { showDeleteConfirmDialog = true },
                            modifier = Modifier.testTag("delete_task_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Task",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    // Save Button
                    Button(
                        onClick = { viewModel.saveTask() },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("save_task_button")
                    ) {
                        Text("Save")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title & Completion Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    if (uiState.taskId != null && uiState.taskId!! > 0) {
                        Checkbox(
                            checked = uiState.isCompleted,
                            onCheckedChange = { viewModel.toggleCompletion() },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .testTag("detail_complete_checkbox")
                        )
                    }

                    OutlinedTextField(
                        value = uiState.title,
                        onValueChange = { viewModel.updateTitle(it) },
                        label = { Text("Task Title *") },
                        placeholder = { Text("Enter task title") },
                        singleLine = false,
                        maxLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("task_title_input")
                    )
                }

                // AI Assistant Actions & Status (when enabled)
                if (uiState.isAIEnabled) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "AI Assistant",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // AI Action Chips
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AssistChip(
                                onClick = {
                                    promptInputText = ""
                                    showPromptDialog = true
                                },
                                label = { Text("Draft with AI") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                enabled = !uiState.isAILoading,
                                modifier = Modifier.testTag("ai_draft_button")
                            )

                            AssistChip(
                                onClick = { viewModel.improveTaskWithAI() },
                                label = { Text("Improve") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.TipsAndUpdates,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                enabled = !uiState.isAILoading && uiState.title.isNotBlank(),
                                modifier = Modifier.testTag("ai_improve_button")
                            )

                            AssistChip(
                                onClick = { viewModel.generateSubtasksWithAI() },
                                label = { Text("Subtasks") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.FormatListNumbered,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                enabled = !uiState.isAILoading && uiState.title.isNotBlank(),
                                modifier = Modifier.testTag("ai_subtasks_button")
                            )

                            AssistChip(
                                onClick = { viewModel.suggestPriorityAndDurationWithAI() },
                                label = { Text("Suggest Priority") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Psychology,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                enabled = !uiState.isAILoading && uiState.title.isNotBlank(),
                                modifier = Modifier.testTag("ai_suggest_priority_button")
                            )
                        }

                        // AI Loading Indicator
                        if (uiState.isAILoading) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Text(
                                    text = uiState.aiActionMessage ?: "Working with AI...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // AI Proposal / Review Banner
                        if (uiState.aiSuccessMessage != null) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("ai_proposal_banner"),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = uiState.aiSuccessMessage ?: "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.dismissAIMessage() },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Dismiss",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Description
                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = { viewModel.updateDescription(it) },
                    label = { Text("Description") },
                    placeholder = { Text("Add details or context") },
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_description_input")
                )

                // List Selection Dropdown
                ExposedDropdownMenuBox(
                    expanded = listDropdownExpanded,
                    onExpandedChange = { listDropdownExpanded = !listDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val currentListName = uiState.taskLists.firstOrNull { it.id == uiState.selectedListId }?.name ?: "No List (Inbox)"
                    OutlinedTextField(
                        value = currentListName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("List") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = listDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                            .testTag("task_list_dropdown")
                    )
                    ExposedDropdownMenu(
                        expanded = listDropdownExpanded,
                        onDismissRequest = { listDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("No List (Inbox)") },
                            onClick = {
                                viewModel.updateSelectedList(null)
                                listDropdownExpanded = false
                            }
                        )
                        uiState.taskLists.forEach { list ->
                            DropdownMenuItem(
                                text = { Text(list.name) },
                                onClick = {
                                    viewModel.updateSelectedList(list.id)
                                    listDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Priority Selection Row
                Column {
                    Text(
                        text = "Priority",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TaskPriority.entries.forEach { priority ->
                            val isSelected = uiState.priority == priority
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.updatePriority(priority) },
                                label = {
                                    Text(
                                        text = priority.name.lowercase()
                                            .replaceFirstChar { it.uppercase() }
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Flag,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.testTag("priority_chip_${priority.name.lowercase()}")
                            )
                        }
                    }
                }

                // Due Date & Time Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Schedule",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Date Button
                            Button(
                                onClick = { showDatePicker = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("date_picker_button")
                            ) {
                                Icon(imageVector = Icons.Default.Event, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (uiState.dueDate != null) DateUtils.formatShortDate(uiState.dueDate!!) else "Set Date"
                                )
                            }

                            // Time Button (active if date set)
                            Button(
                                onClick = { showTimePicker = true },
                                enabled = uiState.dueDate != null,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("time_picker_button")
                            ) {
                                Icon(imageVector = Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = uiState.dueTime ?: "Set Time"
                                )
                            }
                        }

                        // Clear Schedule Button
                        if (uiState.dueDate != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = {
                                    viewModel.updateDueDate(null)
                                },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Clear Schedule")
                            }
                        }
                    }
                }

                // Recurrence Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Repeat, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Recurrence",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RecurrenceType.entries.forEach { type ->
                                val isSelected = uiState.recurrence.type == type
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        viewModel.updateRecurrence(uiState.recurrence.copy(type = type))
                                    },
                                    label = {
                                        Text(
                                            text = type.name.lowercase().replaceFirstChar { it.uppercase() }
                                        )
                                    },
                                    modifier = Modifier.testTag("recurrence_chip_${type.name.lowercase()}")
                                )
                            }
                        }

                        // Weekly days of week picker
                        if (uiState.recurrence.type == RecurrenceType.WEEKLY) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Repeat on",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            val dayLabels = listOf("M" to 1, "T" to 2, "W" to 3, "T" to 4, "F" to 5, "S" to 6, "S" to 7)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                dayLabels.forEach { (label, dayInt) ->
                                    val isDaySelected = uiState.recurrence.daysOfWeek.contains(dayInt)
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isDaySelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                        contentColor = if (isDaySelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                        tonalElevation = 2.dp,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                val newDays = if (isDaySelected) {
                                                    uiState.recurrence.daysOfWeek - dayInt
                                                } else {
                                                    (uiState.recurrence.daysOfWeek + dayInt).sorted()
                                                }
                                                viewModel.updateRecurrence(uiState.recurrence.copy(daysOfWeek = newDays))
                                            }
                                            .testTag("weekday_toggle_$dayInt")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // Custom interval
                        if (uiState.recurrence.type == RecurrenceType.CUSTOM) {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = uiState.recurrence.interval.toString(),
                                onValueChange = { str ->
                                    val interval = str.filter { it.isDigit() }.toIntOrNull() ?: 1
                                    viewModel.updateRecurrence(uiState.recurrence.copy(interval = if (interval < 1) 1 else interval))
                                },
                                label = { Text("Repeat every (days)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Reminders Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Reminder",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        ExposedDropdownMenuBox(
                            expanded = reminderDropdownExpanded,
                            onExpandedChange = { reminderDropdownExpanded = !reminderDropdownExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = uiState.reminderType.label,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Reminder Timing") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = reminderDropdownExpanded) },
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth()
                                    .testTag("reminder_dropdown")
                            )
                            ExposedDropdownMenu(
                                expanded = reminderDropdownExpanded,
                                onDismissRequest = { reminderDropdownExpanded = false }
                            ) {
                                ReminderType.entries.forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(type.label) },
                                        onClick = {
                                            viewModel.updateReminderType(type)
                                            reminderDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        if (uiState.reminderType != ReminderType.NONE && uiState.dueDate == null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "⚠️ Set a due date or time above for the reminder to trigger.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // Focus & Time Tracking Section
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_focus_card"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Focus & Time Tracking",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Estimated Duration
                        Text(
                            text = "Estimated Duration",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        val estimateOptions = listOf(
                            null to "None",
                            15 to "15m",
                            25 to "25m",
                            45 to "45m",
                            60 to "1h",
                            90 to "1.5h"
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            estimateOptions.forEach { (mins, label) ->
                                val isSelected = uiState.estimatedDurationMinutes == mins
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.updateEstimatedDuration(mins) },
                                    label = { Text(label) },
                                    modifier = Modifier.testTag("estimate_chip_${label.lowercase()}")
                                )
                            }
                        }

                        // Actual Tracked Time
                        if (uiState.actualDurationMinutes > 0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HourglassEmpty,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Actual focus tracked: ${uiState.actualDurationMinutes} mins",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }

                        // Start Focus Button (if task exists)
                        if (uiState.taskId != null && uiState.taskId!! > 0 && onStartFocus != null) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    onStartFocus(
                                        uiState.taskId!!,
                                        uiState.title,
                                        uiState.estimatedDurationMinutes
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("start_focus_for_task_button")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Start Focus Session")
                            }
                        }
                    }
                }

                // Subtasks Section (Available when task is created in DB)
                if (uiState.taskId != null && uiState.taskId!! > 0) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Subtasks (${uiState.completedSubtasksCount} of ${uiState.totalSubtasksCount} completed)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // List existing subtasks
                            uiState.subtasks.forEach { subtask ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = subtask.isCompleted,
                                        onCheckedChange = { checked ->
                                            viewModel.toggleSubtaskCompletion(subtask.id, checked)
                                        },
                                        modifier = Modifier.testTag("subtask_checkbox_${subtask.id}")
                                    )
                                    Text(
                                        text = subtask.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        textDecoration = if (subtask.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                        color = if (subtask.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { viewModel.deleteSubtask(subtask.id) },
                                        modifier = Modifier.testTag("delete_subtask_${subtask.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Delete Subtask",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Add new subtask input
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newSubtaskTitle,
                                    onValueChange = { newSubtaskTitle = it },
                                    placeholder = { Text("Add a subtask") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("new_subtask_input")
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        if (newSubtaskTitle.isNotBlank()) {
                                            viewModel.addSubtask(newSubtaskTitle)
                                            newSubtaskTitle = ""
                                        }
                                    },
                                    modifier = Modifier.testTag("add_subtask_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add Subtask",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                // Notes
                OutlinedTextField(
                    value = uiState.notes,
                    onValueChange = { viewModel.updateNotes(it) },
                    label = { Text("Notes") },
                    placeholder = { Text("Additional notes, links, or instructions") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_notes_input")
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Task") },
            text = { Text("Are you sure you want to delete this task? Any associated subtasks will also be removed.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.deleteTask()
                    },
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmDialog = false },
                    modifier = Modifier.testTag("cancel_delete_button")
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val initialDate = uiState.dueDate?.let { DateUtils.localDateMillisToUtcMidnight(it) } ?: System.currentTimeMillis()
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialDate
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val localStartOfDay = datePickerState.selectedDateMillis?.let {
                            DateUtils.utcDateMillisToLocalStartOfDay(it)
                        }
                        viewModel.updateDueDate(localStartOfDay)
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.updateDueDate(null)
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

    // Time Picker Dialog
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = 9,
            initialMinute = 0
        )
        com.flowtasks.app.core.designsystem.component.TimePickerDialog(
            state = timePickerState,
            onDismissRequest = { showTimePicker = false },
            onConfirm = {
                val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", timePickerState.hour, timePickerState.minute)
                viewModel.updateDueTime(formattedTime)
                showTimePicker = false
            },
            onClear = {
                viewModel.updateDueTime(null)
                showTimePicker = false
            }
        )
    }

    // AI Draft Prompt Dialog
    if (showPromptDialog) {
        AlertDialog(
            onDismissRequest = { showPromptDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Draft Task with AI")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Describe the task in natural language. AI will propose a structured title, description, priority, and time estimate for you to review.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = promptInputText,
                        onValueChange = { promptInputText = it },
                        placeholder = { Text("e.g., Prepare quarterly budget review deck with revenue breakdowns") },
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_prompt_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (promptInputText.isNotBlank()) {
                            viewModel.generateTaskFromPrompt(promptInputText)
                            showPromptDialog = false
                        }
                    },
                    enabled = promptInputText.isNotBlank(),
                    modifier = Modifier.testTag("ai_generate_prompt_button")
                ) {
                    Text("Generate Proposal")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPromptDialog = false },
                    modifier = Modifier.testTag("ai_cancel_prompt_button")
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // AI Subtasks Suggestion Dialog
    if (uiState.showSubtasksDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissSubtasksDialog() },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.FormatListNumbered, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Suggested Subtasks")
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Review suggested subtasks. You can add them individually or all at once.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    uiState.suggestedSubtasks.forEach { subtaskTitle ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = subtaskTitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.applySuggestedSubtask(subtaskTitle) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add Subtask",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.applyAllSuggestedSubtasks() },
                    enabled = uiState.suggestedSubtasks.isNotEmpty(),
                    modifier = Modifier.testTag("ai_add_all_subtasks_button")
                ) {
                    Text("Add All (${uiState.suggestedSubtasks.size})")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissSubtasksDialog() },
                    modifier = Modifier.testTag("ai_dismiss_subtasks_button")
                ) {
                    Text("Done")
                }
            }
        )
    }
}
