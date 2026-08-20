package com.flowtasks.app.feature.focus

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private val DURATION_PRESETS = listOf(15, 25, 30, 45, 60)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusScreen(
    viewModel: FocusViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler {
        viewModel.requestExit(onDirectExit = onNavigateBack)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Focus Mode", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.requestExit(onDirectExit = onNavigateBack) },
                        modifier = Modifier.testTag("focus_close_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close Focus Mode")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        modifier = modifier
            .fillMaxSize()
            .testTag("focus_screen")
    ) { paddingValues ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Associated Task Card
            if (state.taskTitle != null) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("focus_task_card")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.TaskAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = state.taskTitle ?: "",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "Quick Focus Session",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Duration Presets (Only visible before starting)
            AnimatedVisibility(
                visible = !state.isRunning && !state.isPaused && !state.isFinished,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Target Duration",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        DURATION_PRESETS.forEach { duration ->
                            FilterChip(
                                selected = state.targetDurationMinutes == duration,
                                onClick = { viewModel.setTargetDuration(duration) },
                                label = { Text("${duration}m") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("focus_duration_${duration}")
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Circular Timer Display
            val primaryColor = MaterialTheme.colorScheme.primary
            val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            val progress = state.progress

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(240.dp)
                    .testTag("focus_timer_circle")
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 14.dp.toPx()
                    // Background track
                    drawCircle(
                        color = trackColor,
                        style = Stroke(width = strokeWidth)
                    )
                    // Progress arc
                    val sweepAngle = progress * 360f
                    drawArc(
                        color = primaryColor,
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (state.isRunning || state.isPaused) state.formattedRemainingTime else "${state.targetDurationMinutes}:00",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = 1.sp,
                        modifier = Modifier.testTag("focus_timer_text")
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    val statusText = when {
                        state.isFinished -> "Session Finished"
                        state.isPaused -> "Paused"
                        state.isRunning -> "Elapsed: ${state.formattedElapsedTime}"
                        else -> "Ready to Focus"
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Timer Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                if (!state.isRunning && !state.isPaused) {
                    // Start Button
                    Button(
                        onClick = { viewModel.startTimer() },
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier
                            .height(56.dp)
                            .fillMaxWidth(0.7f)
                            .testTag("focus_start_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Focus", style = MaterialTheme.typography.titleMedium)
                    }
                } else {
                    // Pause / Resume Button
                    if (state.isPaused) {
                        Button(
                            onClick = { viewModel.resumeTimer() },
                            shape = CircleShape,
                            modifier = Modifier
                                .size(64.dp)
                                .testTag("focus_resume_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Resume", modifier = Modifier.size(28.dp))
                        }
                    } else {
                        OutlinedButton(
                            onClick = { viewModel.pauseTimer() },
                            shape = CircleShape,
                            modifier = Modifier
                                .size(64.dp)
                                .testTag("focus_pause_button")
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = "Pause", modifier = Modifier.size(28.dp))
                        }
                    }

                    // Finish & Save Session Button
                    Button(
                        onClick = { viewModel.finishSession(onFinished = onNavigateBack) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier
                            .height(56.dp)
                            .testTag("focus_finish_button")
                    ) {
                        Icon(Icons.Default.DoneAll, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Finish & Save", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    // Exit Confirmation Dialog
    if (state.showExitConfirmDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissExitDialog() },
            icon = { Icon(Icons.Default.HourglassEmpty, contentDescription = null) },
            title = { Text("Leave Focus Mode?") },
            text = { Text("Your focus timer is currently running. Leaving now will stop the session without saving.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmExitWithoutSaving(onExit = onNavigateBack) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_exit_focus_button")
                ) {
                    Text("Exit")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissExitDialog() },
                    modifier = Modifier.testTag("cancel_exit_focus_button")
                ) {
                    Text("Stay")
                }
            },
            modifier = Modifier.testTag("focus_exit_dialog")
        )
    }

    // Complete Task Prompt Dialog on Finish
    if (state.showCompleteTaskPrompt) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissWithoutCompletingTask(onDismiss = onNavigateBack) },
            icon = { Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Great Focus Session!") },
            text = {
                Text(
                    "You focused for ${state.formattedElapsedTime}.\n\nWould you like to mark \"${state.taskTitle}\" as completed?"
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.completeTaskAndDismiss(onDismiss = onNavigateBack) },
                    modifier = Modifier.testTag("focus_mark_task_completed_button")
                ) {
                    Text("Mark Completed")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissWithoutCompletingTask(onDismiss = onNavigateBack) },
                    modifier = Modifier.testTag("focus_keep_task_uncompleted_button")
                ) {
                    Text("Keep Active")
                }
            },
            modifier = Modifier.testTag("focus_complete_prompt_dialog")
        )
    }
}
