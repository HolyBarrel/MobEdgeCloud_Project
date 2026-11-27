package sapienz.allison.disclab

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import sapienz.allison.disclab.capture.CancelReason
import sapienz.allison.disclab.capture.CapturePhase
import sapienz.allison.disclab.capture.ThrowKey
import sapienz.allison.disclab.ui.theme.DiscLabTheme

/**
 * The Android-facing side of the screen. It receives the hardware key and the lifecycle
 * callbacks, turns them into plain events for [MainViewModel], and hosts [MainScreen].
 */
class MainActivity : ComponentActivity() {
    // The Activity owns the ViewModel because the Activity is what receives the volume key. The
    // same instance survives rotation and other configuration changes.
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DiscLabTheme {
                // Collects only while the Activity is at least STARTED.
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                val armed = state.capture.phase != CapturePhase.Idle
                LaunchedEffect(armed) { holdWindowForCapture(armed) }
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        state = state,
                        onArm = viewModel::onArm,
                        onDisarm = viewModel::onDisarm,
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }

    // Volume-down is the throw button. Android calls onKeyDown and onKeyUp for keys no view
    // handled, before its own volume handling, so returning true keeps the key from changing the
    // volume. The ViewModel decides, and it consumes the key only while capture is armed. These
    // two callbacks are used instead of dispatchKeyEvent: androidx's ComponentActivity marks its
    // dispatchKeyEvent override as library-internal, and lint reports overriding it as an error.
    //
    // getEventTime() is in the SystemClock.uptimeMillis() base. It's stored as-is, and #4
    // converts it to the sensor clock.

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN &&
            viewModel.onThrowKey(ThrowKey.Down(event.eventTime, event.repeatCount))
        ) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN &&
            viewModel.onThrowKey(ThrowKey.Up(event.eventTime, canceled = event.isCanceled))
        ) {
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    // Switching apps and locking the screen both pause the Activity. onStop always follows
    // onPause, so cancelling here covers stopping too.
    override fun onPause() {
        super.onPause()
        viewModel.onInterrupted(CancelReason.APP_PAUSED)
    }

    // The notification shade and system dialogs take window focus without pausing the Activity.
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus) viewModel.onInterrupted(CancelReason.FOCUS_LOST)
    }

    /**
     * While capture is armed, keeps the screen on and the orientation fixed. A screen timeout
     * would pause the Activity. A rotation would recreate it, which also passes through onPause.
     * Either one would cancel the throw.
     */
    private fun holdWindowForCapture(armed: Boolean) {
        if (armed) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
}
