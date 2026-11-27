package sapienz.allison.disclab

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sapienz.allison.disclab.capture.CancelReason
import sapienz.allison.disclab.capture.CaptureEvent
import sapienz.allison.disclab.capture.CaptureMachine
import sapienz.allison.disclab.capture.CapturePhase
import sapienz.allison.disclab.capture.CaptureState
import sapienz.allison.disclab.capture.ThrowKey

/**
 * Everything [MainScreen] needs to draw itself. It is immutable: the ViewModel publishes a new
 * copy on every change instead of mutating this one.
 */
data class MainUiState(
    val capture: CaptureState = CaptureState(),
)

/**
 * Owns the screen state and drives the throw button's state machine. Android keeps one instance
 * alive across configuration changes, so the state survives while the Activity is recreated.
 *
 * Input arrives as plain events. MainActivity forwards volume-down, pauses and focus loss, and the
 * screen forwards taps. Each event goes through the pure [CaptureMachine.transition], and the
 * result is published on [uiState]. The UI only reads that state and never changes it directly
 * (unidirectional data flow).
 *
 * Every event arrives on the main thread. The hold timer does too, because [viewModelScope] runs on
 * the main dispatcher.
 */
class MainViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    /** Cancels a press held past [CaptureMachine.MAX_HOLD_MS], even when no key-up arrives. */
    private var holdTimer: Job? = null

    fun onArm() = handle(CaptureEvent.Arm)

    fun onDisarm() = handle(CaptureEvent.Disarm)

    /**
     * Handles one volume-down event. Returns true when DiscLab consumes the key, which keeps it
     * from changing the volume, and false to hand it back to the system.
     */
    fun onThrowKey(key: ThrowKey): Boolean {
        val consumed = CaptureMachine.consumesThrowKey(_uiState.value.capture.phase)
        handle(key)
        return consumed
    }

    /** The Activity paused or lost window focus. This cancels a throw in progress. */
    fun onInterrupted(reason: CancelReason) = handle(CaptureEvent.Interrupted(reason))

    private fun handle(event: CaptureEvent) {
        val before = _uiState.value.capture
        val after = CaptureMachine.transition(before, event)
        if (after == before) return
        _uiState.update { it.copy(capture = after) }
        updateHoldTimer(before.phase, after.phase)
    }

    /** Starts the hold timer when a throw starts, and stops it when the throw ends. */
    private fun updateHoldTimer(before: CapturePhase, after: CapturePhase) {
        if (after is CapturePhase.Recording && before !is CapturePhase.Recording) {
            holdTimer = viewModelScope.launch {
                delay(CaptureMachine.MAX_HOLD_MS)
                handle(CaptureEvent.HoldTimedOut(after.pressTimeMs))
            }
        } else if (after !is CapturePhase.Recording) {
            holdTimer?.cancel()
            holdTimer = null
        }
    }
}
