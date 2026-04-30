package com.murugan.dailycalm.ui.main

import com.murugan.dailycalm.DailyContent

sealed interface MainUiState {
    data object Loading : MainUiState
    data class Success(val content: DailyContent) : MainUiState
    data class Error(val message: String) : MainUiState
}
