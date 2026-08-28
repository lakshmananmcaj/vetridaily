package com.murugan.dailycalm.ui.festivals

import com.murugan.dailycalm.data.info.FestivalGroup
import com.murugan.dailycalm.data.info.FestivalMaster

sealed interface FestivalsUiState {
    data object Loading : FestivalsUiState

    data class Success(
        /** Sections in display order: festivals, vratham days, then auspicious days. */
        val groups: List<FestivalGroup>,
        /** Skanda Shashti, monthly Shashti and Karthigai, pinned above everything else. */
        val muruganPinned: List<FestivalMaster>
    ) : FestivalsUiState

    data class Error(val message: String) : FestivalsUiState
}

/** Which month is on screen. Kept apart from [FestivalsUiState] so the tabs stay put while loading. */
data class FestivalPeriod(
    val month: Int,
    val year: Int
)
