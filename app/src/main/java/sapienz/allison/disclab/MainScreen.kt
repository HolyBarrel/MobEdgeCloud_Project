package sapienz.allison.disclab

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import sapienz.allison.disclab.ui.theme.DiscLabTheme

/**
 * Stateless screen: it draws [state] and forwards clicks to [onTap]. It knows nothing about the
 * ViewModel, which keeps it previewable and testable on its own.
 */
@Composable
fun MainScreen(
    state: MainUiState,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
        Text(text = stringResource(R.string.tap_count, state.tapCount))
        Spacer(Modifier.height(16.dp))
        Button(onClick = onTap) {
            Text(text = stringResource(R.string.tap_button))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenPreview() {
    DiscLabTheme {
        MainScreen(state = MainUiState(tapCount = 3), onTap = {})
    }
}
