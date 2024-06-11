package com.emm.retrofit.experiences.readassets.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.retrofit.core.Result
import com.emm.retrofit.core.mapResult
import com.emm.retrofit.experiences.readassets.domain.Experience
import com.emm.retrofit.experiences.readassets.domain.ExperiencesReader
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ExperiencesViewModel(experiencesReader: ExperiencesReader) : ViewModel() {

    val experiences: StateFlow<Result<List<ExperienceItemUiState>>> = experiencesReader.read()
        .mapResult(List<Experience>::toUi)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = Result.Loading
        )
}