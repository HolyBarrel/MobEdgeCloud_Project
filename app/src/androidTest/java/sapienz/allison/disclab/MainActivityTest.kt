package sapienz.allison.disclab

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Launches the real Activity and checks the ViewModel -> screen wiring end to end. */
@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun launchesAndReflectsViewModelState() {
        composeRule.onNodeWithText("DiscLab").assertExists()
        composeRule.onNodeWithText("State updates: 0").assertExists()

        composeRule.onNodeWithText("Update state").performClick()

        composeRule.onNodeWithText("State updates: 1").assertExists()
    }
}
