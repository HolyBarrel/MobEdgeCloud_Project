package sapienz.allison.disclab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import sapienz.allison.disclab.ui.theme.DiscLabTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DiscLabTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // viewModel() returns the same instance after rotation.
                    val viewModel: MainViewModel = viewModel()
                    // Collects only while the Activity is at least STARTED.
                    val state by viewModel.uiState.collectAsStateWithLifecycle()
                    MainScreen(
                        state = state,
                        onTap = viewModel::onTap,
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }
}
