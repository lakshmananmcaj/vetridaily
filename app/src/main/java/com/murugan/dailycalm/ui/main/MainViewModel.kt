package com.murugan.dailycalm.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.murugan.dailycalm.BuildConfig
import com.murugan.dailycalm.data.ContentNotFoundException
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

    /** Last day that has audio published. Null until the backend has been asked. */
    private var publishedThroughDay: Int? = null

    init {
        val unlockedDay = resolveMaxUnlockedDay()
        _maxUnlockedDay.value = unlockedDay
        _selectedDay.value = unlockedDay

        viewModelScope.launch {
            syncPublishedCeiling()
            if (hasOutlivedContent()) {
                _uiState.value = MainUiState.JourneyComplete(lastDay = _selectedDay.value)
            } else {
                loadDay(day = _selectedDay.value, allowFallbackToDay1 = true)
            }
        }
    }

    fun retry() {
        viewModelScope.launch {
            syncPublishedCeiling()
            loadDay(day = _selectedDay.value, allowFallbackToDay1 = true)
        }
    }

    fun loadPreviousDay() {
        // From the completion screen, "previous" steps back into the last day that exists.
        if (_uiState.value is MainUiState.JourneyComplete) {
            viewModelScope.launch { loadDay(day = _selectedDay.value, allowFallbackToDay1 = false) }
            return
        }

        val previousDay = (_selectedDay.value - 1).coerceAtLeast(1)
        if (previousDay == _selectedDay.value) return
        _selectedDay.value = previousDay
        viewModelScope.launch { loadDay(day = previousDay, allowFallbackToDay1 = true) }
    }

    fun loadNextDay() {
        val limit = currentDayLimit()
        _maxUnlockedDay.value = limit

        if (_selectedDay.value >= limit) {
            if (hasOutlivedContent()) {
                _uiState.value = MainUiState.JourneyComplete(lastDay = limit)
            }
            return
        }

        val nextDay = _selectedDay.value + 1
        _selectedDay.value = nextDay
        viewModelScope.launch { loadDay(day = nextDay, allowFallbackToDay1 = true) }
    }

    private suspend fun loadDay(day: Int, allowFallbackToDay1: Boolean) {
        _uiState.value = MainUiState.Loading
        repository.getDailyContent(day)
            .onSuccess { content ->
                _uiState.value = MainUiState.Success(content)
            }
            .onFailure { error ->
                when {
                    // The day simply has not been published yet; the journey has reached its end.
                    error is ContentNotFoundException && day > 1 -> {
                        val lastDay = (day - 1).coerceAtLeast(1)
                        publishedThroughDay = lastDay
                        _maxUnlockedDay.value = lastDay
                        _selectedDay.value = lastDay
                        _uiState.value = MainUiState.JourneyComplete(lastDay)
                    }

                    allowFallbackToDay1 && day > 1 -> {
                        _selectedDay.value = 1
                        loadDay(day = 1, allowFallbackToDay1 = false)
                    }

                    else -> {
                        _uiState.value = MainUiState.Error(error.message ?: "Unable to load day $day")
                    }
                }
            }
    }

    private suspend fun syncPublishedCeiling() {
        repository.getLatestPublishedDay().onSuccess { latest ->
            if (latest < 1) return@onSuccess
            publishedThroughDay = latest

            val limit = currentDayLimit()
            _maxUnlockedDay.value = limit
            if (_selectedDay.value > limit) {
                _selectedDay.value = limit
            }
        }
    }

    /** Days the user has earned by elapsed time, capped by what is actually published. */
    private fun currentDayLimit(): Int {
        val unlocked = resolveMaxUnlockedDay()
        val ceiling = publishedThroughDay ?: return unlocked
        return minOf(unlocked, ceiling).coerceAtLeast(1)
    }

    /** True once the user has been enrolled longer than there is content for. */
    private fun hasOutlivedContent(): Boolean {
        val ceiling = publishedThroughDay ?: return false
        return resolveMaxUnlockedDay() > ceiling
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
