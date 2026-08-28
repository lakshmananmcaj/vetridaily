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

    private val currentYear = Calendar.getInstance().get(Calendar.YEAR)

    /** Opens on the current month, which is what someone checking dates almost always wants. */
    private val _period = MutableStateFlow(
        FestivalPeriod(
            month = Calendar.getInstance().get(Calendar.MONTH) + 1,
            year = currentYear
        )
    )
    val period: StateFlow<FestivalPeriod> = _period.asStateFlow()

    /** Last year, this year, next year — the same span other Tamil calendars offer. */
    val years: List<Int> = listOf(currentYear - 1, currentYear, currentYear + 1)

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

    fun selectMonth(month: Int) {
        if (month == _period.value.month) return
        _period.value = _period.value.copy(month = month)
        load()
    }

    fun selectYear(year: Int) {
        if (year == _period.value.year) return
        _period.value = _period.value.copy(year = year)
        load()
    }

    fun toggleSection(section: FestivalSection) {
        _expandedSections.value = _expandedSections.value.toMutableSet().apply {
            if (!add(section)) remove(section)
        }
    }

    private fun load() {
        val (month, year) = _period.value
        _uiState.value = FestivalsUiState.Loading

        viewModelScope.launch {
            repository.getMonthGrouped(month = month, year = year).fold(
                onSuccess = { sections ->
                    // Pinned Murugan masters are a nicety; the month is still usable without them.
                    val pinned = repository.getMuruganMasters().getOrDefault(emptyList())
                    _uiState.value = FestivalsUiState.Success(
                        groups = sections,
                        muruganPinned = pinned
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
