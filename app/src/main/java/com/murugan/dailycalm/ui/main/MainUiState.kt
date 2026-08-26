package com.murugan.dailycalm.ui.main

import com.murugan.dailycalm.DailyContent

sealed interface MainUiState {
    data object Loading : MainUiState
    data class Success(val content: DailyContent) : MainUiState

    /** Every published day has been read. [lastDay] is the final day available. */
    data class JourneyComplete(val lastDay: Int) : MainUiState

    data class Error(val message: String) : MainUiState
}
