package sapienz.allison.disclab

import org.junit.Assert.assertEquals
import org.junit.Test

class MainViewModelTest {
    @Test
    fun startsWithNoUpdates() {
        assertEquals(MainUiState(tapCount = 0), MainViewModel().uiState.value)
    }

    @Test
    fun eachTapPublishesExactlyOneIncrement() {
        val viewModel = MainViewModel()

        viewModel.onTap()
        assertEquals(1, viewModel.uiState.value.tapCount)

        viewModel.onTap()
        assertEquals(2, viewModel.uiState.value.tapCount)
    }
}
