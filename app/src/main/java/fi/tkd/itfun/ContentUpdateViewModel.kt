package fi.tkd.itfun

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fi.tkd.itfun.data.db.DbProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ContentUpdateUiState(
    val statusText: String = "",
    val progress: Float = 0f,
    val showBar: Boolean = false,
    val isUpdating: Boolean = false
)

class ContentUpdateViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ContentRepository(application)
    private val _uiState = MutableStateFlow(ContentUpdateUiState())
    val uiState = _uiState.asStateFlow()

    fun updateContent() {
        viewModelScope.launch {
            _uiState.value = ContentUpdateUiState(
                statusText = "Checking for updates...",
                showBar = true,
                isUpdating = true
            )
            val result = repository.checkForUpdates()
            val statusText = when (result) {
                DbProvider.UpdateResult.NetworkError ->
                    "Unable to check for updates.\nPlease check your connection."
                DbProvider.UpdateResult.NoUpdate ->
                    "You already have the latest content;\nchecking missing videos..."
                DbProvider.UpdateResult.Updated -> "Updating content..."
            }
            _uiState.update { it.copy(statusText = statusText) }

            repository.downloadContent { progress ->
                _uiState.update { it.copy(progress = progress) }
            }
            if (result != DbProvider.UpdateResult.NetworkError) {
                _uiState.update { it.copy(statusText = "Done.") }
            }
            delay(3000)
            _uiState.value = ContentUpdateUiState()
        }
    }
}
