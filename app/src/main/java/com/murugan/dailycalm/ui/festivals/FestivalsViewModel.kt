package com.murugan.dailycalm.ui.festivals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.murugan.dailycalm.data.info.FestivalRepository
import com.murugan.dailycalm.data.info.FestivalSection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

class FestivalsViewModel(
    private val repository: FestivalRepository = FestivalRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<FestivalsUiState>(FestivalsUiState.Loading)
    val uiState: StateFlow<FestivalsUiState> = _uiState.asStateFlow()

    /**
     * Auspicious days start collapsed. They outnumber everything else roughly two to one, so
     * expanding them by default would bury the festivals people opened the tab to find.
     */
    private val _expandedSections = MutableStateFlow(
        setOf(FestivalSection.FESTIVAL, FestivalSection.VRATHAM)
    )
    val expandedSections: StateFlow<Set<FestivalSection>> = _expandedSections.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    fun toggleSection(section: FestivalSection) {
        _expandedSections.value = _expandedSections.value.toMutableSet().apply {
            if (!add(section)) remove(section)
        }
    }

    private fun load() {
        val year = Calendar.getInstance().get(Calendar.YEAR)
        _uiState.value = FestivalsUiState.Loading

        viewModelScope.launch {
            val groups = repository.getYearGrouped(year)
            groups.fold(
                onSuccess = { sections ->
                    // Pinned Murugan masters are a nicety; the list is still usable without them.
                    val pinned = repository.getMuruganMasters().getOrDefault(emptyList())
                    _uiState.value = FestivalsUiState.Success(
                        groups = sections,
                        muruganPinned = pinned,
                        year = year
                    )
                },
                onFailure = { error ->
                    _uiState.value = FestivalsUiState.Error(
                        error.message ?: "Could not load festivals"
                    )
                }
            )
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { FestivalsViewModel() }
        }
    }
}
