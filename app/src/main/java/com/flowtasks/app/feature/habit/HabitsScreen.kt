package com.flowtasks.app.feature.habit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flowtasks.app.core.designsystem.component.EmptyStateView
import com.flowtasks.app.core.designsystem.component.FlowTasksBottomNavBar
import com.flowtasks.app.core.designsystem.component.NavigationTab
import com.flowtasks.app.domain.model.Habit
import com.flowtasks.app.domain.model.HabitDayStatus
import com.flowtasks.app.domain.model.HabitFrequencyType

private val PRESET_COLORS = listOf(
    "#10B981", // Emerald
    "#4F46E5", // Indigo
    "#0EA5E9", // Sky
    "#F59E0B", // Amber
    "#EF4444", // Red
    "#8B5CF6", // Purple
    "#EC4899", // Pink
    "#14B8A6"  // Teal
)

private val DAYS_OF_WEEK_LABELS = listOf(
    1 to "Mon",
    2 to "Tue",
    3 to "Wed",
    4 to "Thu",
    5 to "Fri",
    6 to "Sat",
    7 to "Sun"
)

private fun parseColorSafe(hex: String, defaultColor: Color = Color(0xFF10B981)): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        defaultColor
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsScreen(
    viewModel: HabitsViewModel,
    onTabSelected: (NavigationTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Habits & Streaks",
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        },
        bottomBar = {
            FlowTasksBottomNavBar(
                selectedTab = NavigationTab.HABITS,
                onTabSelected = onTabSelected
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openCreateHabitSheet() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("habits_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Habit"
                )
            }
        },
        modifier = modifier.testTag("habits_screen")
    ) { paddingValues ->
        if (state.habits.isEmpty()) {
            EmptyStateView(
                icon = Icons.Outlined.Refresh,
                title = "No Habits Yet",
                subtitle = "Build daily momentum with habit tracking, streak counters, and weekly history.",
                actionLabel = "Create Habit",
                onActionClick = { viewModel.openCreateHabitSheet() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .testTag("habits_list")
            ) {
                items(state.habits, key = { it.id }) { habit ->
                    HabitCardItem(
                        habit = habit,
                        onToggleToday = { viewModel.toggleHabitToday(habit.id) },
                        onToggleDate = { dateMillis -> viewModel.toggleHabitForDate(habit.id, dateMillis) },
                        onViewHistory = { viewModel.openHabitHistory(habit) },
                        onEdit = { viewModel.openEditHabitSheet(habit) },
                        onDelete = { viewModel.confirmDeleteHabit(habit) }
                    )
                }
            }
        }
    }

    // Habit History Bottom Sheet
    if (state.historyHabit != null) {
        HabitHistoryBottomSheet(
            habit = state.historyHabit!!,
            onToggleDate = { dateMillis -> viewModel.toggleHabitForDate(state.historyHabit!!.id, dateMillis) },
            onDismiss = { viewModel.closeHabitHistory() }
        )
    }

    // Create / Edit Sheet
    if (state.isCreateHabitSheetOpen) {
        HabitBottomSheet(
            editingHabit = state.editingHabit,
            onDismiss = { viewModel.closeHabitSheet() },
            onSave = { title, description, freqType, days, targetCount, colorHex ->
                viewModel.saveHabit(title, description, freqType, days, targetCount, colorHex)
            }
        )
    }

    // Delete Confirmation Dialog
    if (state.habitToDelete != null) {
        val habit = state.habitToDelete!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteHabitDialog() },
            title = { Text("Delete Habit?") },
            text = { Text("Are you sure you want to delete \"${habit.title}\"? All check-in history will be removed.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteHabitConfirmed() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_habit_button")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissDeleteHabitDialog() },
                    modifier = Modifier.testTag("cancel_delete_habit_button")
                ) {
                    Text("Cancel")
                }
            },
            modifier = Modifier.testTag("delete_habit_dialog")
        )
    }
}

