package sapienz.allison.disclab

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Everything [MainScreen] needs to draw itself. It is immutable: the ViewModel publishes a new
 * copy on every change instead of mutating this one.
 *
 * Stage 0 skeleton: the tap counter only proves that state flows ViewModel -> UI and actions flow
 * UI -> ViewModel. Later stages replace it with real controller and gameplay state.
 */
data class MainUiState(
    val tapCount: Int = 0,
)

/**
 * Owns the screen state. Android keeps one instance alive across configuration changes such as
 * rotation, so the state survives while the Activity and its composables are recreated.
 *
 * The UI only reads [uiState] and reports user actions by calling methods; it never changes state
 * directly (unidirectional data flow).
 */
class MainViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    fun onTap() {
        _uiState.update { it.copy(tapCount = it.tapCount + 1) }
    }
}
