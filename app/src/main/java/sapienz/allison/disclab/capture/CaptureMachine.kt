package sapienz.allison.disclab.capture

// The throw button's state machine (issue #2), the first slice of DiscLab's gameplay state
// machine. Later stages add their phases (calibration, processing, playback, ...) to it.
//
// Design: a finite-state machine written as plain data plus one pure function. The states and
// events below are immutable values, and CaptureMachine.transition computes the next state from
// the current state and one event, with no side effects. In Java terms it is an enum-like type and
// a static method that switches over it. It is deliberately not the GoF State pattern, where each
// state is a class with its own behaviour: here every rule sits in one function.
//
// Nothing in this file uses Android types, so all the rules are tested on the JVM
// (CaptureMachineTest).
//
// Times are KeyEvent.getEventTime() values: whole milliseconds in the SystemClock.uptimeMillis()
// base, stored as-is. Issue #4 converts them to the sensor clock.

/** Where the throw button is in its cycle. */
sealed interface CapturePhase {
    /** Capture is off, and volume-down changes the volume as usual. */
    data object Idle : CapturePhase

    /** Capture is armed and waiting for volume-down. */
    data object Ready : CapturePhase

    /** Volume-down is held and a throw is in progress. Only one throw can be active at a time. */
    data class Recording(val pressTimeMs: Long) : CapturePhase
}

/** How a throw ended. */
sealed interface ThrowOutcome {
    val pressTimeMs: Long

    /** Released by the player. [releaseTimeMs], the key-up time, is the authoritative release. */
    data class Captured(override val pressTimeMs: Long, val releaseTimeMs: Long) : ThrowOutcome {
        val heldMs: Long get() = releaseTimeMs - pressTimeMs
    }

    /** Ended by something other than a normal release. */
    data class Cancelled(override val pressTimeMs: Long, val reason: CancelReason) : ThrowOutcome
}

enum class CancelReason {
    /** The system cancelled the key press (a key-up with `FLAG_CANCELED`). */
    KEY_CANCELED,

    /** The Activity paused, for example when switching apps or locking the screen. */
    APP_PAUSED,

    /** The window lost focus without pausing: the notification shade or a system dialog. */
    FOCUS_LOST,

    /** The button was held longer than [CaptureMachine.MAX_HOLD_MS]. */
    HELD_TOO_LONG,
}

/**
 * The machine's whole state. When a throw ends, [phase] returns to [CapturePhase.Ready] and the
 * outcome stays in [lastThrow], so it remains visible while the next throw is armed.
 */
data class CaptureState(
    val phase: CapturePhase = CapturePhase.Idle,
    val lastThrow: ThrowOutcome? = null,
)

/** Everything that can happen to the machine. */
sealed interface CaptureEvent {
    /** The on-screen control switched capture on. */
    data object Arm : CaptureEvent

    /** The on-screen control switched capture off. */
    data object Disarm : CaptureEvent

    /** Something outside the gesture interrupted it. */
    data class Interrupted(val reason: CancelReason) : CaptureEvent

    /** The hold timer started by the press at [pressTimeMs] ran out. */
    data class HoldTimedOut(val pressTimeMs: Long) : CaptureEvent
}

/**
 * A volume-down `KeyEvent`, reduced to the plain values the machine needs. MainActivity builds
 * these from Android's `KeyEvent`.
 */
sealed interface ThrowKey : CaptureEvent {
    val eventTimeMs: Long

    /** The key went down. [repeatCount] is above 0 for the auto-repeats sent while it's held. */
    data class Down(override val eventTimeMs: Long, val repeatCount: Int) : ThrowKey

    /** The key went up. [canceled] is `FLAG_CANCELED`: the system aborted the press. */
    data class Up(override val eventTimeMs: Long, val canceled: Boolean) : ThrowKey
}

object CaptureMachine {
    /**
     * The longest accepted hold (the 5 s suggested in #2). A longer press isn't treated as a
     * throw, and the limit also bounds how long one recording can get.
     */
    const val MAX_HOLD_MS = 5_000L

    /**
     * Whether DiscLab takes volume-down for itself. It does only while capture is armed (Ready or
     * Recording). In every other phase the key changes the volume as usual.
     */
    fun consumesThrowKey(phase: CapturePhase): Boolean = phase != CapturePhase.Idle

    /** The state after [event]. An event that doesn't apply in the current phase changes nothing. */
    fun transition(state: CaptureState, event: CaptureEvent): CaptureState {
        val phase = state.phase
        return when (event) {
            CaptureEvent.Arm ->
                if (phase == CapturePhase.Idle) state.copy(phase = CapturePhase.Ready) else state

            // Ignored while recording: only the key or an interruption ends a throw, so a palm
            // brushing the screen mid-throw can't end it.
            CaptureEvent.Disarm ->
                if (phase == CapturePhase.Ready) state.copy(phase = CapturePhase.Idle) else state

            // Only the first down of a press starts a throw. The auto-repeats sent while the key
            // is held are ignored, and so is any press while a throw is already active.
            is ThrowKey.Down ->
                if (phase == CapturePhase.Ready && event.repeatCount == 0) {
                    state.copy(phase = CapturePhase.Recording(pressTimeMs = event.eventTimeMs))
                } else {
                    state
                }

            is ThrowKey.Up -> when {
                phase !is CapturePhase.Recording -> state
                event.canceled -> state.endWith(cancelled(phase, CancelReason.KEY_CANCELED))
                event.eventTimeMs - phase.pressTimeMs > MAX_HOLD_MS ->
                    state.endWith(cancelled(phase, CancelReason.HELD_TOO_LONG))
                else -> state.endWith(
                    ThrowOutcome.Captured(phase.pressTimeMs, releaseTimeMs = event.eventTimeMs),
                )
            }

            is CaptureEvent.Interrupted ->
                if (phase is CapturePhase.Recording) {
                    state.endWith(cancelled(phase, event.reason))
                } else {
                    state
                }

            // The press time identifies the throw, so a timer left over from an earlier throw
            // can't cancel the current one.
            is CaptureEvent.HoldTimedOut ->
                if (phase is CapturePhase.Recording && phase.pressTimeMs == event.pressTimeMs) {
                    state.endWith(cancelled(phase, CancelReason.HELD_TOO_LONG))
                } else {
                    state
                }
        }
    }

    private fun cancelled(recording: CapturePhase.Recording, reason: CancelReason) =
        ThrowOutcome.Cancelled(recording.pressTimeMs, reason)

    /** A throw ended: back to Ready, with its outcome kept visible. */
    private fun CaptureState.endWith(outcome: ThrowOutcome) =
        copy(phase = CapturePhase.Ready, lastThrow = outcome)
}
