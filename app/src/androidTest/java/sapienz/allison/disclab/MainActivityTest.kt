package sapienz.allison.disclab

import android.os.SystemClock
import android.view.KeyEvent
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Launches the real Activity and checks the key -> ViewModel -> screen wiring end to end. */
@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun armsAndCapturesAVolumeDownPress() {
        composeRule.onNodeWithText("DiscLab").assertExists()
        composeRule.onNodeWithText("No throws yet.").assertExists()

        composeRule.onNodeWithText("Arm capture").performClick()
        composeRule.onNodeWithText("Disarm").assertIsEnabled()

        val pressTime = SystemClock.uptimeMillis()
        sendVolumeDown(KeyEvent.ACTION_DOWN, pressTime)
        composeRule.onNodeWithText("Disarm").assertIsNotEnabled()
        SystemClock.sleep(250)
        sendVolumeDown(KeyEvent.ACTION_UP, pressTime)

        composeRule.onNodeWithText("Last throw: captured").assertExists()
        composeRule.onNodeWithText("Held for", substring = true).assertExists()

        composeRule.onNodeWithText("Disarm").performClick()
        composeRule.onNodeWithText("Arm capture").assertExists()
    }

    /**
     * Injects a volume-down event through the system's input pipeline, as a real key press would
     * arrive, and waits until the app has handled it. The times must be real: the system drops
     * key events that are more than a few seconds old.
     */
    private fun sendVolumeDown(action: Int, downTime: Long) {
        val event = KeyEvent(
            downTime,
            SystemClock.uptimeMillis(),
            action,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            0,
        )
        InstrumentationRegistry.getInstrumentation().sendKeySync(event)
    }
}
