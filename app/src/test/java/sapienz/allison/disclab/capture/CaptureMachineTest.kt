package sapienz.allison.disclab.capture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import sapienz.allison.disclab.capture.CaptureMachine.MAX_HOLD_MS

/** The throw button's rules (issue #2). Times are made-up uptime milliseconds. */
class CaptureMachineTest {
    private val idle = CaptureState()
    private val ready = CaptureState(phase = CapturePhase.Ready)

    private fun CaptureState.after(vararg events: CaptureEvent): CaptureState =
        events.fold(this) { state, event -> CaptureMachine.transition(state, event) }

    private fun down(timeMs: Long, repeatCount: Int = 0) = ThrowKey.Down(timeMs, repeatCount)

    private fun up(timeMs: Long, canceled: Boolean = false) = ThrowKey.Up(timeMs, canceled)

    private fun cancelled(pressTimeMs: Long, reason: CancelReason) =
        ThrowOutcome.Cancelled(pressTimeMs, reason)

    @Test
    fun armingAndDisarmingSwitchBetweenIdleAndReady() {
        assertEquals(CapturePhase.Ready, idle.after(CaptureEvent.Arm).phase)
        assertEquals(CapturePhase.Idle, ready.after(CaptureEvent.Disarm).phase)
    }

    @Test
    fun theFirstDownInReadyStartsRecording() {
        assertEquals(CapturePhase.Recording(pressTimeMs = 1_000), ready.after(down(1_000)).phase)
    }

    @Test
    fun repeatsWhileHeldAreIgnored() {
        val recording = ready.after(down(1_000))

        val afterRepeats = recording.after(down(1_500, repeatCount = 1), down(1_550, repeatCount = 2))

        assertEquals(recording, afterRepeats)
    }

    @Test
    fun aRepeatInReadyDoesNotStartAThrow() {
        // The key was already held when capture was armed, or is still held after a cancel.
        assertEquals(ready, ready.after(down(1_500, repeatCount = 3)))
    }

    @Test
    fun theUpEndsRecordingAndItsTimeIsTheRelease() {
        val state = ready.after(down(1_000), up(1_640))

        assertEquals(CapturePhase.Ready, state.phase)
        assertEquals(ThrowOutcome.Captured(pressTimeMs = 1_000, releaseTimeMs = 1_640), state.lastThrow)
    }

    @Test
    fun aCanceledUpCancelsInsteadOfReleasing() {
        val state = ready.after(down(1_000), up(1_640, canceled = true))

        assertEquals(CapturePhase.Ready, state.phase)
        assertEquals(cancelled(1_000, CancelReason.KEY_CANCELED), state.lastThrow)
    }

    @Test
    fun pausingCancelsTheActiveThrow() {
        val state = ready.after(down(1_000), CaptureEvent.Interrupted(CancelReason.APP_PAUSED))

        assertEquals(CapturePhase.Ready, state.phase)
        assertEquals(cancelled(1_000, CancelReason.APP_PAUSED), state.lastThrow)
    }

    @Test
    fun losingFocusCancelsTheActiveThrow() {
        val state = ready.after(down(1_000), CaptureEvent.Interrupted(CancelReason.FOCUS_LOST))

        assertEquals(CapturePhase.Ready, state.phase)
        assertEquals(cancelled(1_000, CancelReason.FOCUS_LOST), state.lastThrow)
    }

    @Test
    fun theHoldTimeoutCancelsTheActiveThrow() {
        val state = ready.after(down(1_000), CaptureEvent.HoldTimedOut(pressTimeMs = 1_000))

        assertEquals(CapturePhase.Ready, state.phase)
        assertEquals(cancelled(1_000, CancelReason.HELD_TOO_LONG), state.lastThrow)
    }

    @Test
    fun aTimeoutFromAnEarlierThrowIsIgnored() {
        val secondThrow = ready.after(down(1_000), up(1_400), down(2_000))

        assertEquals(secondThrow, secondThrow.after(CaptureEvent.HoldTimedOut(pressTimeMs = 1_000)))
    }

    @Test
    fun aReleaseAfterTheMaximumHoldIsCancelled() {
        val state = ready.after(down(1_000), up(1_000 + MAX_HOLD_MS + 1))

        assertEquals(cancelled(1_000, CancelReason.HELD_TOO_LONG), state.lastThrow)
    }

    @Test
    fun aReleaseAtExactlyTheMaximumHoldIsCaptured() {
        val state = ready.after(down(1_000), up(1_000 + MAX_HOLD_MS))

        assertEquals(ThrowOutcome.Captured(1_000, 1_000 + MAX_HOLD_MS), state.lastThrow)
    }

    @Test
    fun pressesAreIgnoredOutsideReady() {
        assertEquals(idle, idle.after(down(1_000)))
        assertEquals(idle, idle.after(down(1_000), up(1_300)))
        // In Ready, an up with no throw in progress (released after a cancel) changes nothing.
        assertEquals(ready, ready.after(up(1_300)))
    }

    @Test
    fun onlyOneThrowCanBeActive() {
        val recording = ready.after(down(1_000))

        assertEquals(recording, recording.after(down(1_200)))
        assertEquals(recording, recording.after(CaptureEvent.Arm))
    }

    @Test
    fun disarmingIsIgnoredWhileRecording() {
        val recording = ready.after(down(1_000))

        assertEquals(recording, recording.after(CaptureEvent.Disarm))
    }

    @Test
    fun interruptionsOutsideRecordingChangeNothing() {
        val reasons = CancelReason.entries.map { CaptureEvent.Interrupted(it) }.toTypedArray()

        assertEquals(idle, idle.after(*reasons))
        assertEquals(ready, ready.after(*reasons))
    }

    @Test
    fun theLastOutcomeStaysVisibleUntilTheNextThrowEnds() {
        val first = ready.after(down(1_000), up(1_500))

        val during = first.after(down(3_000))
        assertEquals(first.lastThrow, during.lastThrow)

        val second = during.after(up(3_700))
        assertEquals(ThrowOutcome.Captured(3_000, 3_700), second.lastThrow)
    }

    @Test
    fun volumeDownIsConsumedOnlyWhileArmed() {
        assertFalse(CaptureMachine.consumesThrowKey(CapturePhase.Idle))
        assertTrue(CaptureMachine.consumesThrowKey(CapturePhase.Ready))
        assertTrue(CaptureMachine.consumesThrowKey(CapturePhase.Recording(pressTimeMs = 1_000)))
    }
}
