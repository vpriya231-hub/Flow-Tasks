package com.flowtasks.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flowtasks.app.domain.model.TaskPriority
import com.flowtasks.app.ui.theme.PriorityHigh
import com.flowtasks.app.ui.theme.PriorityHighContainer
import com.flowtasks.app.ui.theme.PriorityLow
import com.flowtasks.app.ui.theme.PriorityLowContainer
import com.flowtasks.app.ui.theme.PriorityMedium
import com.flowtasks.app.ui.theme.PriorityMediumContainer
import com.flowtasks.app.ui.theme.PriorityNone
import com.flowtasks.app.ui.theme.PriorityNoneContainer

@Composable
fun PriorityBadge(
    priority: TaskPriority,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true
) {
    if (priority == TaskPriority.NONE && !showLabel) return

    val (bg, fg, label) = when (priority) {
        TaskPriority.HIGH -> Triple(PriorityHighContainer, PriorityHigh, "High")
        TaskPriority.MEDIUM -> Triple(PriorityMediumContainer, PriorityMedium, "Medium")
        TaskPriority.LOW -> Triple(PriorityLowContainer, PriorityLow, "Low")
        TaskPriority.NONE -> Triple(PriorityNoneContainer, PriorityNone, "None")
    }

    Box(
        modifier = modifier
            .testTag("priority_badge_${priority.name.lowercase()}")
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Flag,
                contentDescription = "$label Priority",
                tint = fg,
                modifier = Modifier.size(12.dp)
            )
            if (showLabel) {
                Text(
                    text = label,
                    color = fg,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}
