package fi.tkd.itfun

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ContentInventoryUiState(
    val inventory: ContentInventory? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val updateCheckFailed: Boolean = false
)

class ContentInventoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ContentInventoryRepository(application)
    private val manifestRepository = ContentManifestRepository(application)
    private val _uiState = MutableStateFlow(ContentInventoryUiState())
    val uiState = _uiState.asStateFlow()
    private var refreshJob: Job? = null
    private var checkJob: Job? = null

    fun checkForAvailableContent() {
        if (checkJob?.isActive == true) return
        checkJob = viewModelScope.launch {
            try {
                manifestRepository.check()
                refresh()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                _uiState.update { it.copy(updateCheckFailed = true) }
            }
        }
    }

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val cached = manifestRepository.cached()
                val inventory = repository.getInventory(cached.manifest)
                _uiState.update {
                    it.copy(inventory = inventory, isLoading = false, updateCheckFailed = cached.failed)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                _uiState.update { it.copy(
                    isLoading = false,
                    error = "Unable to read installed content."
                ) }
            }
        }
    }
}
