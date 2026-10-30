package fi.tkd.itfun

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

data class StartupUiState(
    val contentReady: Boolean,
    val progress: Float = 0f
)

class StartupViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ContentRepository(application)

    // Preserve the initial readiness check so returning users skip the download screen.
    private val initialContentReady = runBlocking(Dispatchers.IO) {
        repository.isContentReady()
    }

    private val _uiState = MutableStateFlow(
        StartupUiState(contentReady = initialContentReady)
    )
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.prepareContent(initialContentReady) { progress ->
                _uiState.update { it.copy(progress = progress) }
            }
            _uiState.update { it.copy(contentReady = true) }
        }
    }
}
