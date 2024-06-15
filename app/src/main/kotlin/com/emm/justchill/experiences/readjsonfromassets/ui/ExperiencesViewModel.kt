package com.emm.justchill.experiences.readjsonfromassets.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.justchill.core.Result
import com.emm.justchill.core.mapResult
import com.emm.justchill.experiences.readjsonfromassets.domain.Experience
import com.emm.justchill.experiences.readjsonfromassets.domain.ExperiencesReader
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