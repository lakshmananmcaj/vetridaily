package com.murugan.dailycalm.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.murugan.dailycalm.BuildConfig
import com.murugan.dailycalm.data.DailyContentDataSource
import com.murugan.dailycalm.data.DailyContentRepository
import com.murugan.dailycalm.data.DayProgressProvider
import com.murugan.dailycalm.data.DayProgressStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: DailyContentDataSource,
    private val dayProgressStore: DayProgressProvider,
    private val debugForceMaxUnlockedDay: Int = BuildConfig.DEBUG_FORCE_MAX_UNLOCKED_DAY
) : ViewModel() {

    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Loading)
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _selectedDay = MutableStateFlow(1)
    val selectedDay: StateFlow<Int> = _selectedDay.asStateFlow()

    private val _maxUnlockedDay = MutableStateFlow(1)
    val maxUnlockedDay: StateFlow<Int> = _maxUnlockedDay.asStateFlow()

    init {
        val realUnlockedDay = dayProgressStore.getMaxUnlockedDay().coerceAtLeast(1)
        val effectiveMaxUnlockedDay = resolveMaxUnlockedDay()
        _maxUnlockedDay.value = effectiveMaxUnlockedDay
        _selectedDay.value = realUnlockedDay
        loadDailyContent(day = realUnlockedDay, allowFallbackToDay1 = true)
    }

    fun retry() {
        loadDailyContent(day = _selectedDay.value, allowFallbackToDay1 = true)
    }

    fun loadPreviousDay() {
        val previousDay = (_selectedDay.value - 1).coerceAtLeast(1)
        if (previousDay == _selectedDay.value) return
        _selectedDay.value = previousDay
        loadDailyContent(day = previousDay, allowFallbackToDay1 = true)
    }

    fun loadNextDay() {
        val unlockedDay = resolveMaxUnlockedDay()
        _maxUnlockedDay.value = unlockedDay
        if (_selectedDay.value >= unlockedDay) return

        val nextDay = _selectedDay.value + 1
        _selectedDay.value = nextDay
        loadDailyContent(day = nextDay, allowFallbackToDay1 = true)
    }

    private fun loadDailyContent(day: Int, allowFallbackToDay1: Boolean) {
        _uiState.value = MainUiState.Loading
        viewModelScope.launch {
            repository.getDailyContent(day)
                .onSuccess { content ->
                    _uiState.value = MainUiState.Success(content)
                }
                .onFailure { error ->
                    if (allowFallbackToDay1 && day > 1) {
                        _selectedDay.value = 1
                        loadDailyContent(day = 1, allowFallbackToDay1 = false)
                    } else {
                        _uiState.value = MainUiState.Error(error.message ?: "Unable to load day $day")
                    }
                }
        }
    }

    private fun resolveMaxUnlockedDay(): Int {
        val calculated = dayProgressStore.getMaxUnlockedDay().coerceAtLeast(1)
        val debugOverride = debugForceMaxUnlockedDay
        return if (debugOverride > 0) {
            maxOf(calculated, debugOverride)
        } else {
            calculated
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = checkNotNull(
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                )
                MainViewModel(
                    repository = DailyContentRepository(),
                    dayProgressStore = DayProgressStore(application.applicationContext)
                )
            }
        }
    }
}
