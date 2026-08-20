package com.flowtasks.app.core.designsystem.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag

enum class NavigationTab(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    TASKS("tasks", "Tasks", Icons.Filled.TaskAlt, Icons.Outlined.TaskAlt),
    GOALS("goals", "Goals", Icons.Filled.Flag, Icons.Outlined.Flag),
    HABITS("habits", "Habits", Icons.Filled.Refresh, Icons.Outlined.Refresh),
    DASHBOARD("dashboard", "Dashboard", Icons.Filled.BarChart, Icons.Outlined.BarChart),
    LISTS("lists", "Lists", Icons.Filled.FormatListBulleted, Icons.Outlined.FormatListBulleted),
    SETTINGS("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
    STARRED("starred", "Starred", Icons.Filled.Star, Icons.Outlined.StarOutline)
}

val MainBottomNavTabs = listOf(
    NavigationTab.TASKS,
    NavigationTab.GOALS,
    NavigationTab.HABITS,
    NavigationTab.DASHBOARD,
    NavigationTab.SETTINGS
)

@Composable
fun FlowTasksBottomNavBar(
    selectedTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier.testTag("bottom_nav_bar")
    ) {
        MainBottomNavTabs.forEach { tab ->
            val isSelected = selectedTab == tab
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = tab.title
                    )
                },
                label = { Text(tab.title) },
                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
            )
        }
    }
}