@Composable
fun HabitCardItem(
    habit: Habit,
    onToggleToday: () -> Unit,
    onToggleDate: (Long) -> Unit,
    onViewHistory: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuOpen by remember { mutableStateOf(false) }
    val accentColor = parseColorSafe(habit.colorHex)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("habit_card_${habit.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row: Title, Description, Overflow Menu
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Color Bar
                Box(
                    modifier = Modifier
                        .size(width = 4.dp, height = 28.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(accentColor)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = habit.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (habit.description.isNotBlank()) {
                        Text(
                            text = habit.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                // Overflow Menu
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Habit options")
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("History") },
                            leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                onViewHistory()
                            },
                            modifier = Modifier.testTag("habit_menu_history_${habit.id}")
                        )
                        DropdownMenuItem(
                            text = { Text("Edit Habit") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Habit", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                menuOpen = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Frequency and Streak Badges Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Frequency Badge
                val freqLabel = when (habit.frequencyType) {
                    HabitFrequencyType.DAILY -> "Daily"
                    HabitFrequencyType.WEEKLY -> "Weekly"
                    HabitFrequencyType.CUSTOM_DAYS -> {
                        val daysMap = mapOf(1 to "M", 2 to "Tu", 3 to "W", 4 to "Th", 5 to "F", 6 to "Sa", 7 to "Su")
                        habit.frequencyDays.joinToString(", ") { daysMap[it] ?: "" }
                    }
                }

                Text(
                    text = freqLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )

                // Current Streak Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "🔥 ${habit.currentStreak} day streak",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                // Best Streak Badge
                if (habit.bestStreak > 0) {
                    Text(
                        text = "Best: ${habit.bestStreak}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 7-Day History Matrix + Today Check-in Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 7-Day History Bubbles
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    habit.weeklyHistory.forEach { day ->
                        HabitDayBubble(
                            day = day,
                            accentColor = accentColor,
                            onClick = { onToggleDate(day.dateMillis) }
                        )
                    }
                }

                // Today Quick Check-in Button
                Button(
                    onClick = onToggleToday,
                    colors = if (habit.isCompletedToday) {
                        ButtonDefaults.buttonColors(containerColor = accentColor)
                    } else {
                        ButtonDefaults.filledTonalButtonColors()
                    },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("habit_today_button_${habit.id}")
                ) {
                    if (habit.isCompletedToday) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Done", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    } else {
                        Text("Check In", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
fun HabitDayBubble(
    day: HabitDayStatus,
    accentColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(2.dp)
    ) {
        Text(
            text = day.dayLabel,
            fontSize = 10.sp,
            fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
            color = if (day.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    if (day.isCompleted) accentColor
                    else if (day.isToday) MaterialTheme.colorScheme.surfaceVariant
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
                .then(
                    if (day.isToday && !day.isCompleted) {
                        Modifier.border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    } else {
                        Modifier
                    }
                )
        ) {
            if (day.isCompleted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitBottomSheet(
    editingHabit: Habit?,
    onDismiss: () -> Unit,
    onSave: (String, String, HabitFrequencyType, List<Int>, Int, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf(editingHabit?.title ?: "") }
    var description by remember { mutableStateOf(editingHabit?.description ?: "") }
    var frequencyType by remember { mutableStateOf(editingHabit?.frequencyType ?: HabitFrequencyType.DAILY) }
    val selectedDays = remember {
        mutableStateListOf<Int>().apply {
            if (editingHabit != null && editingHabit.frequencyDays.isNotEmpty()) {
                addAll(editingHabit.frequencyDays)
            } else {
                addAll(listOf(1, 2, 3, 4, 5)) // Mon-Fri default for custom days
            }
        }
    }
    var selectedColor by remember { mutableStateOf(editingHabit?.colorHex ?: PRESET_COLORS.first()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.testTag("habit_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = if (editingHabit != null) "Edit Habit" else "Create New Habit",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Habit Title") },
                placeholder = { Text("e.g. Read 20 pages, Morning Run") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("habit_title_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (Optional)") },
                placeholder = { Text("e.g. Daily reading before sleep") },
                minLines = 2,
                maxLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("habit_description_input")
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Frequency", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                HabitFrequencyType.entries.forEach { type ->
                    FilterChip(
                        selected = frequencyType == type,
                        onClick = { frequencyType = type },
                        label = {
                            Text(
                                when (type) {
                                    HabitFrequencyType.DAILY -> "Daily"
                                    HabitFrequencyType.CUSTOM_DAYS -> "Specific Days"
                                    HabitFrequencyType.WEEKLY -> "Weekly"
                                }
                            )
                        },
                        modifier = Modifier.testTag("habit_freq_${type.name.lowercase()}")
                    )
                }
            }

            // If Specific Days is selected, show Day of Week toggles
            if (frequencyType == HabitFrequencyType.CUSTOM_DAYS) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DAYS_OF_WEEK_LABELS.forEach { (dayNum, label) ->
                        val isSelected = selectedDays.contains(dayNum)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) {
                                    if (selectedDays.size > 1) selectedDays.remove(dayNum)
                                } else {
                                    selectedDays.add(dayNum)
                                }
                            },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
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
                            onSave(
                                title,
                                description,
                                frequencyType,
                                if (frequencyType == HabitFrequencyType.CUSTOM_DAYS) selectedDays.sorted() else emptyList(),
                                1,
                                selectedColor
                            )
                        }
                    },
                    enabled = title.isNotBlank(),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("save_habit_button")
                ) {
                    Text("Save Habit")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
