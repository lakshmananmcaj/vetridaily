package com.murugan.dailycalm.ui.temples

import com.murugan.dailycalm.data.info.Temple

sealed interface TemplesUiState {
    data object Loading : TemplesUiState

    data class Success(
        /** The six abodes of Murugan, in traditional order. */
        val arupadaiVeedu: List<Temple>,
        /** The rest of the directory, alphabetical. */
        val otherTemples: List<Temple>
    ) : TemplesUiState

    data class Error(val message: String) : TemplesUiState
}
