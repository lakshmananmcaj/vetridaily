package com.murugan.dailycalm.ui.temples

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.murugan.dailycalm.data.info.TempleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TemplesViewModel(
    private val repository: TempleRepository = TempleRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<TemplesUiState>(TemplesUiState.Loading)
    val uiState: StateFlow<TemplesUiState> = _uiState.asStateFlow()

    /** The wider directory is collapsed; the six abodes are the point of this tab. */
    private val _showAllTemples = MutableStateFlow(false)
    val showAllTemples: StateFlow<Boolean> = _showAllTemples.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    fun toggleAllTemples() {
        _showAllTemples.value = !_showAllTemples.value
    }

    private fun load() {
        _uiState.value = TemplesUiState.Loading

        viewModelScope.launch {
            // One request backs both lists — the repository caches the 30 rows and filters locally.
            repository.getArupadaiVeedu().fold(
                onSuccess = { six ->
                    _uiState.value = TemplesUiState.Success(
                        arupadaiVeedu = six,
                        otherTemples = repository.getOtherTemples().getOrDefault(emptyList())
                    )
                },
                onFailure = { error ->
                    _uiState.value = TemplesUiState.Error(
                        error.message ?: "Could not load temples"
                    )
                }
            )
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { TemplesViewModel() }
        }
    }
}
