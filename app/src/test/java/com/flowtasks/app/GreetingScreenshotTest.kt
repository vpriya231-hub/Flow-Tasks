package com.flowtasks.app

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.flowtasks.app.core.designsystem.component.EmptyStateView
import com.flowtasks.app.ui.theme.FlowTasksTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TaskAlt
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
class GreetingScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun empty_state_screenshot() {
        composeTestRule.setContent {
            FlowTasksTheme {
                EmptyStateView(
                    icon = Icons.Default.TaskAlt,
                    title = "No tasks yet",
                    subtitle = "Tap the + button to create your first task.",
                    actionLabel = "Create Task"
                )
            }
        }

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/empty_state.png")
    }
}
