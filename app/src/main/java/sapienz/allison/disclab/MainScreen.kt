package sapienz.allison.disclab

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import sapienz.allison.disclab.capture.CancelReason
import sapienz.allison.disclab.capture.CaptureMachine
import sapienz.allison.disclab.capture.CapturePhase
import sapienz.allison.disclab.capture.CaptureState
import sapienz.allison.disclab.capture.ThrowOutcome
import sapienz.allison.disclab.ui.theme.DiscLabTheme

/**
 * Stateless screen: it draws [state] and forwards taps to [onArm] and [onDisarm]. It knows nothing
 * about the ViewModel, which keeps it previewable and testable on its own.
 *
 * Plain text for now; issue #5 turns it into a diagnostics screen.
 */
@Composable
fun MainScreen(
    state: MainUiState,
    onArm: () -> Unit,
    onDisarm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val capture = state.capture
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineLarge,
        )
        Text(
            text = stringResource(R.string.tagline),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        Text(
            text = when (capture.phase) {
                CapturePhase.Idle -> stringResource(R.string.phase_idle)
                CapturePhase.Ready ->
                    stringResource(R.string.phase_ready, CaptureMachine.MAX_HOLD_MS / 1000)
                is CapturePhase.Recording -> stringResource(R.string.phase_recording)
            },
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        if (capture.phase == CapturePhase.Idle) {
            Button(onClick = onArm) {
                Text(text = stringResource(R.string.arm_capture))
            }
        } else {
            // Disabled while recording, so a palm on the screen can't end a throw.
            OutlinedButton(onClick = onDisarm, enabled = capture.phase == CapturePhase.Ready) {
                Text(text = stringResource(R.string.disarm_capture))
            }
        }
        Spacer(Modifier.height(32.dp))
        LastThrow(capture.lastThrow)
    }
}

/** The outcome of the most recent throw: its press and release times, or why it was cancelled. */
@Composable
private fun LastThrow(outcome: ThrowOutcome?) {
    when (outcome) {
        null -> Text(text = stringResource(R.string.no_throws_yet))

        is ThrowOutcome.Captured -> {
            Text(text = stringResource(R.string.last_throw_captured))
            Text(
                text = stringResource(
                    R.string.press_and_release,
                    outcome.pressTimeMs,
                    outcome.releaseTimeMs,
                ),
            )
            Text(text = stringResource(R.string.held_for, outcome.heldMs))
        }

        is ThrowOutcome.Cancelled -> {
            Text(
                text = stringResource(
                    R.string.last_throw_cancelled,
                    stringResource(outcome.reason.label()),
                ),
            )
            Text(text = stringResource(R.string.press_only, outcome.pressTimeMs))
        }
    }
    if (outcome != null) {
        Text(
            text = stringResource(R.string.time_base),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@StringRes
private fun CancelReason.label(): Int = when (this) {
    CancelReason.KEY_CANCELED -> R.string.cancel_key_canceled
    CancelReason.APP_PAUSED -> R.string.cancel_app_paused
    CancelReason.FOCUS_LOST -> R.string.cancel_focus_lost
    CancelReason.HELD_TOO_LONG -> R.string.cancel_held_too_long
}

@Preview(showBackground = true)
@Composable
private fun MainScreenPreview() {
    DiscLabTheme {
        MainScreen(
            state = MainUiState(
                capture = CaptureState(
                    phase = CapturePhase.Ready,
                    lastThrow = ThrowOutcome.Captured(pressTimeMs = 812_400, releaseTimeMs = 813_040),
                ),
            ),
            onArm = {},
            onDisarm = {},
        )
    }
}
