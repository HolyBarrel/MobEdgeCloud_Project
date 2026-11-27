package sapienz.allison.disclab

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import sapienz.allison.disclab.capture.CancelReason
import sapienz.allison.disclab.capture.CaptureMachine.MAX_HOLD_MS
import sapienz.allison.disclab.capture.CapturePhase
import sapienz.allison.disclab.capture.ThrowKey
import sapienz.allison.disclab.capture.ThrowOutcome

/**
 * The ViewModel's own jobs: which keys it consumes, and the hold timer. The state machine's rules
 * are covered in CaptureMachineTest.
 *
 * Key times are made-up uptime milliseconds. The timer runs on the test dispatcher's virtual
 * clock instead.
 */
// advanceTimeBy and runCurrent are still marked experimental in kotlinx-coroutines-test 1.9.0.
@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val MainViewModel.capture get() = uiState.value.capture

    @Test
    fun startsIdleWithNoThrows() {
        assertEquals(MainUiState(), MainViewModel().uiState.value)
        assertEquals(CapturePhase.Idle, MainViewModel().capture.phase)
    }

    @Test
    fun volumeDownBelongsToDiscLabOnlyWhileArmed() {
        val viewModel = MainViewModel()

        assertFalse(viewModel.onThrowKey(ThrowKey.Down(1_000, repeatCount = 0)))
        assertFalse(viewModel.onThrowKey(ThrowKey.Up(1_100, canceled = false)))

        viewModel.onArm()
        assertTrue(viewModel.onThrowKey(ThrowKey.Down(2_000, repeatCount = 0)))
        assertTrue(viewModel.onThrowKey(ThrowKey.Down(2_500, repeatCount = 1)))
        assertTrue(viewModel.onThrowKey(ThrowKey.Up(2_600, canceled = false)))

        assertEquals(ThrowOutcome.Captured(2_000, 2_600), viewModel.capture.lastThrow)
    }

    @Test
    fun holdingPastTheMaximumCancelsWithoutAKeyUp() = runTest {
        val viewModel = MainViewModel()
        viewModel.onArm()
        viewModel.onThrowKey(ThrowKey.Down(1_000, repeatCount = 0))

        advanceTimeBy(MAX_HOLD_MS - 1)
        runCurrent()
        assertEquals(CapturePhase.Recording(pressTimeMs = 1_000), viewModel.capture.phase)

        advanceTimeBy(1)
        runCurrent()
        assertEquals(CapturePhase.Ready, viewModel.capture.phase)
        assertEquals(
            ThrowOutcome.Cancelled(1_000, CancelReason.HELD_TOO_LONG),
            viewModel.capture.lastThrow,
        )
    }

    @Test
    fun anEarlierThrowsTimerDoesNotCancelTheNextThrow() = runTest {
        val viewModel = MainViewModel()
        viewModel.onArm()

        viewModel.onThrowKey(ThrowKey.Down(1_000, repeatCount = 0))
        runCurrent()
        advanceTimeBy(500)
        viewModel.onThrowKey(ThrowKey.Up(1_500, canceled = false))

        advanceTimeBy(3_500)
        viewModel.onThrowKey(ThrowKey.Down(5_000, repeatCount = 0))
        runCurrent()

        // Past the first throw's deadline, but well inside the second one's.
        advanceTimeBy(1_500)
        runCurrent()
        assertEquals(CapturePhase.Recording(pressTimeMs = 5_000), viewModel.capture.phase)
        assertEquals(ThrowOutcome.Captured(1_000, 1_500), viewModel.capture.lastThrow)
    }
}
